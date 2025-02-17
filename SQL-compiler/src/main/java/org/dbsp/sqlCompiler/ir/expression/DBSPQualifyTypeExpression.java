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

import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.util.Linq;

/**
 * An expression qualified with a type.
 */
public class DBSPQualifyTypeExpression extends DBSPExpression {
    public final DBSPExpression expression;
    public final DBSPType[] types;

    public DBSPQualifyTypeExpression(DBSPExpression expression, DBSPType... types) {
        super(null, expression.getType());
        this.expression = expression;
        this.types = types;
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        if (this.type != null)
            this.type.accept(visitor);
        this.expression.accept(visitor);
        for (DBSPType type: this.types)
            type.accept(visitor);
        visitor.postorder(this);
    }

    @Override
    public boolean shallowSameExpression(DBSPExpression other) {
        if (this == other)
            return true;
        DBSPQualifyTypeExpression fe = other.as(DBSPQualifyTypeExpression.class);
        if (fe == null)
            return false;
        return this.expression == fe.expression &&
                Linq.all(Linq.zipSameLength(this.types, fe.types, (l, r) -> l.sameType(r), Boolean.class));
    }
}
