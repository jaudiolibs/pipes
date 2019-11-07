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

import org.jaudiolibs.audioops.impl.IIRFilterOp;
import org.jaudiolibs.pipes.OpHolder;

/**
 * A single channel unit that processed audio through an IIR filter.
 */
public final class IIRFilter extends OpHolder {

    /**
     * 6dB one pole low pass filter.
     */
    public final static Type LP6 = Type.LP6;

    /**
     * 12dB two pole low pass filter.
     */
    public final static Type LP12 = Type.LP12;
    /**
     * 24dB four pole low pass filter.
     */
    public final static Type LP24 = Type.LP24;
    /**
     * 12dB two pole high pass filter.
     */
    public final static Type HP12 = Type.HP12;
    /**
     * 24dB four pole high pass filter.
     */
    public final static Type HP24 = Type.HP24;
    /**
     * 12dB two pole band pass filter.
     */
    public final static Type BP12 = Type.BP12;
    /**
     * 12dB two pole notch pass filter.
     */
    public final static Type NP12 = Type.NP12;

    private final IIRFilterOp filter;

    /**
     * Create an IIRFilter unit.
     */
    public IIRFilter() {
        this(new IIRFilterOp());
    }

    private IIRFilter(IIRFilterOp filter) {
        super(filter, 1);
        this.filter = filter;
        reset();
    }

    /**
     * Set the filter frequency in Hz. Default 20,000.
     *
     * @param frequency filter frequency in Hz (20 .. 20,000)
     * @return this for chaining
     */
    public IIRFilter frequency(double frequency) {
        filter.setFrequency((float) Utils.constrain(frequency, 20, 20000));
        return this;
    }

    /**
     * Query the filter frequency in Hz.
     *
     * @return filter frequency
     */
    public double frequency() {
        return filter.getFrequency();
    }

    /**
     * Set the filter resonance in dB. Default 0.
     *
     * @param db filter resonance (0 .. 30)
     * @return this for chaining
     */
    public IIRFilter resonance(double db) {
        filter.setResonance((float) Utils.constrain(db, 0, 30));
        return this;
    }

    /**
     * Query the filter resonance in dB.
     *
     * @return resonance in dB
     */
    public double resonance() {
        return filter.getResonance();
    }

    /**
     * Set the filter type. Default {@link Type#LP6}
     *
     * @param type filter type
     * @return this for chaining
     */
    public IIRFilter type(Type type) {
        switch (type) {
            case LP6:
                filter.setFilterType(IIRFilterOp.Type.LP6);
                break;
            case LP12:
                filter.setFilterType(IIRFilterOp.Type.LP12);
                break;
            case LP24:
                filter.setFilterType(IIRFilterOp.Type.LP24);
                break;
            case HP12:
                filter.setFilterType(IIRFilterOp.Type.HP12);
                break;
            case HP24:
                filter.setFilterType(IIRFilterOp.Type.HP24);
                break;
            case BP12:
                filter.setFilterType(IIRFilterOp.Type.BP12);
                break;
            case NP12:
                filter.setFilterType(IIRFilterOp.Type.NP12);
                break;
        }
        return this;
    }

    /**
     * Query the filter type.
     * 
     * @return filter type
     */
    public Type type() {
        switch (filter.getFilterType()) {
            case LP6:
                return Type.LP6;
            case LP12:
                return Type.LP12;
            case LP24:
                return Type.LP24;
            case HP12:
                return Type.HP12;
            case HP24:
                return Type.HP24;
            case BP12:
                return Type.BP12;
            case NP12:
                return Type.NP12;
        }
        return LP6;
    }

    @Override
    public void reset() {
        resonance(0);
        frequency(20000);
        type(LP6);
    }

    /**
     * Filter type
     */
    public enum Type {

        /**
         * 6dB one pole low pass filter.
         */
        LP6,
        /**
         * 12dB two pole low pass filter.
         */
        LP12,
        /**
         * 24dB four pole low pass filter.
         */
        LP24,
        /**
         * 12dB two pole high pass filter.
         */
        HP12,
        /**
         * 24dB four pole high pass filter.
         */
        HP24,
        /**
         * 12dB two pole band pass filter.
         */
        BP12,
        /**
         * 12dB two pole notch pass filter.
         */
        NP12
    }

}
