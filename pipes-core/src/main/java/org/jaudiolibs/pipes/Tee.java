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
 */
package org.jaudiolibs.pipes;

import java.util.List;

/**
 * Pipe that splits a single input to multiple outputs.
 */
public class Tee extends Pipe {

    /**
     * Create a Tee that supports a maximum of 64 outputs.
     */
    public Tee() {
        super(1, 64);
    }

    /**
     * Create a Tee that supports up to the specified number of outputs.
     *
     * @param maxOutputs maximum number of outputs
     */
    public Tee(int maxOutputs) {
        super(1, maxOutputs);
    }

    @Override
    protected void process(List<Buffer> buffers) {
        // no op
    }

    @Override
    protected void writeOutput(List<Buffer> inputBuffers, Buffer outputBuffer, int sinkIndex) {
        super.writeOutput(inputBuffers, outputBuffer, 0);
    }

}
