/*
 * Copyright 2022 VMware, Inc.
 * SPDX-License-Identifier: MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.dbsp.sqlCompiler.compiler.sqlparser;

import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.config.*;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepMatchOrder;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.prepare.Prepare;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.RelVisitor;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.rules.*;
import org.apache.calcite.rel.type.*;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.ddl.SqlColumnDeclaration;
import org.apache.calcite.sql.ddl.SqlCreateTable;
import org.apache.calcite.sql.ddl.SqlCreateView;
import org.apache.calcite.sql.ddl.SqlDropTable;
import org.apache.calcite.sql.fun.SqlLibrary;
import org.apache.calcite.sql.fun.SqlLibraryOperatorTableFactory;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.parser.ddl.SqlDdlParserImpl;
import org.apache.calcite.sql.type.*;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.sql2rel.RelDecorrelator;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.sql2rel.StandardConvertletTable;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.Pair;
import org.dbsp.sqlCompiler.compiler.CompilerOptions;
import org.dbsp.sqlCompiler.compiler.frontend.statements.*;
import org.dbsp.util.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * The calcite compiler compiles SQL into Calcite RelNode representations.
 * It is stateful.
 * The protocol is:
 * - compiler is initialized (startCompilation)
 * - a sequence of statements is supplied as SQL strings:
 *   - table definition statements
 *   - insert or delete statements
 *   - view definition statements
 * For each statement the compiler returns a representation suitable for passing
 * to the mid-end.
 * The front-end is itself composed of several stages:
 * - compile SQL to SqlNode
 * - compile SqlNode to RelNode
 * - optimize RelNode
 */
public class CalciteCompiler implements IModule {
    private final SqlParser.Config parserConfig;
    private final SqlValidator validator;
    private final Catalog catalog;
    private final SqlToRelConverter converter;
    public final RelOptCluster cluster;
    public final RelDataTypeFactory typeFactory;
    private final SqlToRelConverter.Config converterConfig;
    private final RewriteDivision astRewriter;

    /**
     * This class rewrites instances of the division operator in the SQL AST
     * into calls to a user-defined function DIVISION.  We do this
     * because we don't like how Calcite infers result types for division,
     * and we want to supply our own rules.
     */
    public static class RewriteDivision extends SqlShuttle {
        @Override
        public SqlNode visit(SqlCall call) {
            SqlNode node = Objects.requireNonNull(super.visit(call));
            if (node instanceof SqlCall &&
                    node.getKind() == SqlKind.DIVIDE) {
                SqlCall rewrittenCall = (SqlCall)node;
                return new SqlBasicCall(
                        new SqlUnresolvedFunction(
                                new SqlIdentifier("DIVISION", SqlParserPos.ZERO),
                                null,
                                null,
                                null,
                                null,
                                SqlFunctionCategory.USER_DEFINED_FUNCTION),
                        rewrittenCall.getOperandList(),
                        rewrittenCall.getParserPosition()
                );
            }
            return node;
        }
    }

    static class SqlDivideFunction extends SqlFunction {
        // Custom implementation of type inference DIVISION for our division operator.
        static SqlReturnTypeInference divResultInference = new SqlReturnTypeInference() {
            @Override
            public @org.checkerframework.checker.nullness.qual.Nullable
            RelDataType inferReturnType(SqlOperatorBinding opBinding) {
                // Default policy for division.
                RelDataType result = ReturnTypes.QUOTIENT_NULLABLE.inferReturnType(opBinding);
                List<RelDataType> opTypes = opBinding.collectOperandTypes();
                // If all operands are integer or decimal, result is nullable
                // otherwise it's not.
                boolean nullable = true;
                for (RelDataType type: opTypes) {
                    if (type.getSqlTypeName() == SqlTypeName.FLOAT) {
                        nullable = false;
                        break;
                    }
                }
                if (nullable)
                    result = opBinding.getTypeFactory().createTypeWithNullability(result, true);
                return result;
            }
        };

        public SqlDivideFunction() {
            super("DIVISION",
                    SqlKind.OTHER_FUNCTION,
                    divResultInference,
                    null,
                    OperandTypes.NUMERIC_NUMERIC,
                    SqlFunctionCategory.NUMERIC);
        }

        @Override
        public boolean isDeterministic() {
            // TODO: change this when we learn how to constant-fold in the RexToLixTranslator
            // https://issues.apache.org/jira/browse/CALCITE-3394 may give a solution
            return false;
        }
    }

    // Adapted from https://www.querifylabs.com/blog/assembling-a-query-optimizer-with-apache-calcite
    public CalciteCompiler(CompilerOptions options) {
        this.astRewriter = new RewriteDivision();
        Properties connConfigProp = new Properties();
        connConfigProp.put(CalciteConnectionProperty.CASE_SENSITIVE.camelName(), Boolean.TRUE.toString());
        connConfigProp.put(CalciteConnectionProperty.UNQUOTED_CASING.camelName(), Casing.UNCHANGED.toString());
        connConfigProp.put(CalciteConnectionProperty.QUOTED_CASING.camelName(), Casing.UNCHANGED.toString());
        CalciteConnectionConfig connectionConfig = new CalciteConnectionConfigImpl(connConfigProp);
        SqlConformance conformance = connectionConfig.conformance();
        this.parserConfig = SqlParser.config()
                .withLex(options.ioOptions.dialect)
                // Add support for DDL language
                .withParserFactory(SqlDdlParserImpl.FACTORY)
                .withConformance(conformance);
        this.typeFactory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
        this.catalog = new Catalog("schema");
        CalciteSchema rootSchema = CalciteSchema.createRootSchema(false, false);
        rootSchema.add(catalog.schemaName, this.catalog);
        // Register new types
        rootSchema.add("INT64", factory -> factory.createSqlType(SqlTypeName.INTEGER));
        rootSchema.add("FLOAT64", factory -> factory.createSqlType(SqlTypeName.DOUBLE));
        rootSchema.add("FLOAT32", factory -> factory.createSqlType(SqlTypeName.FLOAT));
        rootSchema.add("STRING", factory -> factory.createSqlType(SqlTypeName.VARCHAR));
        rootSchema.add("BOOL", factory -> factory.createSqlType(SqlTypeName.BOOLEAN));
        // TODO: not entirely correct
        rootSchema.add("UINT64", factory -> factory.createSqlType(SqlTypeName.INTEGER));
        Prepare.CatalogReader catalogReader = new CalciteCatalogReader(
                rootSchema, Collections.singletonList(catalog.schemaName), this.typeFactory, connectionConfig);

        SqlFunction division = new SqlDivideFunction();
        SqlOperatorTable operatorTable = SqlOperatorTables.chain(
                // Libraries of user-defined functions supported.
                SqlLibraryOperatorTableFactory.INSTANCE.getOperatorTable(
                        // Standard SQL functions
                        EnumSet.of(SqlLibrary.STANDARD,
                                // Geospatial functions
                                SqlLibrary.SPATIAL)),
                // Our custom division operation
                SqlOperatorTables.of(division)
        );

        SqlValidator.Config validatorConfig = SqlValidator.Config.DEFAULT
                .withLenientOperatorLookup(connectionConfig.lenientOperatorLookup())
                .withTypeCoercionEnabled(true)
                .withDefaultNullCollation(connectionConfig.defaultNullCollation())
                .withIdentifierExpansion(true);

        this.validator = SqlValidatorUtil.newValidator(
                operatorTable,
                catalogReader,
                this.typeFactory,
                validatorConfig
        );

        // This planner does not do anything.
        // We use a series of planner stages later to perform the real optimizations.
        RelOptPlanner planner = new HepPlanner(new HepProgramBuilder().build());
        planner.setExecutor(RexUtil.EXECUTOR);
        this.cluster = RelOptCluster.create(planner, new RexBuilder(this.typeFactory));
        this.converterConfig = SqlToRelConverter.config()
                .withExpand(true);
        this.converter = new SqlToRelConverter(
                (type, query, schema, path) -> null,
                this.validator,
                catalogReader,
                this.cluster,
                StandardConvertletTable.INSTANCE,
                this.converterConfig
        );
    }

    /**
     * Policy which decides whether to run the busy join optimization.
     * @param rootRel Current plan.
     */
    public static boolean avoidBushyJoin(RelNode rootRel) {
        class OuterJoinFinder extends RelVisitor {
            public int outerJoinCount = 0;
            public int joinCount = 0;
            @Override public void visit(RelNode node, int ordinal,
                                        @org.checkerframework.checker.nullness.qual.Nullable RelNode parent) {
                if (node instanceof Join) {
                    Join join = (Join)node;
                    ++joinCount;
                    if (join.getJoinType().isOuterJoin())
                        ++outerJoinCount;
                }
                super.visit(node, ordinal, parent);
            }

            void run(RelNode node) {
                this.go(node);
            }
        }

        OuterJoinFinder finder = new OuterJoinFinder();
        finder.run(rootRel);
        // Bushy join optimization fails when the query contains outer joins.
        return (finder.outerJoinCount > 0) || (finder.joinCount < 3);
    }

    /**
     * Helper function used to assemble sequence of optimization rules
     * into an optimization plan.  The rules are executed in sequence.
     */
    static HepProgram createProgram(RelOptRule... rules) {
        HepProgramBuilder builder = new HepProgramBuilder();
        for (RelOptRule r: rules)
            builder.addRuleInstance(r);
        return builder.build();
    }

    /**
     * List of optimization stages to apply on RelNodes.
     * We do program-dependent optimization, since some optimizations
     * are buggy and don't always work.
     */
    static List<HepProgram> getOptimizationStages(RelNode rel) {
        HepProgram constantFold = createProgram(
                CoreRules.FILTER_REDUCE_EXPRESSIONS,
                CoreRules.PROJECT_REDUCE_EXPRESSIONS,
                CoreRules.JOIN_REDUCE_EXPRESSIONS,
                CoreRules.WINDOW_REDUCE_EXPRESSIONS,
                CoreRules.CALC_REDUCE_EXPRESSIONS
        );
        // Remove empty collections
        HepProgram removeEmpty = createProgram(
                PruneEmptyRules.UNION_INSTANCE,
                PruneEmptyRules.INTERSECT_INSTANCE,
                PruneEmptyRules.MINUS_INSTANCE,
                PruneEmptyRules.PROJECT_INSTANCE,
                PruneEmptyRules.FILTER_INSTANCE,
                PruneEmptyRules.SORT_INSTANCE,
                PruneEmptyRules.AGGREGATE_INSTANCE,
                PruneEmptyRules.JOIN_LEFT_INSTANCE,
                PruneEmptyRules.JOIN_RIGHT_INSTANCE,
                PruneEmptyRules.SORT_FETCH_ZERO_INSTANCE);
        HepProgram distinctAggregates = createProgram(
                // Convert DISTINCT aggregates into separate computations and join the results
                CoreRules.AGGREGATE_EXPAND_DISTINCT_AGGREGATES_TO_JOIN);
        HepProgram multiJoins = new HepProgramBuilder()
                // Join order optimization
                .addRuleInstance(CoreRules.FILTER_INTO_JOIN)
                .addMatchOrder(HepMatchOrder.BOTTOM_UP)
                .addRuleInstance(CoreRules.JOIN_TO_MULTI_JOIN)
                .addRuleInstance(CoreRules.PROJECT_MULTI_JOIN_MERGE)
                .addRuleInstance(CoreRules.MULTI_JOIN_OPTIMIZE_BUSHY)
                .build();
        HepProgram mergeNodes = createProgram(
                CoreRules.PROJECT_MERGE,
                CoreRules.MINUS_MERGE,
                CoreRules.UNION_MERGE,
                CoreRules.AGGREGATE_MERGE,
                CoreRules.INTERSECT_MERGE);
        // Remove unused code
        HepProgram remove = createProgram(
                CoreRules.AGGREGATE_REMOVE,
                CoreRules.UNION_REMOVE,
                CoreRules.PROJECT_REMOVE,
                CoreRules.PROJECT_JOIN_JOIN_REMOVE,
                CoreRules.PROJECT_JOIN_REMOVE
                );
        HepProgram window = createProgram(
                CoreRules.PROJECT_TO_LOGICAL_PROJECT_AND_WINDOW
        );
            if (avoidBushyJoin(rel))
                return Linq.list(constantFold, removeEmpty, window, distinctAggregates, mergeNodes, remove);
            return Linq.list(constantFold, removeEmpty, window, distinctAggregates, multiJoins, mergeNodes, remove);
            /*
        return Linq.list(
                CoreRules.PROJECT_JOIN_TRANSPOSE)
                CoreRules.AGGREGATE_PROJECT_PULL_UP_CONSTANTS,
                CoreRules.AGGREGATE_UNION_AGGREGATE,
                CoreRules.JOIN_PUSH_EXPRESSIONS,
                CoreRules.JOIN_CONDITION_PUSH,
                CoreRules.PROJECT_FILTER_TRANSPOSE,
                CoreRules.PROJECT_SET_OP_TRANSPOSE,
        );
         */
    }

    public static String getPlan(RelNode rel) {
        return RelOptUtil.dumpPlan("[Logical plan]", rel,
                SqlExplainFormat.TEXT,
                SqlExplainLevel.NON_COST_ATTRIBUTES);
    }

    public RexBuilder getRexBuilder() {
        return this.cluster.getRexBuilder();
    }

    /**
     * Given a SQL statement returns a SqlNode - a calcite AST
     * representation of the query.
     * @param sql  SQL query to compile
     */
    public SqlNode parse(String sql) throws SqlParseException {
        // This is a weird API - the parser depends on the query string!
        try {
            SqlParser sqlParser = SqlParser.create(sql, this.parserConfig);
            return sqlParser.parseStmt();
        } catch (SqlParseException parse) {
            System.err.println("Exception while parsing " + sql);
            throw parse;
        }
    }

    /**
     * Given a list of statements separated by semicolons, parse all of them.
     */
    public SqlNodeList parseStatements(String statements) throws SqlParseException {
        SqlParser sqlParser = SqlParser.create(statements, this.parserConfig);
        return sqlParser.parseStmtList();
    }

    RelNode optimize(RelNode rel) {
        // Without the following some optimization rules do nothing.
        Logger.instance.from(this, 2)
                .append("Before optimizer")
                .increase()
                .append(getPlan(rel))
                .decrease()
                .newline();

        RelBuilder relBuilder = this.converterConfig.getRelBuilderFactory().create(
                cluster, null);
        // This converts correlated sub-queries into standard joins.
        rel = RelDecorrelator.decorrelateQuery(rel, relBuilder);
        Logger.instance.from(this, 2)
                .append("After decorrelator")
                .increase()
                .append(getPlan(rel))
                .decrease()
                .newline();

        int stage = 0;
        for (HepProgram program: getOptimizationStages(rel)) {
            HepPlanner planner = new HepPlanner(program);
            planner.setRoot(rel);
            rel = planner.findBestExp();
            Logger.instance.from(this, 3)
                    .append("After optimizer stage ")
                    .append(stage)
                    .increase()
                    .append(getPlan(rel))
                    .decrease()
                    .newline();
            stage++;
        }

        Logger.instance.from(this, 2)
                .append("After optimizer ")
                .increase()
                .append(getPlan(rel))
                .decrease()
                .newline();
        return rel;
    }

    RelDataType convertType(SqlDataTypeSpec spec) {
        SqlTypeNameSpec type = spec.getTypeNameSpec();
        RelDataType result = type.deriveType(this.validator);
        if (Objects.requireNonNull(spec.getNullable()))
            result = this.typeFactory.createTypeWithNullability(result, true);
        return result;
    }

    List<RelDataTypeField> getColumnTypes(SqlNodeList list) {
        List<RelDataTypeField> result = new ArrayList<>();
        int index = 0;
        for (SqlNode col: Objects.requireNonNull(list)) {
            if (col.getKind().equals(SqlKind.COLUMN_DECL)) {
                SqlColumnDeclaration cd = (SqlColumnDeclaration)col;
                RelDataType type = this.convertType(cd.dataType);
                String name = Catalog.identifierToString(cd.name);
                RelDataTypeField field = new RelDataTypeFieldImpl(name, index++, type);
                result.add(field);
                continue;
            }
            throw new Unimplemented(col);
        }
        return result;
    }

    public List<RelDataTypeField> getColumnTypes(RelRoot relRoot) {
        List<RelDataTypeField> columns = new ArrayList<>();
        RelDataType rowType = relRoot.rel.getRowType();
        for (Pair<Integer, String> field : relRoot.fields) {
            String name = field.right;
            RelDataTypeField f = rowType.getField(name, false, false);
            columns.add(f);
        }
        return columns;
    }

    /**
     * Compile a SQL statement.  Return a description
     */
    public FrontEndStatement compile(
            String sqlStatement,
            SqlNode node,
            @Nullable String comment) {
        if (SqlKind.DDL.contains(node.getKind())) {
            if (node.getKind().equals(SqlKind.DROP_TABLE)) {
                SqlDropTable dt = (SqlDropTable) node;
                String tableName = Catalog.identifierToString(dt.name);
                this.catalog.dropTable(tableName);
                return new DropTableStatement(node, sqlStatement, tableName, comment);
            } else if (node.getKind().equals(SqlKind.CREATE_TABLE)) {
                SqlCreateTable ct = (SqlCreateTable)node;
                String tableName = Catalog.identifierToString(ct.name);
                List<RelDataTypeField> cols;
                if (ct.columnList != null) {
                    cols = this.getColumnTypes(Objects.requireNonNull(ct.columnList));
                } else {
                    if (ct.query == null)
                        throw new UnsupportedException(node);
                    Logger.instance.from(this, 1)
                            .append(ct.query.toString())
                            .newline();
                    RelRoot relRoot = this.converter.convertQuery(ct.query, true, true);
                    cols = this.getColumnTypes(relRoot);
                }
                CreateTableStatement table = new CreateTableStatement(node, sqlStatement, tableName, comment, cols);
                this.catalog.addTable(tableName, table.getEmulatedTable());
                return table;
            }

            if (node.getKind().equals(SqlKind.CREATE_VIEW)) {
                SqlCreateView cv = (SqlCreateView) node;
                SqlNode query = cv.query;
                Logger.instance.from(this, 2)
                        .append(query.toString())
                        .newline();
                query = query.accept(this.astRewriter);
                Logger.instance.from(this, 2)
                        .append(Objects.requireNonNull(query).toString())
                        .newline();
                RelRoot relRoot = this.converter.convertQuery(query, true, true);
                List<RelDataTypeField> columns = this.getColumnTypes(relRoot);
                RelNode optimized = this.optimize(relRoot.rel);
                relRoot = relRoot.withRel(optimized);
                String viewName = Catalog.identifierToString(cv.name);
                CreateViewStatement view = new CreateViewStatement(node, sqlStatement,
                        Catalog.identifierToString(cv.name), comment,
                        columns, cv.query, relRoot);
                // From Calcite's point of view we treat this view just as another table.
                this.catalog.addTable(viewName, view.getEmulatedTable());
                return view;
            }
        }

        if (SqlKind.DML.contains(node.getKind())) {
            if (node instanceof SqlInsert) {
                SqlInsert insert = (SqlInsert) node;
                SqlNode table = insert.getTargetTable();
                if (!(table instanceof SqlIdentifier))
                    throw new Unimplemented(table);
                SqlIdentifier id = (SqlIdentifier) table;
                TableModifyStatement stat = new TableModifyStatement(node, sqlStatement, id.toString(), insert.getSource(), comment);
                RelRoot values = this.converter.convertQuery(stat.data, true, true);
                values = values.withRel(this.optimize(values.rel));
                stat.setTranslation(values.rel);
                return stat;
            }
        }

        throw new Unimplemented(node);
    }
}
