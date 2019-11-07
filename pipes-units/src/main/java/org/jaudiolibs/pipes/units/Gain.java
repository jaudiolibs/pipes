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

import org.jaudiolibs.audioops.impl.GainOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel unit that multiplies the signal by a linear gain value.
 */
public final class Gain extends OpHolder {

    private final GainOp op;

    /**
     * Create a Gain unit.
     */
    public Gain() {
        this(new GainOp());
    }

    private Gain(GainOp op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the linear gain level. Default 1.0.
     *
     * @param level linear gain
     * @return this for chaining
     */
    public Gain level(double level) {
        op.setGain((float) level);
        return this;
    }

    /**
     * Query the linear gain value.
     * 
     * @return linear gain
     */
    public double level() {
        return op.getGain();
    }

    @Override
    public void reset() {
        op.setGain(1);
    }

}
