package org.dbsp.sqlCompiler.compiler;

import org.dbsp.sqlCompiler.ir.expression.DBSPExpression;
import org.dbsp.sqlCompiler.ir.expression.DBSPSomeExpression;
import org.dbsp.sqlCompiler.ir.expression.DBSPTupleExpression;
import org.dbsp.sqlCompiler.ir.expression.literal.*;
import org.dbsp.sqlCompiler.ir.type.primitive.DBSPTypeBool;
import org.dbsp.sqlCompiler.ir.type.primitive.DBSPTypeDouble;
import org.dbsp.sqlCompiler.ir.type.primitive.DBSPTypeInteger;
import org.junit.Test;

/**
 * Test end-to-end by compiling some DDL statements and view
 * queries by compiling them to rust and executing them
 * by inserting data in the input tables and reading data
 * from the declared views.
 */
public class EndToEndTests extends BaseSQLTests {
    /**
     * T is
     * -------------------------------------------
     * | 10 | 12.0 | true  | Hi | NULL    | NULL |
     * | 10 |  1.0 | false | Hi | Some[1] |  0.0 |
     * -------------------------------------------
     *
     * INSERT INTO T VALUES (10, 12, true, 'Hi'. NULL, NULL);
     * INSERT INTO T VALUES (10, 1.0, false, 'Hi', 1, 0.0);
     */

    void testQuery(String query, DBSPZSetLiteral expectedOutput) {
        DBSPZSetLiteral input = this.createInput();
        super.testQueryBase(query, false, false, new InputOutputPair(input, expectedOutput));
    }

    @Test
    public void overTest() {
        DBSPExpression t = new DBSPTupleExpression(new DBSPIntegerLiteral(10), new DBSPLongLiteral(2));
        String query = "SELECT T.COL1, COUNT(*) OVER (ORDER BY T.COL1 RANGE UNBOUNDED PRECEDING) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(t, t));
    }

    @Test
    public void overSumTest() {
        DBSPExpression t = new DBSPTupleExpression(new DBSPIntegerLiteral(10), new DBSPDoubleLiteral(13.0));
        String query = "SELECT T.COL1, SUM(T.COL2) OVER (ORDER BY T.COL1 RANGE UNBOUNDED PRECEDING) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(t, t));
    }

    @Test
    public void overTwiceTest() {
        DBSPExpression t = new DBSPTupleExpression(
                new DBSPIntegerLiteral(10),
                new DBSPDoubleLiteral(13.0),
                new DBSPLongLiteral(2));
        String query = "SELECT T.COL1, " +
                "SUM(T.COL2) OVER (ORDER BY T.COL1 RANGE UNBOUNDED PRECEDING), " +
                "COUNT(*) OVER (ORDER BY T.COL1 RANGE UNBOUNDED PRECEDING) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(t, t));
    }

    @Test
    public void overConstantWindowTest() {
        DBSPExpression t = new DBSPTupleExpression(
                new DBSPIntegerLiteral(10),
                new DBSPLongLiteral(2));
        String query = "SELECT T.COL1, " +
                "COUNT(*) OVER (ORDER BY T.COL1 RANGE BETWEEN 2 PRECEDING AND 1 PRECEDING) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(t, t));
    }

    @Test
    public void overTwiceDifferentTest() {
        DBSPExpression t = new DBSPTupleExpression(
                new DBSPIntegerLiteral(10),
                new DBSPDoubleLiteral(13.0),
                new DBSPLongLiteral(2));
        String query = "SELECT T.COL1, " +
                "SUM(T.COL2) OVER (ORDER BY T.COL1 RANGE UNBOUNDED PRECEDING), " +
                "COUNT(*) OVER (ORDER BY T.COL1 RANGE BETWEEN 2 PRECEDING AND 1 PRECEDING) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(t, t));
    }

    @Test
    public void correlatedAggregate() {
        // From: Efficient Incrementialization of Correlated Nested Aggregate
        // Queries using Relative Partial Aggregate Indexes (RPAI)
        // Supun Abeysinghe, Qiyang He, Tiark Rompf, SIGMOD 22
        String query = "SELECT Sum(r.COL1 * r.COL5) FROM T r\n" +
                "WHERE\n" +
                "0.5 * (SELECT Sum(r1.COL5) FROM T r1) =\n" +
                "(SELECT Sum(r2.COL5) FROM T r2 WHERE r2.COL1 = r.COL1)";
        this.testQuery(query, new DBSPZSetLiteral(new DBSPTupleExpression(
                DBSPLiteral.none(DBSPTypeInteger.signed32.setMayBeNull(true)))));

        // TODO
        query = "SELECT Sum(b.price * b.volume) FROM bids b\n" +
                "WHERE\n" +
                "0.75 * (SELECT Sum(b1.volume) FROM bids b1)\n" +
                "< (SELECT Sum(b2.volume) FROM bids b2\n" +
                "WHERE b2.price <= b.price)";
    }

    @Test
    public void projectTest() {
        String query = "SELECT T.COL3 FROM T";
        this.testQuery(query,
                new DBSPZSetLiteral(
                        new DBSPTupleExpression(DBSPBoolLiteral.True),
                        new DBSPTupleExpression(DBSPBoolLiteral.False)));
    }

    @Test
    public void intersectTest() {
        String query = "SELECT * FROM T INTERSECT (SELECT * FROM T)";
        this.testQuery(query, this.createInput());
    }

    @Test
    public void plusNullTest() {
        String query = "SELECT T.COL1 + T.COL5 FROM T";
        this.testQuery(query,
                new DBSPZSetLiteral(
                        new DBSPTupleExpression(new DBSPIntegerLiteral(11, true)),
                        new DBSPTupleExpression(DBSPLiteral.none(DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void negateNullTest() {
        String query = "SELECT -T.COL5 FROM T";
        this.testQuery(query,
                new DBSPZSetLiteral(
                        new DBSPTupleExpression(new DBSPIntegerLiteral(-1, true)),
                        new DBSPTupleExpression(DBSPLiteral.none(DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void projectNullTest() {
        String query = "SELECT T.COL5 FROM T";
        this.testQuery(query,
                new DBSPZSetLiteral(
                        new DBSPTupleExpression(new DBSPIntegerLiteral(1, true)),
                        new DBSPTupleExpression(DBSPLiteral.none(DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void unionTest() {
        String query = "(SELECT * FROM T) UNION (SELECT * FROM T)";
        this.testQuery(query, this.createInput());
    }

    @Test
    public void unionAllTest() {
        String query = "(SELECT * FROM T) UNION ALL (SELECT * FROM T)";
        DBSPZSetLiteral output = this.createInput();
        output.add(output);
        this.testQuery(query, output);
    }

    @Test
    public void joinTest() {
        String query = "SELECT T1.COL3, T2.COL3 FROM T AS T1 JOIN T AS T2 ON T1.COL1 = T2.COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPBoolLiteral.False, DBSPBoolLiteral.False),
                new DBSPTupleExpression(DBSPBoolLiteral.False, DBSPBoolLiteral.True),
                new DBSPTupleExpression(DBSPBoolLiteral.True,  DBSPBoolLiteral.False),
                new DBSPTupleExpression(DBSPBoolLiteral.True,  DBSPBoolLiteral.True)));
    }

    @Test
    public void joinNullableTest() {
        String query = "SELECT T1.COL3, T2.COL3 FROM T AS T1 JOIN T AS T2 ON T1.COL1 = T2.COL5";
        this.testQuery(query, empty);
    }

    @Test
    public void zero() {
        String query = "SELECT 0";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(new DBSPIntegerLiteral(0))));
    }

    @Test
    public void geoPointTest() {
        String query = "SELECT ST_POINT(0, 0)";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(
                        new DBSPSomeExpression(new DBSPGeoPointLiteral(null,
                                new DBSPDoubleLiteral(0), new DBSPDoubleLiteral(0))))));
    }

    @Test
    public void geoDistanceTest() {
        String query = "SELECT ST_DISTANCE(ST_POINT(0, 0), ST_POINT(0,1))";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(
                        new DBSPSomeExpression(new DBSPDoubleLiteral(1)))));
    }

    @Test
    public void leftOuterJoinTest() {
        String query = "SELECT T1.COL3, T2.COL3 FROM T AS T1 LEFT JOIN T AS T2 ON T1.COL1 = T2.COL5";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPBoolLiteral.False, DBSPBoolLiteral.None),
                new DBSPTupleExpression(DBSPBoolLiteral.True, DBSPBoolLiteral.None)
        ));
    }

    @Test
    public void rightOuterJoinTest() {
        String query = "SELECT T1.COL3, T2.COL3 FROM T AS T1 RIGHT JOIN T AS T2 ON T1.COL1 = T2.COL5";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPBoolLiteral.None, DBSPBoolLiteral.False),
                new DBSPTupleExpression(DBSPBoolLiteral.None, DBSPBoolLiteral.True)
        ));
    }

    @Test
    public void fullOuterJoinTest() {
        String query = "SELECT T1.COL3, T2.COL3 FROM T AS T1 FULL OUTER JOIN T AS T2 ON T1.COL1 = T2.COL5";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPBoolLiteral.NullableFalse, DBSPBoolLiteral.None),
                new DBSPTupleExpression(DBSPBoolLiteral.NullableTrue, DBSPBoolLiteral.None),
                new DBSPTupleExpression(DBSPBoolLiteral.None, DBSPBoolLiteral.NullableFalse),
                new DBSPTupleExpression(DBSPBoolLiteral.None, DBSPBoolLiteral.NullableTrue)
        ));
    }

    @Test
    public void emptyWhereTest() {
        String query = "SELECT * FROM T WHERE FALSE";
        this.testQuery(query, empty);
    }

    @Test
    public void whereTest() {
        String query = "SELECT * FROM T WHERE COL3";
        this.testQuery(query, z0);
    }

    @Test
    public void whereImplicitCastTest() {
        String query = "SELECT * FROM T WHERE COL2 < COL1";
        this.testQuery(query, z1);
    }

    @Test
    public void whereExplicitCastTest() {
        String query = "SELECT * FROM T WHERE COL2 < CAST(COL1 AS DOUBLE)";
        this.testQuery(query, z1);
    }

    @Test
    public void whereExplicitCastTestNull() {
        String query = "SELECT * FROM T WHERE COL2 < CAST(COL5 AS DOUBLE)";
        this.testQuery(query, empty);
    }

    @Test
    public void whereExplicitImplicitCastTest() {
        String query = "SELECT * FROM T WHERE COL2 < CAST(COL1 AS FLOAT)";
        this.testQuery(query, z1);
    }

    @Test
    public void whereExplicitImplicitCastTestNull() {
        String query = "SELECT * FROM T WHERE COL2 < CAST(COL5 AS FLOAT)";
        this.testQuery(query, empty);
    }

    @Test
    public void whereExpressionTest() {
        String query = "SELECT * FROM T WHERE COL2 < 0";
        this.testQuery(query, new DBSPZSetLiteral(z0.getNonVoidType()));
    }

    @Test
    public void exceptTest() {
        String query = "SELECT * FROM T EXCEPT (SELECT * FROM T WHERE COL3)";
        this.testQuery(query, z1);
    }

    @Test
    public void constantFoldTest() {
        String query = "SELECT 1 + 2";
        this.testQuery(query, new DBSPZSetLiteral(new DBSPTupleExpression(new DBSPIntegerLiteral(3))));
    }

    @Test
    public void groupByTest() {
        String query = "SELECT COL1 FROM T GROUP BY COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(new DBSPIntegerLiteral(10))));
    }

    @Test
    public void groupByCountTest() {
        String query = "SELECT COL1, COUNT(col2) FROM T GROUP BY COL1, COL3";
        DBSPExpression row =  new DBSPTupleExpression(new DBSPIntegerLiteral(10), new DBSPLongLiteral(1));
        this.testQuery(query, new DBSPZSetLiteral( row, row));
    }

    @Test
    public void groupBySumTest() {
        String query = "SELECT COL1, SUM(col2) FROM T GROUP BY COL1, COL3";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(new DBSPIntegerLiteral(10), new DBSPDoubleLiteral(1)),
                new DBSPTupleExpression(new DBSPIntegerLiteral(10), new DBSPDoubleLiteral(12))));
    }

    @Test
    public void divTest() {
        String query = "SELECT T.COL1 / T.COL5 FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeInteger.signed32.setMayBeNull(true))),
                new DBSPTupleExpression(new DBSPIntegerLiteral(10, true))));
    }

    @Test
    public void divIntTest() {
        String query = "SELECT T.COL5 / T.COL5 FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeInteger.signed32.setMayBeNull(true))),
                new DBSPTupleExpression(new DBSPIntegerLiteral(1, true))));
    }

    @Test
    public void divZeroTest() {
        String query = "SELECT 1 / 0";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void nestedDivTest() {
        String query = "SELECT 2 / (1 / 0)";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void customDivisionTest() {
        // Use a custom division operator.
        String query = "SELECT DIVISION(1, 0)";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void floatDivTest() {
        String query = "SELECT T.COL6 / T.COL6 FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(
                        DBSPTypeDouble.instance.setMayBeNull(true))),
                new DBSPTupleExpression(new DBSPDoubleLiteral(Double.NaN, true))));
    }

    @Test
    public void aggregateDistinctTest() {
        String query = "SELECT SUM(DISTINCT T.COL1), SUM(T.COL2) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(10, true), new DBSPDoubleLiteral(13.0, true))));
    }

    @Test
    public void aggregateTest() {
        String query = "SELECT SUM(T.COL1) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(20, true))));
    }

    @Test
    public void maxTest() {
        String query = "SELECT MAX(T.COL1) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(10, true))));
    }

    @Test
    public void maxConst() {
        String query = "SELECT MAX(6) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(6, true))));
    }

    @Test
    public void constAggregateExpression() {
        String query = "SELECT 34 / SUM (1) FROM T GROUP BY COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(17, true))));
    }

    @Test
    public void inTest() {
        String query = "SELECT 3 in (SELECT COL5 FROM T)";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPTupleExpression(DBSPLiteral.none(DBSPTypeBool.instance.setMayBeNull(true)))));
    }

    @Test
    public void constAggregateExpression2() {
        String query = "SELECT 34 / AVG (1) FROM T GROUP BY COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(34, true))));
    }

    @Test
    public void constAggregateDoubleExpression() {
        String query = "SELECT 34 / SUM (1), 20 / SUM(2) FROM T GROUP BY COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(17, true), new DBSPIntegerLiteral(5, true))));
    }

    @Test
    public void aggregateFloatTest() {
        String query = "SELECT SUM(T.COL2) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPDoubleLiteral(13.0, true))));
    }

    @Test
    public void optionAggregateTest() {
        String query = "SELECT SUM(T.COL5) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(1, true))));
    }

    @Test
    public void aggregateFalseTest() {
        String query = "SELECT SUM(T.COL1) FROM T WHERE FALSE";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        DBSPLiteral.none(DBSPTypeInteger.signed32.setMayBeNull(true)))));
    }

    @Test
    public void averageTest() {
        String query = "SELECT AVG(T.COL1) FROM T";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                        new DBSPIntegerLiteral(10, true))));
    }

    @Test
    public void cartesianTest() {
        String query = "SELECT * FROM T, T AS X";
        DBSPExpression inResult = DBSPTupleExpression.flatten(e0, e0);
        DBSPZSetLiteral result = new DBSPZSetLiteral( inResult);
        result.add(DBSPTupleExpression.flatten(e0, e1));
        result.add(DBSPTupleExpression.flatten(e1, e0));
        result.add(DBSPTupleExpression.flatten(e1, e1));
        this.testQuery(query, result);
    }

    @Test
    public void foldTest() {
        String query = "SELECT + 91 + NULLIF ( + 93, + 38 )";
        this.testQuery(query, new DBSPZSetLiteral(
                 new DBSPTupleExpression(
                new DBSPIntegerLiteral(184, true))));
    }

    @Test
    public void orderbyTest() {
        String query = "SELECT * FROM T ORDER BY T.COL2";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPVecLiteral(e1, e0)
        ));
    }

    @Test
    public void orderbyDescendingTest() {
        String query = "SELECT * FROM T ORDER BY T.COL2 DESC";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPVecLiteral(e0, e1)
        ));
    }

    @Test
    public void orderby2Test() {
        String query = "SELECT * FROM T ORDER BY T.COL2, T.COL1";
        this.testQuery(query, new DBSPZSetLiteral(
                new DBSPVecLiteral(e1, e0)
        ));
    }
}
