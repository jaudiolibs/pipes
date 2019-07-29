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
package org.jaudiolibs.pipes;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Neil C Smith
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

    protected Pipe(int sourceCapacity, int sinkCapacity) {
        sources = sourceCapacity < 8 ? new ArrayList<>(sourceCapacity) : new ArrayList<>();
        sinks = sinkCapacity < 8 ? new ArrayList<>(sinkCapacity) : new ArrayList<>();
        buffers = sourceCapacity < 8 ? new ArrayList<>(sourceCapacity) : new ArrayList<>();
        this.sourceCapacity = sourceCapacity;
        this.sinkCapacity = sinkCapacity;
    }

    public final void addSource(Pipe source) {
        source.registerSink(this);
        try {
            registerSource(source);
        } catch (RuntimeException ex) {
            source.unregisterSink(this);
            throw ex;
        }
    }

    public final void removeSource(Pipe source) {
        source.unregisterSink(this);
        unregisterSource(source);
    }

    public int getSourceCount() {
        return sources.size();
    }

    public int getSourceCapacity() {
        return sourceCapacity;
    }

    public Pipe getSource(int idx) {
        return sources.get(idx);
    }

    public int getSinkCount() {
        return sinks.size();
    }

    public int getSinkCapacity() {
        return sinkCapacity;
    }

    public Pipe getSink(int idx) {
        return sinks.get(idx);
    }
    
    public void reset() {}

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

    protected abstract void process(List<Buffer> buffers);
    
    protected void skip(int samples) {
        // default no op hook
    }
    
    protected void writeOutput(List<Buffer> inputBuffers, Buffer outputBuffer, int sinkIndex) {
        if (sinkIndex < inputBuffers.size()) {
            outputBuffer.copy(inputBuffers.get(sinkIndex));
        } else {
            outputBuffer.clear();
        }
    }

    protected boolean isOutputRequired(Pipe source, long time) {
        return isProcessRequired(time);
    }

    protected boolean isProcessRequired(long time) {
        if (sinks.size() == 1) {
            return simpleOutputCheck(time);
        } else {
            return multipleOutputCheck(time);
        }
    }

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

    protected void unregisterSource(Pipe source) {
        sources.remove(source);
    }

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
