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
 */
package org.jaudiolibs.pipes.graph;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

/**
 * A field type for triggers (actions) - see {@link T @T}. The Trigger type
 * provides a Linkable.Int for listening for triggers, and maintains a count of
 * each time the trigger has been called (useful for sequencing). It is also
 * possible to connect Runnable functions to be called on each trigger.
 */
public final class Clock implements Graph.Dependent {

    private final List<Link> links;
    
    private Graph graph;
    private int index;
    private int maxIndex;
    
    private double bpm = 120;
    private int subdivision = 4;
    private int bufferCount;
    private int position;

    public Clock() {
        this.links = new CopyOnWriteArrayList<>();
        maxIndex = Integer.MAX_VALUE;
    }

    @Override
    public void attach(Graph graph) {
        this.graph = graph;
        updatePulse();
    }
    
    @Override
    public void update() {
        if (position++ == 0) {
            fire();
        }
        position %= bufferCount;
    }
    
    public Clock bpm(double bpm) {
        this.bpm = bpm;
        updatePulse();
        return this;
    }
    
    public double bpm() {
        return bpm;
    }
    
    public Clock subdivision(int subdivision) {
        this.subdivision = subdivision;
        updatePulse();
        return this;
    }
    
    public int subdivision() {
        return subdivision;
    }
    
    
    /**
     * Clear all Linkables from this Trigger.
     *
     * @return this
     */
    public Clock clearLinks() {
        links.clear();
        return this;
    }

    /**
     * Run the provided Runnable each time this Trigger is triggered. This
     * method is shorthand for {@code on().link(i -> runnable.run());}.
     *
     * @param runnable function to run on trigger
     * @return this
     */
    public Clock link(Runnable runnable) {
        Link l = new Link();
        l.link(i -> runnable.run());
        return this;
    }

    /**
     * Returns a new {@link Linkable.Int} for listening to each trigger. The int
     * passed to the created linkable will be the same as index, incrementing
     * each time, wrapping at maxIndex.
     *
     * @return new Linkable.Int for reacting to triggers
     */
    public Linkable.Int on() {
        return new Link();
    }

    /**
     * Set the current index. Must not be negative.
     *
     * @param idx new index
     * @return this
     */
    public Clock index(int idx) {
        if (idx < 0) {
            throw new IllegalArgumentException("Index cannot be less than zero");
        }
        index = (idx % maxIndex);
        return this;
    }

    /**
     * Set the maximum index, at which the index will wrap back to zero.
     *
     * @param max maximum index
     * @return this
     */
    public Clock maxIndex(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Max index must be greater than 0");
        }
        maxIndex = max;
        index %= maxIndex;
        return this;
    }

    /**
     * Get the current index.
     *
     * @return current index
     */
    public int index() {
        return index;
    }

    /**
     * Get the current maximum index.
     *
     * @return maximum index
     */
    public int maxIndex() {
        return maxIndex;
    }

    private void fire() {
        fireLinks();
        incrementIndex();
    }

    private void fireLinks() {
        links.forEach(l -> l.fire(index));
    }

    private void incrementIndex() {
        index = (index + 1) % maxIndex;
    }

    void updatePulse() {
        if (graph == null) {
            return;
        }
        double secPerPulse = 1 / ((bpm * subdivision) / 60);
        double bufPerPulse = secPerPulse / (graph.blockSize() / graph.sampleRate());
        bufferCount = (int) (bufPerPulse + 0.5);
//        period = bufferCount * (blockSize / sampleRate);
//        actualBpm = 60 / subdivision * (1 / period);
    }
    
    
    private class Link implements Linkable.Int {

        private IntConsumer consumer;

        @Override
        public void link(IntConsumer consumer) {
            if (this.consumer != null) {
                throw new IllegalStateException("Cannot link multiple consumers in one chain");
            }
            this.consumer = Objects.requireNonNull(consumer);
            links.add(this);
        }

        private void fire(int value) {
            consumer.accept(value);
        }

    }
}
