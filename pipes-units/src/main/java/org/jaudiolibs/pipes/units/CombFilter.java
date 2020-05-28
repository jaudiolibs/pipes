/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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

import org.jaudiolibs.audioops.impl.CombOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel comb filter.
 */
public final class CombFilter extends OpHolder {
    
    private final CombOp op;
    
    public CombFilter() {
        this(new CombOp());
    }
    
    CombFilter(CombOp op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Set the filter frequency.
     * 
     * @param frequency frequency in Hz, 20..20000, default 20
     * @return this
     */
    public CombFilter frequency(double frequency) {
        op.setFrequency((float) Utils.constrain(frequency, CombOp.MIN_FREQ, CombOp.MAX_FREQ));
        return this;
    }

    /**
     * Query the frequency.
     * 
     * @return frequency
     */
    public double frequency() {
        return op.getFrequency();
    }

    /**
     * Set the feedback.
     * 
     * @param feedback amount of feedback, 0..1, default 0
     * @return this
     */
    public CombFilter feedback(double feedback) {
        op.setFeedback((float) Utils.constrain(feedback, 0, 1));
        return this;
    }

    /**
     * Query the feedback.
     * 
     * @return feedback
     */
    public double feedback() {
        return op.getFeedback();
    }
    
    @Override
    public void reset() {
        op.setFrequency(CombOp.MIN_FREQ);
        op.setFeedback(0);
    }
    
}
