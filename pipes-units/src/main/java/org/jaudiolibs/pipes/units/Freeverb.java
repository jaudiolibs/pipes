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

import org.jaudiolibs.audioops.impl.FreeverbOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A mono or stereo unit that processes audio through a Java port of the
 * Freeverb reverb.
 */
public final class Freeverb extends OpHolder {

    private final static float INITIAL_DAMP = 0.5f;
    private final static float INITIAL_DRY = 0.5f;
    private final static float INITIAL_ROOM_SIZE = 0.5f;
    private final static float INITIAL_WET = 0;
    private final static float INITIAL_WIDTH = 0.5f;

    private final FreeverbOp op;

    /**
     * Create a Freeverb unit supporting up to two channels.
     */
    public Freeverb() {
        this(new FreeverbOp(), 2);
    }

    private Freeverb(FreeverbOp op, int channels) {
        super(op, channels);
        this.op = op;
        reset();
    }

    /**
     * Set the damp value of the reverb. Default value 0.5.
     *
     * @param damp damp value
     * @return this for chaining
     */
    public Freeverb damp(double damp) {
        op.setDamp((float) Utils.constrain(damp, 0, 1));
        return this;
    }

    /**
     * Query the damp value.
     *
     * @return damp value
     */
    public double damp() {
        return op.getDamp();
    }

    /**
     * Set the amount of dry (unprocessed) signal in the output. Default value
     * 0.5.
     *
     * @param dry amount of dry (unprocessed) signal (0 .. 1)
     * @return this for chaining
     */
    public Freeverb dry(double dry) {
        op.setDry((float) Utils.constrain(dry, 0, 1));
        return this;
    }

    /**
     * Query the amount of dry (unprocessed) signal in the output.
     *
     * @return amount of dry signal
     */
    public double dry() {
        return op.getDry();
    }

    /**
     * Set the room size of the reverb. Default value 0.5.
     *
     * @param size room size (0 .. 1)
     * @return this for chaining
     */
    public Freeverb roomSize(double size) {
        op.setRoomSize((float) Utils.constrain(size, 0, 1));
        return this;
    }

    /**
     * Query the room size of the reverb.
     *
     * @return room size
     */
    public double roomSize() {
        return op.getRoomSize();
    }

    /**
     * Set the amount of wet (processed) signal in the output. Default value 0.
     *
     * @param wet amount of wet (processed) signal (0 .. 1)
     * @return this for chaining
     */
    public Freeverb wet(double wet) {
        op.setWet((float) Utils.constrain(wet, 0, 1));
        return this;
    }

    /**
     * Query the amount of wet (processed) signal in the output.
     *
     * @return amount of wet signal
     */
    public double wet() {
        return op.getWet();
    }

    /**
     * Set the width of the reverb. Default value 0.5.
     * 
     * @param width width of reverb (0 .. 1)
     * @return this for chaining
     */
    public Freeverb width(double width) {
        op.setWidth((float) Utils.constrain(width, 0, 1));
        return this;
    }

    /**
     * Query the width of the reverb.
     * 
     * @return width
     */
    public double width() {
        return op.getWidth();
    }

    @Override
    public void reset() {
        op.setDamp(INITIAL_DAMP);
        op.setDry(INITIAL_DRY);
        op.setRoomSize(INITIAL_ROOM_SIZE);
        op.setWet(INITIAL_WET);
        op.setWidth(INITIAL_WIDTH);
    }

}
