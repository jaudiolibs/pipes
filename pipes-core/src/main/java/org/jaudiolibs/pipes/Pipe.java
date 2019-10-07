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
package org.jaudiolibs.pipes;

import java.util.ArrayList;
import java.util.List;

/**
 * The base type of the Pipes library. A Pipe is a unit generator, providing the
 * basic building blocks for synthesis and signal-processing. Pipes are joined
 * to each other in a graph-like structure of sources and sinks.
 */
public abstract class Pipe {

    private final List<Pipe> sources;
    private final List<Pipe> sinks;
    private final List<Buffer> buffers;
    private final int sourceCapacity;
    private final int sinkCapacity;

    private long time;
    private long renderReqTime;
    private boolean renderReqCache;
    private int renderIdx = 0;

    /**
     * Base constructor. The capacity, the maximum number of source and sink
     * pipes that can be connected, must be specified.
     *
     * @param sourceCapacity maximum number of sources
     * @param sinkCapacity maximum numbers of sinks
     */
    protected Pipe(int sourceCapacity, int sinkCapacity) {
        sources = sourceCapacity < 8 ? new ArrayList<>(sourceCapacity) : new ArrayList<>();
        sinks = sinkCapacity < 8 ? new ArrayList<>(sinkCapacity) : new ArrayList<>();
        buffers = sourceCapacity < 8 ? new ArrayList<>(sourceCapacity) : new ArrayList<>();
        this.sourceCapacity = sourceCapacity;
        this.sinkCapacity = sinkCapacity;
    }

    /**
     * Add another Pipe as a source.
     *
     * @param source pipe to add
     */
    public final void addSource(Pipe source) {
        source.registerSink(this);
        try {
            registerSource(source);
        } catch (RuntimeException ex) {
            source.unregisterSink(this);
            throw ex;
        }
    }

    /**
     * Remove a source Pipe.
     *
     * @param source pipe to remove
     */
    public final void removeSource(Pipe source) {
        source.unregisterSink(this);
        unregisterSource(source);
    }

    /**
     * Query the actual number of connected sources.
     *
     * @return number of sources
     */
    public int getSourceCount() {
        return sources.size();
    }

    /**
     * Query the maximum number of sources supported.
     *
     * @return maximum number of sources
     */
    public int getSourceCapacity() {
        return sourceCapacity;
    }

    /**
     * Get the source Pipe at the specified index, between 0 and
     * getSourceCount() - 1.
     *
     * @param idx index
     * @return source pipe at index
     */
    public Pipe getSource(int idx) {
        return sources.get(idx);
    }

    /**
     * Query the actual number of connected sinks.
     *
     * @return number of sinks
     */
    public int getSinkCount() {
        return sinks.size();
    }

    /**
     * Query the maximum number of sinks supported.
     *
     * @return maximum number of sinks
     */
    public int getSinkCapacity() {
        return sinkCapacity;
    }

    /**
     * Get the sink Pipe at the specified index, between 0 and getSinkCount() -
     * 1
     *
     * @param idx index
     * @return sink pipe at index
     */
    public Pipe getSink(int idx) {
        return sinks.get(idx);
    }

    /**
     * Reset the Pipe to its default state. This is an empty hook that may be
     * optionally implemented by sub-classes. The Pipe should set all
     * non-transient state to its default values - eg. the frequency of an
     * oscillator, but not its phase.
     */
    public void reset() {
    }

    /**
     * Process output for the requested sink and time.
     * <p>
     * Sub-classes should normally just implement
     * {@link #process(java.util.List)}
     *
     * @param sink requesting sink
     * @param outputBuffer buffer to write in to
     * @param time nanoseconds time of the requested buffer
     */
    protected void process(Pipe sink, Buffer outputBuffer, long time) {
        int sinkIndex = sinks.indexOf(sink);

        if (sinkIndex < 0) {
            // throw exception?
            return;
        }
        boolean inPlace = sinks.size() == 1 && sources.size() < 2;

        if (this.time != time) {
            boolean processRequired = isProcessRequired(time);
            this.time = time;
            if (inPlace) {
                processInPlace(outputBuffer, processRequired, time);
            } else {
                processCached(outputBuffer, processRequired, time);
            }
        }

        if (!inPlace && sink.isOutputRequired(this, time)) {
            writeOutput(buffers, outputBuffer, sinkIndex);
        }
    }

    private void processInPlace(Buffer outputBuffer, boolean processRequired, long time) {
        if (!buffers.isEmpty()) {
            buffers.forEach(Buffer::dispose);
            buffers.clear();
        }
        if (sources.isEmpty()) {
            outputBuffer.clear();
        } else {
            sources.get(0).process(this, outputBuffer, time);
        }
        if (processRequired) {
            buffers.add(outputBuffer);
            process(buffers);
            buffers.clear();
        } else {
            skip(outputBuffer.getSize());
        }
    }

    private void processCached(Buffer outputBuffer, boolean processRequired, long time) {
        int bufferCount = Math.max(sources.size(), sinks.size());
        while (buffers.size() > bufferCount) {
            buffers.remove(buffers.size() - 1);
        }
        for (int i = 0; i < bufferCount; i++) {
            Buffer inputBuffer;
            if (i < buffers.size()) {
                inputBuffer = buffers.get(i);
                if (!outputBuffer.isCompatible(inputBuffer)) {
                    inputBuffer.dispose();
                    inputBuffer = outputBuffer.createBuffer();
                    buffers.set(i, inputBuffer);
                }
            } else {
                inputBuffer = outputBuffer.createBuffer();
                buffers.add(inputBuffer);
            }
            if (i < sources.size()) {
                sources.get(i).process(this, inputBuffer, time);
            } else {
                inputBuffer.clear();
            }
        }
        if (processRequired) {
            process(buffers);
        } else {
            skip(outputBuffer.getSize());
        }
    }

    /**
     * Process buffers. Buffer data should be processed in-place. This method
     * will be called once per processing cycle (time). The buffer list will be
     * the maximum of source count or sink count. For buffers without a matching
     * input Pipe the buffer will be clear.
     *
     * @param buffers list of buffers to process
     */
    protected abstract void process(List<Buffer> buffers);

    /**
     * A hook called instead of {@link #process(java.util.List)} if no sink
     * requires output during processing. The number of samples being skipped
     * (equivalent to the non-processed buffer's size) is given.
     *
     * @param samples number of samples skipped
     */
    protected void skip(int samples) {
        // default no op hook
    }

    /**
     * Write data to the output buffer. This method is only called if there is
     * more than one sink Pipe and so the output buffer cannot be processed in
     * place. By default this method will write the data from the processed
     * input buffer at the same index if there is one, or otherwise clear the
     * buffer.
     *
     * @param inputBuffers list of processed input buffers
     * @param outputBuffer output buffer to write to
     * @param sinkIndex index of the sink owning the buffer
     */
    protected void writeOutput(List<Buffer> inputBuffers, Buffer outputBuffer, int sinkIndex) {
        if (sinkIndex < inputBuffers.size()) {
            outputBuffer.copy(inputBuffers.get(sinkIndex));
        } else {
            outputBuffer.clear();
        }
    }

    /**
     * Query whether this Pipe requires the output of the provided source Pipe.
     * This method is generally called by the source Pipe during processing. By
     * default, this method will return true if any sink of this Pipe requires
     * the output of this Pipe. Sub-classes can override this to switch off
     * processing eg. a mixer for silent channels
     *
     * @param source input source Pipe
     * @param time processing time in nanoseconds
     * @return true if this Pipe requires the output of the provided source
     */
    protected boolean isOutputRequired(Pipe source, long time) {
        return isProcessRequired(time);
    }

    /**
     * Query whether this Pipe requires processing for the processing cycle at
     * time. By default this method will check if any sink requires output.
     *
     * @param time processing time in nanoseconds
     * @return true if this Pipe should process buffers
     */
    protected boolean isProcessRequired(long time) {
        if (sinks.size() == 1) {
            return simpleOutputCheck(time);
        } else {
            return multipleOutputCheck(time);
        }
    }

    /**
     * Register the provided source with this Pipe. Called as part of
     * {@link #addSource(org.jaudiolibs.pipes.Pipe)}.
     *
     * @param source pipe being added as source
     */
    protected void registerSource(Pipe source) {
        if (source == null) {
            throw new NullPointerException();
        }
        if (sources.size() == sourceCapacity) {
            throw new SinkIsFullException();
        }
        if (sources.contains(source)) {
            throw new IllegalArgumentException();
        }
        sources.add(source);
    }

    /**
     * Unregister the provided source with this Pipe. Called as part of
     * {@link #removeSource(org.jaudiolibs.pipes.Pipe)}
     *
     * @param source pipe being removed as source
     */
    protected void unregisterSource(Pipe source) {
        sources.remove(source);
    }

    /**
     * Register the provided sink with this Pipe. Called as part of
     * {@link #addSource(org.jaudiolibs.pipes.Pipe)} being called on the sink
     * Pipe.
     *
     * @param sink pipe being added as sink
     */
    protected void registerSink(Pipe sink) {
        if (sink == null) {
            throw new NullPointerException();
        }
        if (sinks.size() == sinkCapacity) {
            throw new SourceIsFullException();
        }
        if (sinks.contains(sink)) {
            throw new IllegalArgumentException();
        }
        sinks.add(sink);
    }

    /**
     * Unregister the provided sink with this Pipe. Called as part of
     * {@link #removeSource(org.jaudiolibs.pipes.Pipe)} being called on the sink
     * Pipe.
     *
     * @param sink pipe being removed as sink
     */
    protected void unregisterSink(Pipe sink) {
        sinks.remove(sink);
    }

    private boolean simpleOutputCheck(long time) {
        if (time != renderReqTime) {
            renderReqTime = time;
            renderReqCache = sinks.get(0).isOutputRequired(this, time);
        }
        return renderReqCache;
    }

    private boolean multipleOutputCheck(long time) {
        if (renderIdx > 0) {
            while (renderIdx < sinks.size()) {
                if (sinks.get(renderIdx++).isOutputRequired(this, time)) {
                    renderIdx = 0;
                    return true;
                }
            }
            return false;
        } else {
            if (renderReqTime != time) {
                renderReqTime = time;
                renderReqCache = false;
                while (renderIdx < sinks.size()) {
                    if (sinks.get(renderIdx++).isOutputRequired(this, time)) {
                        renderReqCache = true;
                        break;
                    }
                }
                renderIdx = 0;
            }
            return renderReqCache;
        }
    }

}
