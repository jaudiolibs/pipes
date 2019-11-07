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
package org.jaudiolibs.pipes.units;

import java.util.ServiceLoader;
import java.io.IOException;
import java.net.URI;

/**
 * A table containing audio data. An AudioTable supports multiple channels of
 * audio and optional sample rate information.
 */
public class AudioTable {

    private final float[] data;
    private final double sampleRate;
    private final int channels;
    private final int size;

    private AudioTable(float[] data, double sampleRate, int channels, int size) {
        this.data = data;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.size = size;
    }

//    /**
//     * Get direct access to the audio data as an interleaved float array.
//     *
//     * @return audio data
//     */
//    public float[] data() {
//        return data;
//    }
    /**
     * Query whether this AudioTable has a sample rate.
     *
     * @return true if the sample rate is set
     */
    public boolean hasSampleRate() {
        return sampleRate > 0.5;
    }

    /**
     * Query the sample rate of the data in this AudioTable. If no sample rate
     * is set this method returns 0.
     *
     * @return sample rate
     */
    public double sampleRate() {
        return sampleRate;
    }

    /**
     * Query the number of channels in the audio data.
     *
     * @return number of channels
     */
    public int channels() {
        return channels;
    }

    /**
     * Query the size (length) of the audio data in samples. This is the size of
     * the data in each channel.
     *
     * @return size in samples
     */
    public int size() {
        return size;
    }

    /**
     * Set the sample value at the specified sample index of the specified
     * channel.
     *
     * @param channel audio channel
     * @param index position in samples
     * @param value sample value
     */
    public void set(int channel, int index, double value) {
        data[(index * channels) + channel] = (float) value;
    }

    /**
     * Get the sample value at the specified sample index of the specified
     * channel.
     *
     * @param channel audio channel
     * @param idx position in samples
     * @return sample value
     */
    public double get(int channel, int idx) {
        return data[(idx * channels) + channel];
    }

    /**
     * Get the sample value at the specified sample position of the specified
     * channel. This method handles non-integral positions, returning an
     * interpolated sample value.
     *
     * @param channel audio channel
     * @param pos position in samples
     * @return sample value
     */
    public double get(int channel, double pos) {
        int iPos = (int) pos;
        double frac;
        double a, b, c, d;
        double cminusb;
        if (iPos < 1) {
            iPos = 1;
            frac = 0;
        } else if (iPos > (size() - 3)) {
            iPos = size() - 3;
            frac = 1;
        } else {
            frac = pos - iPos;
        }
        a = get(channel, iPos - 1);
        b = get(channel, iPos);
        c = get(channel, iPos + 1);
        d = get(channel, iPos + 2);
        cminusb = c - b;
        return b + frac * (cminusb - 0.5f * (frac - 1) * ((a - d + 3.0f * cminusb) * frac + (b - a - cminusb)));
    }

    /**
     * Generate an empty AudioTable with the specified size and channel count.
     *
     * @param size channel size in samples
     * @param channels number of channels
     * @return audio table
     */
    public static AudioTable generate(int size, int channels) {
        return new AudioTable(new float[size * channels], 0, channels, size);
    }

    /**
     * Wrap the data from another AudioTable in a table of a smaller size.
     *
     * @param original audio table to get data from
     * @param size new size in samples (>= original.size() )
     * @return audio table
     */
    public static AudioTable wrap(AudioTable original, int size) {
        if (size < 0 || size > original.size) {
            throw new IllegalArgumentException();
        }
        return new AudioTable(original.data, original.sampleRate, original.channels, size);
    }

    /**
     * Wrap a float array in an AudioTable with the specified sample rate and
     * channel count. For multiple channels the data must be interleaved in the
     * array. The data is not copied - changes to the array will be reflected in
     * the table.
     *
     * @param data array of sample date to wrap
     * @param sampleRate sample rate of data (0 for none)
     * @param channels number of channels
     * @return audio table
     */
    public static AudioTable wrap(float[] data, double sampleRate, int channels) {
        return new AudioTable(data, sampleRate, channels, data.length / channels);
    }

    /**
     * Load an AudioTable from the specified URI. An instance of {@link Loader}
     * must be available via {@link ServiceLoader} that can handle the given
     * URI.
     *
     * @param source URI of source for audio data
     * @return audio table
     * @throws IOException in case of loading error or no loader available that
     * can handle given URI
     */
    public static AudioTable load(URI source) throws IOException {
        ServiceLoader<Loader> loaders = ServiceLoader.load(Loader.class);
        Exception ex = null;
        for (Loader loader : loaders) {
            try {
                AudioTable table = loader.load(source);
                if (table != null) {
                    return table;
                }
            } catch (Exception exx) {
                ex = exx;
            }
        }
        if (ex != null) {
            throw new IOException(ex);
        } else {
            throw new IOException("No loader found for " + source);
        }
    }

    /**
     * Instances of Loader should be registered for use via
     * {@link ServiceLoader} to handle loading an AudioTable from the given URI.
     * Registered loaders will be tried in turn until one can handle the URI.
     */
    public static interface Loader {

        /**
         * Load an AudioTable from the given URI. A loader may return null or
         * throw an Exception if it cannot handle the given URI.
         *
         * @param source URI of source for audio data
         * @return AudioTable
         * @throws Exception in case of loading error
         */
        AudioTable load(URI source) throws Exception;

    }

}
