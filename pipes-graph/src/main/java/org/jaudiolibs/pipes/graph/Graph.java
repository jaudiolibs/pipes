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
 *
 * @author Neil C Smith - https://www.neilcsmith.net
 */
public abstract class Graph {

    
    final Pipe[] inputs;
    final Pipe[] outputs; 
    
    final List<Dependent> dependents;
    final Scheduler scheduler;
    
    double sampleRate;
    int blockSize;
    long samplePosition;

    
    protected Graph() {
        this(2,2);
    }
    
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
    
    
    protected abstract void init();
    
    protected void update() {
    }
    
    public long position() {
        return samplePosition;
    }
    
    public int blockSize() {
        return blockSize;
    }
    
    public double sampleRate() {
        return sampleRate;
    }
    
    public long millis() {
        return (long) (samplePosition / sampleRate * 1000);
    }
    
    public long nanos() {
        return (long) (samplePosition / sampleRate * 1000_000_000);
    }
    
    public void invokeLater(Runnable task) {
        scheduler.execute(task);
    }
    
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }
    
    protected void addDependent(Dependent dependent) {
        dependents.add(Objects.requireNonNull(dependent));
        dependent.attach(this);
    }
    
    protected void removeDependent(Dependent dependent) {
        dependents.remove(dependent);
        dependent.detach(this);
    }
    
    protected final Pipe in(int index) {
        return inputs[index];
    }
    
    protected final Pipe out(int index) {
        return outputs[index];
    }

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

    protected final Add add() {
        return new Add();
    }

    protected final Add add(Pipe... ugens) {
        Add add = new Add();
        for (Pipe ugen : ugens) {
            add.addSource(ugen);
        }
        return add;
    }

    protected final Mod mod() {
        return new Mod();
    }

    protected final Mod mod(Pipe... ugens) {
        Mod mod = new Mod();
        for (Pipe ugen : ugens) {
            mod.addSource(ugen);
        }
        return mod;
    }

    protected final Mod modFn(DoubleBinaryOperator function) {
        Mod mod = new Mod();
        mod.function(function);
        return mod;
    }

    protected final Mod modFn(Pipe pipe, DoubleBinaryOperator function) {
        Mod mod = modFn(function);
        mod.addSource(pipe);
        return mod;
    }

    protected final Tee tee() {
        return new Tee();
    }

    protected final Fn fn(DoubleUnaryOperator function) {
        return new Fn(function);
    }

    protected final double noteToFrequency(String note) {
        int midi = noteToMidi(note);
        if (midi < 0) {
            return 0;
        } else {
            return midiToFrequency(midi);
        }
    }

    protected final int noteToMidi(String note) {
        return NoteUtils.noteToMidi(note);
    }

    protected final double midiToFrequency(int midi) {
        return NoteUtils.midiToFrequency(midi);
    }

    protected double tabread(AudioTable table, double position) {
        return table == null ? 0 : table.get(0, position * table.size());
    }
    
    
    public static interface Dependent {
        
        public default void attach(Graph graph) {}
        
        public default void detach(Graph graph) {}
        
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
            ScheduledFutureTask<?> task = 
                    new ScheduledFutureTask<Void>(command, null,
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
