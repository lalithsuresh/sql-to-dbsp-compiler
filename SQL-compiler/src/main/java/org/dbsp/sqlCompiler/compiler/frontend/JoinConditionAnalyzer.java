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

package org.dbsp.sqlCompiler.compiler.frontend;

import org.apache.calcite.rex.*;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.util.IModule;
import org.dbsp.util.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JoinConditionAnalyzer extends RexVisitorImpl<Void> implements IModule {
    private final int leftTableColumnCount;
    private final ConditionDecomposition result;
    private final TypeCompiler typeCompiler;

    public JoinConditionAnalyzer(int leftTableColumnCount, TypeCompiler typeCompiler) {
        super(true);
        this.leftTableColumnCount = leftTableColumnCount;
        this.result = new ConditionDecomposition();
        this.typeCompiler = typeCompiler;
    }

    /**
     * Represents an equality test in a join
     * between two columns in the two tables.
     */
    static class EqualityTest {
        public final int leftColumn;
        public final int rightColumn;
        public final DBSPType resultType;

        EqualityTest(int leftColumn, int rightColumn, DBSPType resultType) {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;
            this.resultType = resultType;
            if (leftColumn < 0 || rightColumn < 0)
                throw new RuntimeException("Illegal column number " + leftColumn + ":" + rightColumn);
        }
    }

    /**
     * A join condition is decomposed into a list of equality comparisons
     * and another general-purpose boolean expression.
     */
    class ConditionDecomposition {
        public final List<EqualityTest> comparisons;
        @Nullable
        RexNode            leftOver;

        ConditionDecomposition() {
            this.comparisons = new ArrayList<>();
            this.leftOver = null;
        }

        void setLeftOver(RexNode leftOver) {
            this.leftOver = leftOver;
        }

        public void addEquality(RexNode left, RexNode right, DBSPType resultType) {
            RexInputRef ref = Objects.requireNonNull(asInputRef(left));
            int l = ref.getIndex();
            ref = Objects.requireNonNull(asInputRef(right));
            int r = ref.getIndex() - JoinConditionAnalyzer.this.leftTableColumnCount;
            this.comparisons.add(new EqualityTest(l, r, resultType));
        }

        void validate() {
            if (this.leftOver == null && this.comparisons.isEmpty())
                throw new RuntimeException("Unexpected empty join condition");
        }

        /**
         * Part of the join condition that is not an equality test.
         * @return Null if the entire condition is an equality test.
         */
        @Nullable
        public RexNode getLeftOver() {
            this.validate();
            return this.leftOver;
        }
    }

    public boolean completed() {
        return this.result.leftOver != null;
    }

    @Nullable
    public static RexInputRef asInputRef(RexNode node) {
        if (!(node instanceof RexInputRef))
            return null;
        return (RexInputRef) node;
    }

    /**
     * Returns 'true' if this expression is referencing a column in the left table.
     * @param node  A row expression.
     * @return null if this does not refer to a table column.
     */
    @Nullable
    public Boolean isLeftTableColumnReference(RexNode node) {
        RexInputRef ref = asInputRef(node);
        if (ref == null)
            return null;
        return ref.getIndex() < this.leftTableColumnCount;
    }

    @Override
    public Void visitLiteral(RexLiteral lit) {
        this.result.setLeftOver(lit);
        return null;
    }

    @Override
    public Void visitCall(RexCall call) {
        switch (call.op.kind) {
            case AND:
                call.operands.get(0).accept(this);
                if (this.completed()) {
                    // If we don't understand the left side we're done
                    // and the leftOver is this call.
                    this.result.setLeftOver(call);
                    return null;
                }
                // Recurse on the right side.
                call.operands.get(1).accept(this);
                return null;
            case EQUALS:
                RexNode left = call.operands.get(0);
                RexNode right = call.operands.get(1);
                @Nullable
                Boolean leftIsLeft = this.isLeftTableColumnReference(left);
                @Nullable
                Boolean rightIsLeft = this.isLeftTableColumnReference(right);
                if (leftIsLeft == null || rightIsLeft == null) {
                    this.result.setLeftOver(call);
                    return null;
                }
                if (leftIsLeft == rightIsLeft) {
                    // Both columns refer to the same table.
                    this.result.setLeftOver(call);
                    return null;
                }
                DBSPType leftType = this.typeCompiler.convertType(left.getType());
                DBSPType rightType = this.typeCompiler.convertType(right.getType());
                DBSPType resultType = ExpressionCompiler.reduceType(leftType, rightType).setMayBeNull(false);
                if (leftIsLeft) {
                    this.result.addEquality(left, right, resultType);
                } else {
                    this.result.addEquality(right, left, resultType);
                }
                return null;
            default:
                // We are done: we don't know how to handle this condition.
                this.result.setLeftOver(call);
                return null;
        }
    }

    JoinConditionAnalyzer.ConditionDecomposition analyze(RexNode expression) {
        Logger.instance.from(this, 1)
                .append("Analyzing ")
                .append(expression.toString())
                .newline();
        expression.accept(this);
        return this.result;
    }
}
