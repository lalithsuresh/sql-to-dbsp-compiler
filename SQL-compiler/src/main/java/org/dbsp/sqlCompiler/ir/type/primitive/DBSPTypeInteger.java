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

package org.dbsp.sqlCompiler.ir.type.primitive;

import org.dbsp.sqlCompiler.ir.InnerVisitor;
import org.dbsp.sqlCompiler.ir.expression.literal.DBSPIntegerLiteral;
import org.dbsp.sqlCompiler.ir.expression.literal.DBSPLiteral;
import org.dbsp.sqlCompiler.ir.expression.literal.DBSPLongLiteral;
import org.dbsp.sqlCompiler.ir.type.DBSPType;
import org.dbsp.sqlCompiler.ir.type.IsNumericType;
import org.dbsp.util.UnsupportedException;

import javax.annotation.Nullable;
import java.util.Objects;

public class DBSPTypeInteger extends DBSPTypeBaseType
        implements IsNumericType {
    private final int width;
    private final boolean signed;

    public static final DBSPTypeInteger signed16 = new DBSPTypeInteger(null, 16, true,false);
    public static final DBSPTypeInteger signed32 = new DBSPTypeInteger(null, 32, true,false);
    public static final DBSPTypeInteger signed64 = new DBSPTypeInteger(null, 64, true,false);
    public static final DBSPTypeInteger unsigned16 = new DBSPTypeInteger(null, 16, false,false);
    public static final DBSPTypeInteger unsigned32 = new DBSPTypeInteger(null, 32, false,false);
    public static final DBSPTypeInteger unsigned64 = new DBSPTypeInteger(null, 64, false,false);

    public DBSPTypeInteger(@Nullable Object node, int width, boolean signed, boolean mayBeNull) {
        super(node, mayBeNull);
        this.width = width;
        this.signed = signed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width);
    }

    @Override
    public String getRustString() {
        return (this.signed ? "i" : "u") + this.width;
    }

    @Override
    public DBSPLiteral getZero() {
        if (this.width <= 32) {
            return new DBSPIntegerLiteral(0, this.mayBeNull);
        } else {
            return new DBSPLongLiteral(0L, this.mayBeNull);
        }
    }

    @Override
    public DBSPLiteral getOne() {
        if (this.width <= 32) {
            return new DBSPIntegerLiteral(1, this.mayBeNull);
        } else {
            return new DBSPLongLiteral(1L, this.mayBeNull);
        }
    }

    @Override
    public DBSPLiteral getMaxValue() {
        switch (this.width) {
            case 16:
                return new DBSPIntegerLiteral((int)Short.MAX_VALUE, this.mayBeNull);
            case 32:
                return new DBSPIntegerLiteral(Integer.MAX_VALUE, this.mayBeNull);
            case 64:
                return new DBSPLongLiteral(Long.MAX_VALUE, this.mayBeNull);
            default:
                throw new UnsupportedException(this);
        }
    }

    @Override
    public DBSPLiteral getMinValue() {
        switch (this.width) {
            case 16:
                return new DBSPIntegerLiteral((int)Short.MIN_VALUE, this.mayBeNull);
            case 32:
                return new DBSPIntegerLiteral(Integer.MIN_VALUE, this.mayBeNull);
            case 64:
                return new DBSPLongLiteral(Long.MIN_VALUE, this.mayBeNull);
            default:
                throw new UnsupportedException(this);
        }
    }

    @Override
    public DBSPType setMayBeNull(boolean mayBeNull) {
        if (mayBeNull == this.mayBeNull)
            return this;
        return new DBSPTypeInteger(this.getNode(), this.width, this.signed, mayBeNull);
    }

    @Override
    public String shortName() {
        return (this.signed ? "i" : "u") + this.width;
    }

    public int getWidth() {
        return this.width;
    }

    @Override
    public boolean sameType(@Nullable DBSPType type) {
        if (!super.sameType(type))
            return false;
        assert type != null;
        if (!type.is(DBSPTypeInteger.class))
            return false;
        DBSPTypeInteger other = type.to(DBSPTypeInteger.class);
        return this.width == other.width && this.signed == other.signed;
    }

    @Override
    public void accept(InnerVisitor visitor) {
        if (!visitor.preorder(this)) return;
        visitor.postorder(this);
    }
}
