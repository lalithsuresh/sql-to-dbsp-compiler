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

package org.dbsp.sqlCompiler.ir.type;

import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.expression.*;
import org.dbsp.sqlCompiler.ir.path.DBSPPath;

import javax.annotation.Nullable;

/**
 * Base class for floating-point types.
 */
public abstract class DBSPTypeFP extends DBSPType implements IsNumericType {
    protected DBSPTypeFP(@Nullable Object node, boolean mayBeNull) { super(node, mayBeNull); }

    /**
     * Width in bits.
     */
    public abstract int getWidth();
    /**
     * Rust string describing this type, e.g., f32.
     */
    @Override
    public String getRustString() { return "f" + this.getWidth(); }

    protected DBSPExpression castFrom(DBSPExpression source, String destType) {
        // Recall: we ignore nullability of this.
        DBSPType noNull = this.setMayBeNull(false);
        DBSPType sourceType = source.getNonVoidType();
        DBSPTypeFunction func = new DBSPTypeFunction(noNull, source.getNonVoidType());
        if (sourceType.is(DBSPTypeFP.class)) {
            return new DBSPApplyExpression(
                    new DBSPPathExpression(func, new DBSPPath(destType, "from")), source);
        }
        if (sourceType.is(DBSPTypeDecimal.class)) {
            return new DBSPApplyExpression(
                    new DBSPPathExpression(func, new DBSPPath(destType, "from")),
                    new DBSPApplyMethodExpression("unwrap", this,
                            new DBSPApplyMethodExpression("to_f" + this.getWidth(), this.setMayBeNull(true), source)));
        }
        if (sourceType.is(DBSPTypeString.class)) {
            return new DBSPApplyMethodExpression("unwrap", this,
                    new DBSPApplyMethodExpression("parse", this.setMayBeNull(true), source));
        }
        return new DBSPApplyExpression(
                new DBSPPathExpression(func, new DBSPPath(destType, "from")),
                new DBSPAsExpression(source, this));
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        visitor.postorder(this);
    }
}
