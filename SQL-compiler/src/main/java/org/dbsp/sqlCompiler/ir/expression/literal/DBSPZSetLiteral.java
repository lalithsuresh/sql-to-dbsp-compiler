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

package org.dbsp.sqlCompiler.ir.expression.literal;

import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.expression.DBSPExpression;
import org.dbsp.sqlCompiler.ir.expression.IDBSPContainer;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.sqlCompiler.ir.type.DBSPTypeZSet;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a (constant) ZSet described by its elements.
 * A ZSet is a map from tuples to integer weights.
 * In general weights should not be zero.
 */
public class DBSPZSetLiteral extends DBSPLiteral implements IDBSPContainer {
    public final Map<DBSPExpression, Integer> data;
    public final DBSPTypeZSet zsetType;

    /**
     * Create a ZSet literal from a set of data values.
     * @param weightType  Type of weight used.
     * @param data Data to insert in zset - cannot be empty, since
     *             it is used to extract the zset type.
     *             To create empty zsets use the constructor
     *             with just a type argument.
     */
    public DBSPZSetLiteral(DBSPType weightType, DBSPExpression... data) {
        super(null, new DBSPTypeZSet(data[0].getNonVoidType(), weightType), 0);
        // value 0 is not used
        this.zsetType = this.getNonVoidType().to(DBSPTypeZSet.class);
        this.data = new HashMap<>();
        for (DBSPExpression e: data) {
            if (!e.getNonVoidType().sameType(data[0].getNonVoidType()))
                throw new RuntimeException("Not all values of set have the same type:" +
                    e.getType() + " vs " + data[0].getType());
            this.add(e);
        }
    }

    public DBSPZSetLiteral(DBSPExpression... data) {
        this(DBSPTypeZSet.defaultWeightType, data);
    }

    protected DBSPZSetLiteral(Map<DBSPExpression, Integer> data,
                              DBSPTypeZSet zsetType) {
        super(null, zsetType, 0);
        this.data = data;
        this.zsetType = zsetType;
    }

    public DBSPZSetLiteral(DBSPType type) {
        super(null, type, 0); // Value is unused, but needs to be non-null
        this.zsetType = this.getNonVoidType().to(DBSPTypeZSet.class);
        this.data = new HashMap<>();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public DBSPZSetLiteral clone() {
        return new DBSPZSetLiteral(new HashMap<>(this.data), this.zsetType);
    }

    public DBSPType getElementType() {
        return this.zsetType.elementType;
    }

    public void add(DBSPExpression expression) {
        this.add(expression, 1);
    }

    public void add(DBSPExpression expression, int weight) {
        // We expect the expression to be a constant value (a literal)
        if (!expression.getNonVoidType().sameType(this.getElementType()))
            throw new RuntimeException("Added element does not match zset type " +
                    expression.getType() + " vs " + this.getElementType());
        if (this.data.containsKey(expression)) {
            int oldWeight = this.data.get(expression);
            int newWeight = weight + oldWeight;
            if (newWeight == 0)
                this.data.remove(expression);
            else
                this.data.put(expression, weight + oldWeight);
            return;
        }
        this.data.put(expression, weight);
    }

    public void add(DBSPZSetLiteral other) {
        if (!this.getNonVoidType().sameType(other.getNonVoidType()))
            throw new RuntimeException("Added zsets do not have the same type " +
                    this.getElementType() + " vs " + other.getElementType());
        other.data.forEach(this::add);
    }

    public DBSPZSetLiteral negate() {
        DBSPZSetLiteral result = new DBSPZSetLiteral(this.zsetType);
        for (Map.Entry<DBSPExpression, Integer> entry: data.entrySet()) {
            result.add(entry.getKey(), -entry.getValue());
        }
        return result;
    }

    public int size() {
        return this.data.size();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        boolean first = true;
        for (Map.Entry<DBSPExpression, Integer> e: data.entrySet()) {
            if (!first)
                builder.append(", ");
            first = false;
            builder.append(e.getKey())
                    .append(" => ")
                    .append(e.getValue());
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        for (DBSPExpression expr: this.data.keySet())
            expr.accept(visitor);
        visitor.postorder(this);
    }

    public DBSPZSetLiteral minus(DBSPZSetLiteral sub) {
        DBSPZSetLiteral result = this.clone();
        result.add(sub.negate());
        return result;
    }

    @Override
    public boolean shallowSameExpression(DBSPExpression other) {
        if (this == other)
            return true;
        DBSPZSetLiteral fe = other.as(DBSPZSetLiteral.class);
        if (fe == null)
            return false;
        return this.data == fe.data;
    }
}
