/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.jaudiolibs.pipes;

import java.util.List;
import java.util.Objects;
import org.jaudiolibs.audioops.AudioOp;

/**
 *
 * @author Neil C Smith
 */
public class OpHolder<T extends AudioOp> extends Pipe {

    private final T op;

    private float samplerate;
    private int buffersize;
    private int skipped;
    private boolean initialized;
    private float[][] dataHolder;

    public OpHolder(T op, int channels) {
        super(channels, channels);
        this.op = Objects.requireNonNull(op);
    }

    protected T getOp() {
        return op;
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
