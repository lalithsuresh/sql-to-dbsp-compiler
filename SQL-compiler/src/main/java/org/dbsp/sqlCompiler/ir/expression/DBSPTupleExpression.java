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

import org.dbsp.sqlCompiler.compiler.frontend.ExpressionCompiler;
import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.sqlCompiler.ir.type.DBSPTypeTuple;
import org.dbsp.util.Linq;
import org.dbsp.util.Utilities;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DBSPTupleExpression extends DBSPBaseTupleExpression {
    public final DBSPExpression[] fields;

    public int size() { return this.fields.length; }

    public DBSPTupleExpression(@Nullable Object object, DBSPExpression... expressions) {
        super(object, new DBSPTypeTuple(Linq.map(expressions, DBSPExpression::getType, DBSPType.class)));
        this.fields = expressions;
    }

    public DBSPTupleExpression(DBSPExpression... expressions) {
        this(null, expressions);
    }

    /**
     * Extend this tuple by appending more fields.
     */
    public DBSPTupleExpression extend(DBSPExpression... extra) {
        DBSPExpression[] fields = new DBSPExpression[this.size() + extra.length];
        System.arraycopy(this.fields, 0, fields, 0, this.fields.length);
        System.arraycopy(extra, 0, fields, fields.length, extra.length);
        return new DBSPTupleExpression(fields);
    }

    public DBSPTupleExpression(List<DBSPExpression> fields) {
        this(null, fields.toArray(new DBSPExpression[0]));
    }

    public DBSPTupleExpression(@Nullable Object node, List<DBSPExpression> fields) {
        this(node, fields.toArray(new DBSPExpression[0]));
    }

    /**
     * @param expressions A list of expressions with tuple types.
     * @return  A tuple expressions that concatenates all fields of these tuple expressions.
     */
    public static DBSPTupleExpression flatten(DBSPExpression... expressions) {
        List<DBSPExpression> fields = new ArrayList<>();
        for (DBSPExpression expression: expressions) {
            DBSPTypeTuple type = expression.getNonVoidType().toRef(DBSPTypeTuple.class);
            for (int i = 0; i < type.size(); i++) {
                DBSPType fieldType = type.tupFields[i];
                DBSPExpression field = new DBSPFieldExpression(expression, i, fieldType).simplify();
                fields.add(field);
            }
        }
        return new DBSPTupleExpression(fields);
    }

    /**
     * Cast each element of the tuple to the corresponding type in the destination tuple.
     */
    public DBSPTupleExpression pointwiseCast(DBSPTypeTuple destType) {
        if (this.size() != destType.size())
            throw new RuntimeException("Cannot cast " + this + " with " + this.size() + " fields "
                    + " to " + destType + " with " + destType.size() + " fields");
        return new DBSPTupleExpression(
                Linq.zip(this.fields, destType.tupFields, ExpressionCompiler::makeCast, DBSPExpression.class));
    }

    public DBSPTupleExpression slice(int start, int endExclusive) {
        if (endExclusive <= start)
            throw new RuntimeException("Incorrect slice parameters " + start + ":" + endExclusive);
        return new DBSPTupleExpression(Utilities.arraySlice(this.fields, start, endExclusive));
    }

    public DBSPExpression get(int index) {
        return this.fields[index];
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        if (this.type != null)
            this.type.accept(visitor);
        for (DBSPExpression expression: this.fields)
            expression.accept(visitor);
        visitor.postorder(this);
    }

    @Override
    public boolean shallowSameExpression(DBSPExpression other) {
        if (this == other)
            return true;
        DBSPTupleExpression fe = other.as(DBSPTupleExpression.class);
        if (fe == null)
            return false;
        return Linq.same(this.fields, fe.fields);
    }
}
