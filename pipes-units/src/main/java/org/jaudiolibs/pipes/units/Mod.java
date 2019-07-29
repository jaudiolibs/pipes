/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 *
 */
package org.jaudiolibs.pipes.units;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import org.jaudiolibs.pipes.Buffer;
import org.jaudiolibs.pipes.Pipe;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public final class Mod extends Pipe {

    private DoubleBinaryOperator function;

    public Mod() {
        super(32, 1);
    }

    public Mod function(DoubleBinaryOperator function) {
        this.function = function;
        return this;
    }
    
    @Override
    protected void process(List<Buffer> buffers) {
        int sourceCount = getSourceCount();
        if (sourceCount < 2) {
            return;
        }
        float[] out = buffers.get(0).getData();
        int size = buffers.get(0).getSize();
        for (int i = 1; i < getSourceCount(); i++) {
            float[] in = buffers.get(i).getData();
            if (function == null) {
                for (int k = 0; k < size; k++) {
                    out[k] *= in[k];
                }
            } else {
                for (int k = 0; k < size; k++) {
                    out[k] = (float) function.applyAsDouble(in[k], out[k]);
                }
            }
        }
    }
    
    @Override
    protected void writeOutput(List<Buffer> inputs, Buffer output, int index) {
        super.writeOutput(inputs, output, 0);

    }

    @Override
    public void reset() {
        function = null;
    }


}
