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
package org.jaudiolibs.pipes;

import java.util.List;
import java.util.Objects;
import org.jaudiolibs.audioops.AudioOp;

/**
 *
 * @author Neil C Smith
 */
public class OpHolder extends Pipe {

    private final AudioOp op;

    private float samplerate;
    private int buffersize;
    private int skipped;
    private boolean initialized;
    private float[][] dataHolder;

    public OpHolder(AudioOp op, int channels) {
        this(op, channels, channels);
    }
    
    public OpHolder(AudioOp op, int inputsChannels, int outputChannels) {
        super(inputsChannels, outputChannels);
        this.op = Objects.requireNonNull(op);
    }

    @Override
    protected boolean isOutputRequired(Pipe source, long time) {
        boolean output = super.isOutputRequired(source, time);
        return op.isInputRequired(output);
    }

    @Override
    protected void process(List<Buffer> buffers) {
        int bCount = buffers.size();
        if (bCount == 0) {
            skipped = -1;
            return;
        }
        Buffer buffer = buffers.get(0);
        if (!initialized
                || samplerate != buffer.getSampleRate()
                || buffersize < buffer.getSize()) {
            samplerate = buffer.getSampleRate();
            buffersize = buffer.getSize();
            op.initialize(samplerate, buffersize);
            initialized = true;
            skipped = 0;
        } else if (skipped != 0) {
            op.reset(skipped);
            skipped = 0;
        }
        if (dataHolder == null || dataHolder.length != bCount) {
            dataHolder = new float[bCount][];
        }
        for (int i = 0; i < bCount; i++) {
            dataHolder[i] = buffers.get(0).getData();
        }
        op.processReplace(buffer.getSize(), dataHolder, dataHolder);
    }

    @Override
    protected void skip(int samples) {
        if (skipped != -1) {
            skipped += samples;
        }
    }
    
    
}
