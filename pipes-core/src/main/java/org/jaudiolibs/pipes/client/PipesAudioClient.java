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
package org.jaudiolibs.pipes.client;

import java.nio.FloatBuffer;
import org.jaudiolibs.audioservers.AudioConfiguration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jaudiolibs.audioservers.AudioClient;
import org.jaudiolibs.pipes.Buffer;
import org.jaudiolibs.pipes.Pipe;

/**
 * An implementation of {@link AudioClient} that processes a graph of Pipes.
 */
public class PipesAudioClient implements AudioClient {

    private final static Logger LOG = Logger.getLogger(PipesAudioClient.class.getName());

    private final InputSource[] sources;
    private final OutputSink[] sinks;
    private final List<Listener> listeners;

    private float sampleRate;
    private int bufferSize;
    private int intBufferSize;
    private long time;
    private long bufferTime;
    private long avgBufferTime;
    private long startTime;
    private long bufferCount;

    /**
     * Create a PipesAudioClient supporting the specified number of inputs and
     * outputs. The internal buffer size (size of buffers in the Pipes graph)
     * will be the same as the buffer size of the audio client's configuration.
     *
     * @param inputs number of inputs (may be 0)
     * @param outputs number of outputs (at least 1)
     * @see
     * AudioClient#configure(org.jaudiolibs.audioservers.AudioConfiguration)
     */
    public PipesAudioClient(int inputs, int outputs) {
        this(0, inputs, outputs);
    }

    /**
     * Create a PipesAudioClient supporting the specified number of inputs and
     * outputs, and the provided internal buffer size for buffers in the Pipes
     * graph. The external buffer size of the audio client's configuration must
     * be a multiple of the internal buffer size.
     *
     * @param intBufferSize
     * @param inputs number of inputs (may be 0)
     * @param outputs number of outputs (at least 1)
     * @see
     * AudioClient#configure(org.jaudiolibs.audioservers.AudioConfiguration)
     */
    public PipesAudioClient(int intBufferSize, int inputs, int outputs) {
        if (intBufferSize < 0 || inputs < 0 || outputs < 1) {
            throw new IllegalArgumentException();
        }
        this.intBufferSize = intBufferSize;
        sinks = new OutputSink[outputs];
        for (int i = 0; i < outputs; i++) {
            sinks[i] = new OutputSink();
        }
        sources = new InputSource[inputs];
        for (int i = 0; i < inputs; i++) {
            sources[i] = new InputSource();
        }
        listeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Add a {@link Listener} to this PipesAudioClient.
     *
     * @param listener
     */
    public void addListener(Listener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
    }

    /**
     * Remove a {@link Listener} from this PipesAudioClient.
     *
     * @param listener
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Query the current time in nanoseconds.
     *
     * @return current time
     */
    public long getTime() {
        return time;
    }

    /**
     * Get the sink (output) at the specified index.
     *
     * @param index sink position
     * @return sink
     */
    public Pipe getSink(int index) {
        return sinks[index];
    }

    /**
     * Query the number of sinks (outputs) this audio client has available.
     *
     * @return number of sinks
     */
    public int getSinkCount() {
        return sinks.length;
    }

    /**
     * Get the source (input) at the specified index.
     *
     * @param index source position
     * @return source
     */
    public Pipe getSource(int index) {
        return sources[index];
    }

    /**
     * Query the number of sources (inputs) this audio client has available.
     *
     * @return number of sources
     */
    public int getSourceCount() {
        return sources.length;
    }

    /**
     * Disconnect all Pipes connected to this client.
     */
    public void disconnectAll() {
        for (InputSource source : sources) {
            if (source.getSinkCount() == 1) {
                source.getSink(0).removeSource(source);
            }
        }
        for (OutputSink sink : sinks) {
            if (sink.getSourceCount() == 1) {
                sink.removeSource(sink.getSource(0));
            }
        }
    }

    @Override
    public void configure(AudioConfiguration context) throws Exception {
        if (!context.isFixedBufferSize()) {
            throw new IllegalArgumentException("BusClient can currently only work with fixed buffer sizes.");
        }
        this.sampleRate = context.getSampleRate();
        this.bufferSize = context.getMaxBufferSize();

        // check internal buffer size
        if (intBufferSize != 0) {
            if (bufferSize % intBufferSize != 0) {
                throw new IllegalArgumentException("External buffersize is not a multiple of internal buffersize.");
            }
        } else {
            intBufferSize = bufferSize;
        }
        bufferTime = (long) ((intBufferSize / context.getSampleRate()) * 1000000000);
        avgBufferTime = bufferTime;
        bufferCount = 0;
        LOG.fine("Buffer time is " + bufferTime);
        // call conf listeners here - after our validation
        for (Listener listener : listeners) {
            listener.configure(context);
        }

        for (InputSource source : sources) {
            source.data = new float[intBufferSize];
        }
        for (OutputSink sink : sinks) {
            sink.buffer = new Buffer(sampleRate, intBufferSize);
        }
        int activeCount = Math.min(context.getOutputChannelCount(), sinks.length);
        for (int i = 0; i < activeCount; i++) {
            sinks[i].active = true;
        }
    }

    @Override
    public boolean process(long time, List<FloatBuffer> inputs, List<FloatBuffer> outputs, int nframes) {
        if (nframes != bufferSize) {
            // @TODO allow variable buffer sizes
            return false;
        }
        if ((time - this.time) < 0) {
            LOG.warning("Passed in time less than last time\nPassed in : " + time + "\nLast time : " + this.time);
        }

        int count = nframes / intBufferSize;

        time -= (count - 1) * avgBufferTime;
        for (int i = 0; i < count; i++) {
            setTime(time);
            readInput(inputs);
            fireListeners();
            processSinks();
            writeOutput(outputs);
            time += avgBufferTime;
        }
        return true;
    }

    private void setTime(long time) {
        this.time = time;
    }

    private void readInput(List<FloatBuffer> inputs) {
        int count = Math.min(inputs.size(), sources.length);
        for (int i = 0; i < count; i++) {
            FloatBuffer data = inputs.get(i);
            data.get(sources[i].data);
        }
    }

    private void fireListeners() {
        try {
            listeners.forEach(Listener::process);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Listener threw exception during process", ex);
        }
    }

    private void processSinks() {
        for (OutputSink sink : sinks) {
            sink.process();
        }
    }

    private void writeOutput(List<FloatBuffer> outputs) {
        int count = Math.min(sinks.length, outputs.size());
        for (int i = 0; i < count; i++) {
            float[] data = sinks[i].buffer.getData();
            FloatBuffer out = outputs.get(i);
            out.put(data, 0, intBufferSize);
        }
    }

    @Override
    public void shutdown() {
        for (OutputSink sink : sinks) {
            sink.active = false;
        }
        try {
            listeners.forEach(Listener::shutdown);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Listener threw exception during shutdown", ex);
        }
    }

    /**
     * An interface for listening on configuration, shutdown and every process
     * cycle of this client.
     */
    public static interface Listener {

        /**
         * Called when the client is configured.
         *
         * @param context configuration of the AudioServer
         * @throws Exception to reject the configuration
         */
        public default void configure(AudioConfiguration context) throws Exception {
        }

        /**
         * Called on every buffer process cycle before the Pipe graph is
         * processed.
         */
        public void process();

        /**
         * Called when the client is being shut down by the AudioServer.
         */
        public default void shutdown() {
        }
    ;

    }


    private final class OutputSink extends Pipe {

        private boolean active = false;
        private Buffer buffer;

        private OutputSink() {
            super(1, 1);
            registerSink(this);
        }

        @Override
        protected boolean isOutputRequired(Pipe source, long time) {
            return active;
        }

        private void process() {
            super.process(this, buffer, time);
        }

        @Override
        protected void process(List<Buffer> buffers) {
            // no op
        }

        @Override
        public int getSinkCapacity() {
            return 0;
        }

        @Override
        public int getSinkCount() {
            return 0;
        }

        @Override
        public Pipe getSink(int idx) {
            throw new IndexOutOfBoundsException();
        }

    }

    private class InputSource extends Pipe {

        private float[] data;

        private InputSource() {
            super(0, 1);
        }

        @Override
        protected void process(List<Buffer> buffers) {
            Buffer output = buffers.get(0);
            System.arraycopy(data, 0, output.getData(), 0, output.getSize());
        }

    }
}
