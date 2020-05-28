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

import org.jaudiolibs.audioops.impl.VariableDelayOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel delay.
 */
public final class Delay extends OpHolder {

    private final OpImpl op;
    
    /**
     * Create a delay with maximum delay time of 2 seconds.
     */
    public Delay() {
        this(new OpImpl(2));
    }
    
    Delay(OpImpl op) {
        super(op, 1);
        this.op = op;
        reset();
    }
    
    /**
     * Set the delay time, in seconds.
     * 
     * @param time delay time
     * @return this
     */
    public Delay time(double time) {
        op.setDelay((float) Utils.constrain(time, 0, 2));
        return this;
    }
    
    /**
     * Query the delay time.
     * 
     * @return delay time
     */
    public double time() {
        return op.getDelay();
    }
    
    /**
     * Set the feedback.
     * 
     * @param fb feedback 0..1, default 0
     * @return this
     */
    public Delay feedback(double fb) {
        op.setFeedback((float) Utils.constrain(fb, 0, 1));
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
    
    /**
     * Set the level.
     * 
     * @param level delay level 0..1, default 1
     * @return this
     */
    public Delay level(double level) {
        op.setGain((float) Utils.constrain(level, 0, 1));
        return this;
    }
    
    /**
     * Query the level.
     * 
     * @return level
     */
    public double level() {
        return op.getGain();
    }
    
    /**
     * Set passthrough, to include the input signal in the output.
     * 
     * @param passthrough whether to include input, default true
     * @return this
     */
    public Delay passthrough(boolean passthrough) {
        op.passthrough = passthrough;
        return this;
    }
    
    /**
     * Query the passthrough.
     * 
     * @return passthrough
     */
    public boolean passthrough() {
        return op.passthrough;
    }
    
    /**
     * Query the maximum delay available.
     * 
     * @return maximum delay
     */
    public double maxDelay() {
        return op.getMaxDelay();
    }
    
    @Override
    public void reset() {
        op.setDelay(0);
        op.setGain(1);
        op.setFeedback(0);
        op.passthrough = true;
    }
    
    private static class OpImpl extends VariableDelayOp {
        
        private boolean passthrough;
        
        private OpImpl(float maxDelay) {
            super(maxDelay);
        }

        @Override
        public void processReplace(int buffersize, float[][] outputs, float[][] inputs) {
            if (passthrough) {
                super.processAdd(buffersize, outputs, inputs);
            } else {
                super.processReplace(buffersize, outputs, inputs);
            }
        }
        
        
        
        
    }
    
}
