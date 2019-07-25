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
