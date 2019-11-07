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

import java.util.List;
import org.jaudiolibs.pipes.Buffer;
import org.jaudiolibs.pipes.Pipe;

/**
 * A sample player unit supporting up to 16 output channels.
 */
public final class Player extends Pipe {

    private AudioTable table;
    private double in;
    private double out;
    private double speed;
    private boolean playing;
    private boolean looping;

    private double cursor;
    private double realSpeed;
    private int iIn, iOut;

    private Channel[] channels;

    /**
     * Create a Player unit.
     */
    public Player() {
        super(0, 16);
        channels = new Channel[]{new Channel()};
        reset();
    }

    /**
     * Set the {@link AudioTable} for this player. If the player has more
     * outputs than there are channels in the table, then channels are repeated
     * modulo for the output (eg. with 4 outputs and a stereo table, the first
     * table channel will be repeated for output 3 and the second for output 4).
     * Default null.
     *
     * @param table audio table to play
     * @return this for chaining
     */
    public Player table(AudioTable table) {
        if (table != this.table) {
            this.table = table;
            triggerSmoothing();
        }
        return this;
    }

    /**
     * Access the audio table.
     *
     * @return audio table, may be null
     */
    public AudioTable table() {
        return table;
    }

    /**
     * Set the looping start point, normalized between 0.0 and 1.0, where 0.0 is
     * the beginning of the audio table and 1.0 is the end of the table. Default
     * 0.0.
     *
     * @param in loop start point 0 .. 1
     * @return this for chaining
     */
    public Player in(double in) {
        if (in < 0) {
            in = 0;
        } else if (in > 1) {
            in = 1;
        }
        this.in = in;
        return this;
    }

    /**
     * Query the looping start point.
     *
     * @return loop start point
     */
    public double in() {
        return in;
    }

    /**
     * Set the looping end point, normalized between 0.0 and 1.0, where 0.0 is
     * the beginning of the audio table and 1.0 is the end of the table. Default
     * 1.0.
     *
     * @param out loop end point 0 .. 1
     * @return this for chaining
     */
    public Player out(double out) {
        if (out < 0) {
            out = 0;
        } else if (out > 1) {
            out = 1;
        }
        this.out = out;
        return this;
    }

    /**
     * Query the looping end point.
     *
     * @return loop end point
     */
    public double out() {
        return out;
    }

    /**
     * Change the playback position, normalized between 0.0 and 1.0, where 0.0
     * is the beginning of the audio table and 1.0 is the end of the table. This
     * is a transient property that is not affected by calling {@link #reset()}.
     *
     * @param position playback position
     * @return this for chaining
     */
    public Player position(double position) {
        if (position < 0) {
            position = 0;
        } else if (position > 1) {
            position = 1;
        }
        if (table != null) {
            cursor = position * table.size();
            triggerSmoothing();
        }
        return this;
    }

    /**
     * Query the playback position.
     *
     * @return playback position
     */
    public double position() {
        return table == null ? 0 : cursor / table.size();
    }

    /**
     * Set the playback speed. Negative values are supported. Default 1.0.
     *
     * @param speed playback speed
     * @return this for chaining
     */
    public Player speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Query the playback speed.
     *
     * @return playback speed
     */
    public double speed() {
        return speed;
    }

    /**
     * Set the player playing. This is a transient property that is not affected
     * by calling {@link #reset()}.
     *
     * @param playing player playing
     * @return this for chaining
     */
    public Player playing(boolean playing) {
        if (this.playing != playing) {
            triggerSmoothing();
        }
        this.playing = playing;
        return this;
    }

    /**
     * Query whether the player is currently playing.
     *
     * @return playing
     */
    public boolean playing() {
        return playing;
    }

    /**
     * Set whether the player should loop or stop when playback reaches the
     * {@link #in()} or {@link #out()} point.
     *
     * @param looping loop at loop points
     * @return this for chaining
     */
    public Player looping(boolean looping) {
        this.looping = looping;
        return this;
    }

    /**
     * Query whether the player is currently set to loop.
     *
     * @return looping
     */
    public boolean looping() {
        return looping;
    }

    /**
     * Start playing. Unlike {@link #playing(boolean)} this will also set the
     * position to the loop in position (or out position if speed is negative).
     *
     * @return this for chaining
     */
    public Player play() {
        if (speed < 0) {
            position(1);
        } else {
            position(0);
        }
        playing = true;
        return this;
    }

    /**
     * Stop playing.
     *
     * @return this for chaining
     */
    public Player stop() {
        return playing(false);
    }

    @Override
    public void reset() {
        table = null;
        in = 0;
        out = 1;
        speed = 1;
        looping = false;
    }

    @Override
    protected void process(List<Buffer> buffers) {
        if (buffers.size() != channels.length) {
            configureChannels(buffers.size());
        }
        if (table != null) {
            iIn = (int) (in * table.size());
            iOut = (int) (out * table.size());
            if (table.hasSampleRate()) {
                realSpeed = speed * (table.sampleRate() / buffers.get(0).getSampleRate());
            } else {
                realSpeed = speed;
            }
        } else {
            iIn = iOut = 0;
        }
        int loopLength = iOut - iIn;
        if (playing && loopLength > 0) {
            for (int i = 0; i < channels.length; i++) {
                channels[i].processPlaying(buffers.get(i), i % table.channels(), true);
            }
            cursor += (realSpeed * buffers.get(0).getSize());
            if (cursor > iOut) {
                if (looping) {
                    while (cursor > iOut) {
                        cursor -= loopLength;
                    }
                } else {
                    cursor = iIn;
                    playing = false;
                }
            } else if (cursor < iIn) {
                if (looping) {
                    while (cursor < iIn) {
                        cursor += loopLength;
                    }
                } else {
                    cursor = iOut - 1;
                    playing = false;
                }
            }
        } else {
            for (int i = 0; i < channels.length; i++) {
                channels[i].processStopped(buffers.get(i), 0, true);
            }
        }
    }

    @Override
    protected void skip(int samples) {
        int loopLength = iOut - iIn;
        if (playing && loopLength > 0) {
            cursor += (realSpeed * samples);
            if (cursor > iOut) {
                if (looping) {
                    while (cursor > iOut) {
                        cursor -= loopLength;
                    }
                } else {
                    cursor = iIn;
                    playing = false;
                }
            } else if (cursor < iIn) {
                if (looping) {
                    while (cursor < iIn) {
                        cursor += loopLength;
                    }
                } else {
                    cursor = iOut - 1;
                    playing = false;
                }
            }
        }
    }

    private void configureChannels(int count) {
        Channel[] old = channels;
        channels = new Channel[count];
        int copy = Math.min(old.length, channels.length);
        if (copy > 0) {
            System.arraycopy(old, 0, channels, 0, copy);
        }
        for (int i = copy; i < channels.length; i++) {
            channels[i] = new Channel();
        }
    }

    private void triggerSmoothing() {
        for (Channel c : channels) {
            c.smoothIndex = SMOOTH_AMOUNT;
        }
    }

    private final static int SMOOTH_AMOUNT = 256;

    private class Channel {

        private double previousSample = 0;
        private int smoothIndex = 0;

        private void processPlaying(Buffer buffer, int channel, boolean rendering) {
            if (!rendering) {
                smoothIndex = 0;
                return;
            }

            int bSize = buffer.getSize();
            float[] data = buffer.getData();
            double p = cursor;
            int loopLength = iOut - iIn;
            if (loopLength > 0 && table != null) {
                for (int i = 0; i < bSize; i++) {
                    if (p >= iOut) {
                        smoothIndex = SMOOTH_AMOUNT;
                        if (looping) {
                            while (p >= iOut) {
                                p -= loopLength;
                            }
                        } else {
                            processStopped(buffer, i, rendering);
                            return;
                        }
                    } else if (p < iIn) {
                        smoothIndex = SMOOTH_AMOUNT;
                        if (looping) {
                            while (p < iIn) {
                                p += loopLength;
                            }
                        } else {
                            processStopped(buffer, i, rendering);
                            return;
                        }
                    }
                    double sample = table.get(channel, p);
                    if (smoothIndex > 0) {
                        sample = smooth(sample);
                        smoothIndex--;
                    }
                    data[i] = (float) sample;
                    previousSample = sample;
                    p += realSpeed;

                }
            } else {
                if (previousSample != 0) {
                    smoothIndex = SMOOTH_AMOUNT;
                }
                processStopped(buffer, 0, rendering);
            }
        }

        private void processStopped(Buffer buffer, int offset, boolean rendering) {
            if (!rendering) {
                smoothIndex = 0;
                return;
            }

            int bSize = buffer.getSize();
            float[] data = buffer.getData();
            if (smoothIndex > 0) {
                for (int i = offset; i < bSize; i++) {
                    double sample = smooth(0);
                    previousSample = sample;
                    data[i] = (float) sample;
                    smoothIndex--;
                    if (sample == 0 || smoothIndex == 0) {
                        smoothIndex = 0;
                        offset = i;
                        break;
                    }
                }
            }
            for (int i = offset; i < bSize; i++) {
                data[i] = 0;
            }
        }

        private double smooth(double sample) {
            sample = sample - ((sample - previousSample)
                    * (double) smoothIndex / SMOOTH_AMOUNT);
            return sample;
        }

    }

}
