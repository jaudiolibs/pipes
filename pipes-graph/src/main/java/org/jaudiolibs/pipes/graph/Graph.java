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
package org.jaudiolibs.pipes.graph;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.jaudiolibs.pipes.Add;
import org.jaudiolibs.pipes.Pipe;
import org.jaudiolibs.pipes.Tee;
import org.jaudiolibs.pipes.units.AudioTable;
import org.jaudiolibs.pipes.units.Fn;
import org.jaudiolibs.pipes.units.Mod;

/**
 * A base audio graph class that replicates the audio API and UGen support
 * inside PraxisLIVE's audio nodes.#
 * <p>
 * Unless specifically noted, methods in this class are not thread-safe, and
 * should only be called on the audio thread.
 */
public abstract class Graph {

    final Pipe[] inputs;
    final Pipe[] outputs;

    final List<Dependent> dependents;
    final Scheduler scheduler;

    double sampleRate;
    int blockSize;
    long samplePosition;

    /**
     * Base Graph constructor for graph with default 2 channels of audio input
     * and output.
     */
    protected Graph() {
        this(2, 2);
    }

    /**
     * Base Graph constructor.
     *
     * @param inputCount number of input channels (can be zero)
     * @param outputCount number of output channels
     */
    protected Graph(int inputCount, int outputCount) {
        inputs = new Pipe[inputCount];
        outputs = new Pipe[outputCount];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = new Tee();
        }
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = new Add();
        }
        this.dependents = new CopyOnWriteArrayList<>();
        this.scheduler = new Scheduler(this);
        dependents.add(scheduler);
    }

    void handleInit() {
        samplePosition = -blockSize;
        init();
    }

    void handleUpdate() {
        samplePosition += blockSize;
        if (samplePosition < 0) {
            samplePosition = 0;
        }
        dependents.forEach(Dependent::update);
        update();
    }

    /**
     * Method called to initialize the audio graph.
     */
    protected abstract void init();

    /**
     * Optional update hook called before every buffer is processed.
     */
    protected void update() {
    }

    /**
     * Query the position in samples.
     *
     * @return position in samples
     */
    public long position() {
        return samplePosition;
    }

    /**
     * Query the audio block size / size of each processed buffer.
     *
     * @return block size
     */
    public int blockSize() {
        return blockSize;
    }

    /**
     * Query the sample rate in Hz.
     *
     * @return sample rate
     */
    public double sampleRate() {
        return sampleRate;
    }

    /**
     * Query the time in milliseconds, based on the sample playback clock.
     *
     * @return time in milliseconds
     */
    public long millis() {
        return (long) (samplePosition / sampleRate * 1000);
    }

    /**
     * Query the time in nanoseconds, based on the sample playback clock.
     *
     * @return time in nanoseconds
     */
    public long nanos() {
        return (long) (samplePosition / sampleRate * 1000_000_000);
    }

    /**
     * Add a task to be run on the audio thread before the next buffer is
     * processed. This method is safe to call from another thread.
     *
     * @param task task to run
     */
    public void invokeLater(Runnable task) {
        scheduler.execute(task);
    }

    /**
     * Get a {@link ScheduledExecutorService} for running tasks on the audio
     * thread. This method is safe to call from another thread. The scheduler
     * cannot be terminated and will throw an exception if attempting to
     * shutdown or await termination.
     *
     * @return executor service
     */
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    /**
     * Add a {@link Dependent} to the graph.
     *
     * @param dependent dependent to add
     */
    protected void addDependent(Dependent dependent) {
        dependents.add(Objects.requireNonNull(dependent));
        dependent.attach(this);
    }

    /**
     * Remove a {@link Dependent} from the graph.
     *
     * @param dependent dependent to remove
     */
    protected void removeDependent(Dependent dependent) {
        dependents.remove(dependent);
        dependent.detach(this);
    }

    /**
     * Access the Pipe for the specified input channel.
     *
     * @param index input channel
     * @return input Pipe
     */
    protected final Pipe in(int index) {
        return inputs[index];
    }

    /**
     * Access the Pipe for the specified output channel.
     *
     * @param index output channel
     * @return output Pipe
     */
    protected final Pipe out(int index) {
        return outputs[index];
    }

    /**
     * Link the provided list of units (Pipes) together. The last provided Pipe
     * is returned.
     *
     * @param ugens list of Pipes to connect
     * @return last Pipe
     */
    protected final Pipe link(Pipe... ugens) {
        int count = ugens.length;
        if (count < 1) {
            return null;
        }
        for (int i = ugens.length - 1; i > 0; i--) {
            ugens[i].addSource(ugens[i - 1]);
        }
        return ugens[ugens.length - 1];
    }

    /**
     * Create an {@link Add} Pipe. This unit type can add together multiple
     * inputs.
     *
     * @return add pipe
     */
    protected final Add add() {
        return new Add();
    }

    /**
     * Create an {@link Add} Pipe, and add the provided Pipes as inputs to it.
     * This unit type can add together multiple inputs. Additional inputs may be
     * added later.
     *
     * @param ugens list of Pipes to add
     * @return add pipe
     */
    protected final Add add(Pipe... ugens) {
        Add add = new Add();
        for (Pipe ugen : ugens) {
            add.addSource(ugen);
        }
        return add;
    }

    /**
     * Create a {@link Mod} pipe. The Mod pipe accepts multiple inputs and an
     * optional function to combine them. The default function is to multiply
     * samples together.
     *
     * @return mod pipe
     */
    protected final Mod mod() {
        return new Mod();
    }

    /**
     * Create a {@link Mod} pipe, and add the provided Pipes as inputs to it.
     * The Mod pipe accepts multiple inputs and an optional function to combine
     * them. The default function is to multiply samples together.
     *
     * @return mod pipe
     */
    protected final Mod mod(Pipe... ugens) {
        Mod mod = new Mod();
        for (Pipe ugen : ugens) {
            mod.addSource(ugen);
        }
        return mod;
    }

    /**
     * Create a {@link Mod} pipe that combines its inputs by applying the
     * provided function to each sample.
     *
     * @param function operation to apply to each sample
     * @return mod pipe
     */
    protected final Mod modFn(DoubleBinaryOperator function) {
        Mod mod = new Mod();
        mod.function(function);
        return mod;
    }

    /**
     * Create a {@link Mod} pipe that combines its inputs by applying the
     * provided function to each sample. The provided Pipe will be added as the
     * first input.
     *
     * @param pipe input pipe
     * @param function operation to apply to each sample
     * @return mod pipe
     */
    protected final Mod modFn(Pipe pipe, DoubleBinaryOperator function) {
        Mod mod = modFn(function);
        mod.addSource(pipe);
        return mod;
    }

    /**
     * Create a {@link Tee} pipe that can split its input to multiple outputs.
     * The input data is buffered, so the Tee may be connected to pipes that
     * feed into its input. This will create a delay of one block size.
     *
     * @return tee pipe
     */
    protected final Tee tee() {
        return new Tee();
    }

    /**
     * Create a {@link Fn} Pipe that applies the given operation to every
     * sample.
     *
     * @param function operation to apply to each sample
     * @return Fn pipe
     */
    protected final Fn fn(DoubleUnaryOperator function) {
        return new Fn(function);
    }

    /**
     * Convert the given note expressed as a String (eg. "a3", "C#5") into its
     * frequency. An invalid note will return 0 (useful for eg. "X" for note
     * off)
     *
     * @param note String representation of a note
     * @return frequency in Hz
     */
    protected final double noteToFrequency(String note) {
        int midi = noteToMidi(note);
        if (midi < 0) {
            return 0;
        } else {
            return midiToFrequency(midi);
        }
    }

    /**
     * Convert the given note expressed as a String (eg. "a3", "C#5") into its
     * equivalent MIDI note. An invalid note will return -1 (useful for eg. "X"
     * for note off)
     *
     * @param note String representation of a note
     * @return MIDI note number
     */
    protected final int noteToMidi(String note) {
        return NoteUtils.noteToMidi(note);
    }

    /**
     * Convert the given MIDI note number into its frequency in Hz.
     *
     * @param midi note number
     * @return frequency in Hz
     */
    protected final double midiToFrequency(int midi) {
        return NoteUtils.midiToFrequency(midi);
    }

    /**
     * Read from the first channel of the given {@link AudioTable} using a
     * normalized position (0 .. 1). Out of range values or a null table will
     * return silence. The returned sample is interpolated where required when
     * the position is between sample points.
     *
     * @param table audio data to read from
     * @param position position normalized 0 .. 1
     * @return sample from table
     */
    protected double tabread(AudioTable table, double position) {
        return table == null ? 0 : table.get(0, position * table.size());
    }

    /**
     * An interface for types that can be attached to a Graph and will be called
     * before every new buffer is processed.
     */
    public static interface Dependent {

        /**
         * Optional hook called when attached to the Graph.
         *
         * @param graph the Graph being added to
         */
        public default void attach(Graph graph) {
        }

        /**
         * Optional hook to be informed when removed from the graph.
         *
         * @param graph the Graph being removed from
         */
        public default void detach(Graph graph) {
        }

        /**
         * Called before every new buffer is processed.
         */
        public void update();

    }

    private static class Scheduler extends AbstractExecutorService implements ScheduledExecutorService, Dependent {

        private final Graph graph;
        private final ConcurrentLinkedQueue<Runnable> queue;
        private final DelayQueue<ScheduledFutureTask<?>> delayQueue;

        private long time;

        private Scheduler(Graph graph) {
            this.graph = graph;
            this.queue = new ConcurrentLinkedQueue<>();
            this.delayQueue = new DelayQueue<>();
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void execute(Runnable command) {
            queue.offer(command);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ScheduledFutureTask<?> task = new ScheduledFutureTask<Void>(command, null, unit.toNanos(delay), 0);
            queue.offer(task);
            return task;
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ScheduledFutureTask<V> task = new ScheduledFutureTask<>(callable, unit.toNanos(delay), 0);
            queue.offer(task);
            return task;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            ScheduledFutureTask<?> task
                    = new ScheduledFutureTask<Void>(command, null,
                            unit.toNanos(initialDelay), unit.toNanos(period));
            queue.offer(task);
            return task;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return scheduleAtFixedRate(command, initialDelay, delay, unit);
        }

        @Override
        public void update() {
            this.time = graph.nanos();
            Runnable r;
            while ((r = queue.poll()) != null) {
                if (r instanceof ScheduledFutureTask) {
                    ScheduledFutureTask<?> task = (ScheduledFutureTask<?>) r;
                    task.time = this.time + task.delay;
                    delayQueue.offer(task);
                } else {
                    r.run();
                }
            }
            while ((r = delayQueue.poll()) != null) {
                r.run();
            }
        }

        private class ScheduledFutureTask<T> extends FutureTask<T> implements RunnableScheduledFuture<T> {

            private final long delay;
            private final long period;

            private long time;

            ScheduledFutureTask(Runnable runnable, T result, long delay, long period) {
                super(runnable, result);
                this.delay = delay;
                this.period = period;
            }

            ScheduledFutureTask(Callable<T> callable, long delay, long period) {
                super(callable);
                this.delay = delay;
                this.period = period;
            }

            @Override
            public boolean isPeriodic() {
                return period > 0;
            }

            @Override
            public void run() {
                if (isPeriodic()) {
                    if (runAndReset()) {
                        time += period;
                        delayQueue.offer(this);
                    }
                } else {
                    super.run();
                }
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return unit.convert(time - graph.nanos(), TimeUnit.NANOSECONDS);
            }

            @Override
            public int compareTo(Delayed other) {
                if (other == this) {
                    return 0;
                }
                if (other instanceof ScheduledFuture) {
                    ScheduledFutureTask<?> o = (ScheduledFutureTask) other;
                    long diff = time - o.time;
                    return diff < 0 ? -1 : 1;
                } else {
                    throw new UnsupportedOperationException();
                }

            }

        }

    }
}
