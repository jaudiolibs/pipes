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

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Neil C Smith
 */
public final class Buffer {

    private final float[] data;
    private final float sampleRate;
    private final int bufferSize;

    public Buffer(float sampleRate, int bufferSize) {
        if (sampleRate < 1 || bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.data = new float[bufferSize];

    }

    public float[] getData() {
        return data;
    }

    public boolean isCompatible(Buffer buffer) {
        return buffer.sampleRate == sampleRate
                && buffer.bufferSize == bufferSize;
    }

    public Buffer createBuffer() {
        return new Buffer(sampleRate, bufferSize);
    }

    public float getSampleRate() {
        return this.sampleRate;
    }

    public int getSize() {
        return this.bufferSize;
    }

    public void clear() {
        Arrays.fill(data, 0);
    }
    
    public void dispose() {}

    public void copy(Buffer source) {
        System.arraycopy(source.data, 0, data, 0, data.length);
    }
    
    public void add(Buffer source) {
        for (int i = 0; i < bufferSize; i++) {
            data[i] += source.data[i];
        }
    }

    public void mix(List<Buffer> sources) {
        if (sources.isEmpty()) {
            clear();
        } else {
            copy(sources.get(0));
            for (int i = 1; i < sources.size(); i++) {
                add(sources.get(i));
            }
        }
    }
    
}
