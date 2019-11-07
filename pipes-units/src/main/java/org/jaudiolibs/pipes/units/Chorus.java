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

import org.jaudiolibs.audioops.impl.ChorusOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel Chorus unit.
 */
public final class Chorus extends OpHolder {

    private final ChorusOp op;

    /**
     * Create a Chorus unit.
     */
    public Chorus() {
        this(new ChorusOp());
    }

    Chorus(ChorusOp op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the depth of the chorus.
     *
     * @param depth
     * @return this
     */
    public Chorus depth(double depth) {
        op.setDepth((float) depth);
        return this;
    }

    /**
     * Query the depth of the chorus.
     * 
     * @return depth
     */
    public double depth() {
        return op.getDepth();
    }

    /**
     * Set the feedback of the chorus.
     * 
     * @param feedback
     * @return this
     */
    public Chorus feedback(double feedback) {
        op.setFeedback((float) feedback);
        return this;
    }

    /**
     * Query the feedback of the chorus.
     * 
     * @return feedback
     */
    public double feedback() {
        return op.getFeedback();
    }

    /**
     * Set the rate of the chorus.
     * 
     * @param rate
     * @return this
     */
    public Chorus rate(double rate) {
        op.setRate((float) rate);
        return this;
    }

    /**
     * Query the rate of the chorus.
     * 
     * @return rate
     */
    public double rate() {
        return op.getRate();
    }

    /**
     * Reset the depth, feedback and rate to 0.
     */
    @Override
    public void reset() {
        op.setDepth(0);
        op.setFeedback(0);
        op.setRate(0);
    }

}
