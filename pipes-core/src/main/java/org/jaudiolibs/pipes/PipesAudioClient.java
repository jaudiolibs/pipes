/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 * 
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.jaudiolibs.pipes;

import java.nio.FloatBuffer;
import org.jaudiolibs.audioservers.AudioConfiguration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jaudiolibs.audioservers.AudioClient;

/**
 *
 * @author Neil C Smith
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
     *
     * @param inputs
     * @param outputs
     */
    public PipesAudioClient(int inputs, int outputs) {
        this(0, inputs, outputs);
    }

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

    public void addListener(Listener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public long getTime() {
        return time;
    }

    public Pipe getSink(int index) {
        return sinks[index];
    }

    public int getSinkCount() {
        return sinks.length;
    }

    public Pipe getSource(int index) {
        return sources[index];
    }

    public int getSourceCount() {
        return sources.length;
    }

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

    public static interface Listener {

        public void configure(AudioConfiguration context) throws Exception;
        
        public void process();

        public void shutdown();

    }


    private final class OutputSink extends Pipe {

        private boolean active = false;
        private Buffer buffer;

        private OutputSink() {
            super(1,1);
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
            super(0,1);
        }

        @Override
        protected void process(List<Buffer> buffers) {
            Buffer output = buffers.get(0);
            System.arraycopy(data, 0, output.getData(), 0, output.getSize());
        }

    }
}
