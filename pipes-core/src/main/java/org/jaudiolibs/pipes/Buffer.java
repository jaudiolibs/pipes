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

import java.util.Arrays;
import java.util.List;

/**
 * An audio buffer class wrapping a float[] with additional size and sample rate
 * information.
 */
public final class Buffer {

    private final float[] data;
    private final float sampleRate;
    private final int bufferSize;

    /**
     * Create a Buffer. If requiring a new Buffer compatible with an existing
     * one, use {@link #createBuffer()} instead.
     *
     * @param sampleRate sample rate in Hz (must be greater than 1)
     * @param bufferSize buffer size (must be greater than 1)
     */
    public Buffer(float sampleRate, int bufferSize) {
        if (sampleRate < 1 || bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.data = new float[bufferSize];

    }

    /**
     * Access the audio data. This is a direct reference and will reflect
     * modifications. The size returned from {@link #getSize()} should be used
     * in preference to the array length.
     *
     * @return float[] of audio data
     */
    public float[] getData() {
        return data;
    }

    /**
     * Check whether a Buffer is compatible with this one. Will check for
     * matching sample rate and buffer size.
     *
     * @param buffer value to check
     * @return true if the buffer is compatible with this
     */
    public boolean isCompatible(Buffer buffer) {
        return buffer.sampleRate == sampleRate
                && buffer.bufferSize == bufferSize;
    }

    /**
     * Create a new compatible Buffer.
     *
     * @return new buffer
     */
    public Buffer createBuffer() {
        return new Buffer(sampleRate, bufferSize);
    }

    /**
     * Query the sample rate in Hz.
     *
     * @return sample rate in Hz
     */
    public float getSampleRate() {
        return this.sampleRate;
    }

    /**
     * Query the buffer size in samples.
     *
     * @return size in samples
     */
    public int getSize() {
        return this.bufferSize;
    }

    /**
     * Clear the Buffer. Fills the data with silence (zeroes).
     */
    public void clear() {
        Arrays.fill(data, 0);
    }

    /**
     * Dispose of the Buffer.
     */
    public void dispose() {
    }

    /**
     * Copy the content of the provided Buffer into this Buffer. The source
     * should be compatible with this buffer.
     *
     * @param source buffer to copy data from
     */
    public void copy(Buffer source) {
        System.arraycopy(source.data, 0, data, 0, data.length);
    }

    /**
     * Add the contents of the provided Buffer to this Buffer. Each sample will
     * be summed with the existing data. The source should be compatible with
     * this buffer.
     *
     * @param source buffer to add data from
     */
    public void add(Buffer source) {
        for (int i = 0; i < bufferSize; i++) {
            data[i] += source.data[i];
        }
    }

    /**
     * Mix the provided source Buffers into this Buffer. Any existing content of
     * this Buffer will be overwritten. With one or more sources, this method is
     * equivalent to calling {@link #copy(org.jaudiolibs.pipes.Buffer)} with the
     * first Buffer, and {@link #add(org.jaudiolibs.pipes.Buffer)} with every
     * other Buffer. If sources is empty, this is equivalent to calling
     * {@link #clear()}.
     *
     * @param sources list of source buffers
     */
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
