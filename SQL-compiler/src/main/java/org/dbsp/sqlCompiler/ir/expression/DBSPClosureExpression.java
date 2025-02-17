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

package org.dbsp.sqlCompiler.ir.expression;

import org.dbsp.sqlCompiler.circuit.DBSPNode;
import org.dbsp.sqlCompiler.circuit.IDBSPInnerNode;
import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.pattern.DBSPPattern;
import org.dbsp.sqlCompiler.ir.pattern.DBSPTuplePattern;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.sqlCompiler.ir.type.DBSPTypeFunction;
import org.dbsp.sqlCompiler.ir.type.DBSPTypeRawTuple;
import org.dbsp.sqlCompiler.ir.type.IHasType;
import org.dbsp.util.Linq;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * An expression of the form |var1, var2, ...| body.
 */
public class DBSPClosureExpression extends DBSPExpression {
    public final DBSPExpression body;
    public final Parameter[] parameters;

    public static class Parameter extends DBSPNode implements IHasType, IDBSPInnerNode {
        public final DBSPPattern pattern;
        @Nullable
        public final DBSPType type;

        public Parameter(DBSPPattern pattern, @Nullable DBSPType type) {
            super(null);
            this.pattern = pattern;
            this.type = type;
        }

        public Parameter(DBSPVariablePath... variables) {
            super(null);
            this.pattern = new DBSPTuplePattern(Linq.map(variables, DBSPVariablePath::asPattern, DBSPPattern.class));
            this.type = new DBSPTypeRawTuple(Linq.map(variables, DBSPExpression::getType, DBSPType.class));
        }

        @Nullable
        @Override
        public DBSPType getType() {
            return this.type;
        }

        @Override
        public void accept(InnerVisitor visitor) {
            if (!visitor.preorder(this)) return;
            if (this.type != null)
                this.type.accept(visitor);
            this.pattern.accept(visitor);
            visitor.postorder(this);
        }
    }

    public DBSPTypeFunction getFunctionType() {
        return this.getNonVoidType().to(DBSPTypeFunction.class);
    }

    @Nullable
    public DBSPType getResultType() {
        return this.getFunctionType().resultType;
    }

    public DBSPType getNonVoidResultType() {
        return Objects.requireNonNull(this.getFunctionType().resultType);
    }

    public DBSPClosureExpression(@Nullable Object node, DBSPExpression body, Parameter... variables) {
        // In Rust in general we can't write the type of a closure.
        super(node, new DBSPTypeFunction(body.getType(), Linq.map(variables, Parameter::getType, DBSPType.class)));
        this.body = body;
        this.parameters = variables;
    }

    DBSPClosureExpression(DBSPExpression body, Parameter... variables) {
        this(null, body, variables);
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        if (this.type != null)
            this.type.accept(visitor);
        for (Parameter param: this.parameters)
            param.accept(visitor);
        this.body.accept(visitor);
        visitor.postorder(this);
    }

    @Override
    public boolean shallowSameExpression(DBSPExpression other) {
        if (this == other)
            return true;
        DBSPClosureExpression cl = other.as(DBSPClosureExpression.class);
        if (cl == null)
            return false;
        return this.body == cl.body &&
                Linq.same(this.parameters, cl.parameters);
    }
}