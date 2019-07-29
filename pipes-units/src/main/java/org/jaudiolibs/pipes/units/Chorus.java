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
package org.jaudiolibs.pipes.units;

import org.jaudiolibs.audioops.impl.ChorusOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public final class Chorus extends OpHolder {

    private final ChorusOp op;
    
    public Chorus() {
        this(new ChorusOp());
    }
    
    Chorus(ChorusOp op) {
        super(op, 1);
        this.op = op;
        reset();
    }
    
    public Chorus depth(double depth) {
        op.setDepth((float) depth);
        return this;
    }
    
    public double depth() {
        return op.getDepth();
    }
    
    public Chorus feedback(double feedback) {
        op.setFeedback((float) feedback);
        return this;
    }
    
    public double feedback() {
        return op.getFeedback();
    }
    
    public Chorus rate(double rate) {
        op.setRate((float) rate);
        return this;
    }
    
    public double rate() {
        return op.getRate();
    }
    
    @Override
    public void reset() {
        op.setDepth(0);
        op.setFeedback(0);
        op.setRate(0);
    }
    
}
