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
 * A single channel unit that processes audio through a simple overdrive effect.
 */
public final class Overdrive extends OpHolder {

    private final Op op;

    /**
     * Create an Overdrive unit.
     */
    public Overdrive() {
        this(new Op());
    }

    private Overdrive(Op op) {
        super(op, 1);
        this.op = op;
        reset();
    }

    /**
     * Drive amount. Default 0.0.
     *
     * @param amt drive 0 .. 1
     * @return this for chaining
     */
    public Overdrive drive(double amt) {
        op.setDrive(amt);
        return this;
    }

    /**
     * Query the drive.
     *
     * @return drive
     */
    public double drive() {
        return op.getDrive();
    }

    @Override
    public void reset() {
        op.setDrive(0);
    }

    private static class Op implements AudioOp {

        private double drive;

        @Override
        public void processReplace(int bufferSize, float[][] outputs, float[][] inputs) {
            float[] in = inputs[0];
            float[] out = outputs[0];
            double preMul = drive * 99 + 1;
            double postMul = 1 / (Math.log(preMul * 2) / Math.log(2));
            for (int i = 0; i < bufferSize; i++) {
                out[i] = (float) (Math.atan(in[i] * preMul) * postMul);
            }
        }

        @Override
        public void processAdd(int bufferSize, float[][] outputs, float[][] inputs) {
            float[] in = inputs[0];
            float[] out = outputs[0];
            double preMul = drive * 99 + 1;
            double postMul = 1 / (Math.log(preMul * 2) / Math.log(2));
            for (int i = 0; i < bufferSize; i++) {
                out[i] += (float) (Math.atan(in[i] * preMul) * postMul);
            }
        }

        public void setDrive(double drive) {
            this.drive = drive < 0 ? 0 : drive > 1 ? 1 : drive;
        }

        public double getDrive() {
            return drive;
        }

        @Override
        public void initialize(float sampleRate, int maxBufferSize) {
        }

        @Override
        public void reset(int skipped) {
        }

        @Override
        public boolean isInputRequired(boolean outputRequired) {
            return outputRequired;
        }

    }

}
