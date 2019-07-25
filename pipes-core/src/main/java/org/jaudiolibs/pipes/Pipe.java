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
        while (buffers.size() > sources.size()) {
            buffers.remove(buffers.size() - 1);
        }
        for (int i = 0; i < sources.size(); i++) {
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
            sources.get(i).process(this, inputBuffer, time);
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
