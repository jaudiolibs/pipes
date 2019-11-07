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

import org.jaudiolibs.audioops.impl.LFODelayOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel unit that processes sound through a variable length mono
 * delay where the delay time is controlled by a low frequency oscillator. This
 * unit is similar to {@link Chorus} but with more extreme parameters.
 */
public final class LFODelay extends OpHolder {

    private final LFODelayOp op;

    /**
     * Create an LFODelay unit.
     */
    public LFODelay() {
        this(new LFODelayOp());
    }

    private LFODelay(LFODelayOp op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the delay time in seconds. This is the centre point around which the
     * delay time oscillates. Default 0.0.
     *
     * @param time delay time in seconds, 0 .. 1
     * @return this for chaining
     */
    public LFODelay time(double time) {
        op.setDelay((float) Utils.constrain(time, 0, 1));
        return this;
    }

    /**
     * Query the delay time.
     *
     * @return delay time in seconds.
     */
    public double time() {
        return op.getDelay();
    }

    /**
     * Set the delay feedback. Default 0.0.
     *
     * @param feedback amount of feedback, 0 .. 1
     * @return this for chaining
     */
    public LFODelay feedback(double feedback) {
        op.setFeedback((float) Utils.constrain(feedback, 0, 1));
        return this;
    }

    /**
     * Query the delay feedback.
     *
     * @return feedback
     */
    public double feedback() {
        return op.getFeedback();
    }

//    public LFODelay phase(double phase) {
//        op.setPhase((float) phase);
//        return this;
//    }
//
//    public double phase() {
//        return op.getPhase();
//    }
    /**
     * Set the range of the delay time oscillation. A value of 0.0 will result
     * in no change of the delay time with oscillation. A value of 1.0 will
     * cause the delay time to oscillate between 0.0 seconds and twice the value
     * of {@link #time()}. Default 0.0.
     *
     * @param range delay time oscillation range, 0 .. 1
     * @return this for chaining
     */
    public LFODelay range(double range) {
        op.setRange((float) Utils.constrain(range, 0, 1));
        return this;
    }

    /**
     * Query the range of the delay time oscillation.
     *
     * @return range
     */
    public double range() {
        return op.getRange();
    }

    /**
     * Set the rate of the oscillator controlling the delay time, in Hz. Default
     * 0.0.
     *
     * @param rate oscillator rate in Hz
     * @return this for chaining
     */
    public LFODelay rate(double rate) {
        op.setRate((float) rate);
        return this;
    }

    /**
     * Query the oscillation rate.
     *
     * @return oscillator rate in Hz
     */
    public double rate() {
        return op.getRate();
    }

    @Override
    public void reset() {
        op.setDelay(0);
        op.setRange(0);
//        op.setPhase(0);
        op.setFeedback(0);
        op.setRate(0);
    }

}
