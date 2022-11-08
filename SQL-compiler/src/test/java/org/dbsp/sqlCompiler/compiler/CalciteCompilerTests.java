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
 *
 *
 */

package org.dbsp.sqlCompiler.compiler;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.dbsp.sqlCompiler.compiler.sqlparser.CalciteCompiler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the calcite compiler infrastructure.
 */
public class CalciteCompilerTests {
    static final CompilerOptions options = new CompilerOptions();

    @Test
    public void DDLTest() throws SqlParseException {
        CalciteCompiler calcite = new CalciteCompiler(options);
        String ddl = "CREATE TABLE T (\n" +
                "COL1 INT" +
                ", COL2 DOUBLE" +
                ", COL3 BOOLEAN" +
                ", COL4 VARCHAR" +
                ")";

        SqlNode node = calcite.parse(ddl);
        Assert.assertNotNull(node);
    }

    @Test
    public void DDLQueryTest() throws SqlParseException {
        CalciteCompiler calcite = new CalciteCompiler(options);
        String query = "CREATE TABLE R AS\n" +
                "SELECT cast(1 as int64) as primary_key,\n" +
                "       cast(1 as int64) as id, cast(\"a1\" as string) as a UNION ALL\n" +
                "  SELECT 2, 2, \"a2\"";
        SqlNode node = calcite.parse(query);
        Assert.assertNotNull(node);
    }
}
