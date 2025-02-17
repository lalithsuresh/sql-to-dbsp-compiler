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

import org.dbsp.sqlCompiler.ir.type.DBSPType;

import javax.annotation.Nullable;

public abstract class DBSPTypeBaseType extends DBSPType {
    protected DBSPTypeBaseType(@Nullable Object node, boolean mayBeNull) {
        super(node, mayBeNull);
    }

    /**
     * Used when generating names for library functions that depend on argument types.
     * Here is a map of the short type names
     *
     * Bool -> b
     * Date -> date
     * Decimal -> decimal
     * Double -> d
     * Float -> f
     * GeoPoint -> geopoint
     * Null -> null
     * String -> s
     * Timestamp -> TS
     * isize -> i
     * signed16 -> i16
     * signed32 -> i32
     * signed64 -> i64
     * str -> str
     * usize -> u
     */
    public abstract String shortName();
}
