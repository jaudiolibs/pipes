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

import org.jaudiolibs.audioops.AudioOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel sawtooth oscillator unit with controllable range.
 */
public final class Phasor extends OpHolder {

    private final static float DEFAULT_FREQUENCY = 1;

    private final Op op;

    /**
     * Create a Phasor unit.
     */
    public Phasor() {
        this(new Op());
    }

    private Phasor(Op op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the phasor frequency in Hz. Default 1.0.
     *
     * @param frequency phasor frequency in Hz
     * @return this for chaining
     */
    public Phasor frequency(double frequency) {
        op.setFrequency((float) frequency);
        return this;
    }

    /**
     * Query the phasor frequency.
     *
     * @return frequency in Hz
     */
    public double frequency() {
        return op.getFrequency();
    }

    /**
     * Set the lower bound of the phasor range. Default 0.0.
     *
     * @param minimum lower bound of phasor range
     * @return this for chaining
     */
    public Phasor minimum(double minimum) {
        op.setMinimum((float) minimum);
        return this;
    }

    /**
     * Query the lower bound of the phasor range.
     *
     * @return lower bound of phasor range
     */
    public double minimum() {
        return op.getMinimum();
    }

    /**
     * Set the upper bound of the phasor range. Default 1.0.
     *
     * @param maximum upper bound of the phasor range
     * @return this for chaining
     */
    public Phasor maximum(double maximum) {
        op.setMaximum((float) maximum);
        return this;
    }

    /**
     * Query the upper bound of the phasor range
     *
     * @return upper bound of phasor range
     */
    public double maximum() {
        return op.getMaximum();
    }

    /**
     * Set the phase of the oscillation, between 0 and TWO_PI. This is a
     * transient property and not affected by {@link #reset()}.
     *
     * @param phase
     * @return this for chaining
     */
    public Phasor phase(double phase) {
        op.setPhase((float) phase);
        return this;
    }

    /**
     * Query the current phase of the oscillation.
     *
     * @return current phase between 0 and TWO_PI
     */
    public double phase() {
        return op.getPhase();
    }

    @Override
    public void reset() {
        op.setFrequency(DEFAULT_FREQUENCY);
        op.setMinimum(0);
        op.setMaximum(1);
    }

    private static class Op implements AudioOp {

        private final static float TWOPI = (float) (2 * Math.PI);

        private float phase;
        private float phaseIncrement;
        private float freq;
        private float srate;
        private float minimum;
        private float maximum;

        private Op() {
            this.freq = DEFAULT_FREQUENCY;
            this.minimum = 0;
            this.maximum = 1;
        }

        public void setFrequency(float frequency) {
            this.freq = frequency;
            updateIncrement();
        }

        public float getFrequency() {
            return this.freq;
        }

        public void setMinimum(float minimum) {
            this.minimum = minimum;
        }

        public float getMinimum() {
            return minimum;
        }

        public void setMaximum(float maximum) {
            this.maximum = maximum;
        }

        public float getMaximum() {
            return maximum;
        }

        public void setPhase(float phase) {
            this.phase = Math.abs(phase) % TWOPI;
        }

        public float getPhase() {
            return phase;
        }

        @Override
        public void initialize(float samplerate, int buffersize) {
            this.srate = samplerate;
            this.phase = 0;
            updateIncrement();
        }

        @Override
        public void reset(int i) {
            phase = 0; // increment phase using i
        }

        @Override
        public boolean isInputRequired(boolean bln) {
            return false;
        }

        @Override
        public void processReplace(int buffersize, float[][] outputs, float[][] inputs) {
            float[] out = outputs[0];
            for (int i = 0; i < buffersize; i++) {
                out[i] = nextSample();
            }
        }

        @Override
        public void processAdd(int buffersize, float[][] outputs, float[][] inputs) {
            float[] out = outputs[0];
            for (int i = 0; i < buffersize; i++) {
                out[i] += nextSample();
            }
        }

        private void updateIncrement() {
            float inc = 0;
            if (srate > 0) {
                inc = TWOPI * freq / srate;
                if (inc < 0) {
                    inc = 0;
                }
            }
            phaseIncrement = inc;
        }

        private float nextSample() {
            float value = (phase / TWOPI) * (maximum - minimum) + minimum;
            phase += phaseIncrement;
            while (phase >= TWOPI) {
                phase -= TWOPI;
            }
            return value;
        }

    }

}
