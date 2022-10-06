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

package org.dbsp.sqlCompiler.circuit.operator;

import org.dbsp.sqlCompiler.ir.Visitor;
import org.dbsp.sqlCompiler.ir.type.DBSPType;

import javax.annotation.Nullable;
import java.util.List;

public class DBSPSinkOperator extends DBSPOperator {
    public final String query;
    @Nullable
    public final String comment;

    public DBSPSinkOperator(@Nullable Object node,
                            String outputName, String query,
                            @Nullable String comment, DBSPOperator input) {
        super(node, "inspect", null, input.outputType, input.isMultiset, outputName);
        this.addInput(input);
        this.query = query;
        this.comment = comment;
    }

    public DBSPOperator input() {
        return this.inputs.get(0);
    }

    @Override
    public void accept(Visitor visitor) {
        if (!visitor.preorder(this)) return;
        if (this.function != null)
            this.function.accept(visitor);
        visitor.postorder(this);
    }

    @Override
    public DBSPOperator replaceInputs(List<DBSPOperator> newInputs, boolean force) {
        if (force || this.inputsDiffer(newInputs))
            return new DBSPSinkOperator(
                    this.getNode(), this.outputName, this.query, this.comment, newInputs.get(0));
        return this;
    }
}
