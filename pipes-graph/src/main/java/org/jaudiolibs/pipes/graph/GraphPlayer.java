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

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Field;
import org.jaudiolibs.audioservers.AudioConfiguration;
import org.jaudiolibs.audioservers.AudioServer;
import org.jaudiolibs.audioservers.AudioServerProvider;
import org.jaudiolibs.pipes.Pipe;
import org.jaudiolibs.pipes.client.PipesAudioClient;

/**
 *
 * @author Neil C Smith - https://www.neilcsmith.net
 */
public class GraphPlayer implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(GraphPlayer.class.getName());
    
    private static final float DEFAULT_SAMPLERATE = 48000;
    private static final int DEFAULT_BUFFERSIZE = 1024;
    private static final int DEFAULT_BLOCKSIZE = 64;
    private static final String DEFAULT_LIB = "JavaSound";
    
    private final Graph graph;
    private final String libName;
    private final float sampleRate;
    private final int bufferSize;
    private final int blockSize;
    private final AudioConfiguration config;
    private final PipesAudioClient client;
    private final AtomicReference<AudioServer> server;
    
    private GraphPlayer(Builder init) {
        this.graph = init.graph;
        this.libName = init.libName;
        this.sampleRate = init.sampleRate;
        this.bufferSize = init.bufferSize;
        this.blockSize = init.blockSize;
        this.config = new AudioConfiguration(sampleRate,
                graph.inputs.length,
                graph.outputs.length,
                bufferSize);
        this.client = new PipesAudioClient(blockSize,
                graph.inputs.length,
                graph.outputs.length);
        this.client.addListener(new ClientListener());
        this.server = new AtomicReference<>();
    }
    
    @Override
    public void run() {
        try {
            if (!server.compareAndSet(null, createServer())) {
                throw new IllegalStateException("GraphPlayer already started");
            }
            server.get().run();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception thrown running graph", ex);
        }
    }
    
    public void start() {
        Thread t = new Thread(this);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
    }
    
    public void shutdown() {
        server.get().shutdown();
    }
    
    private AudioServer createServer() throws Exception {
        AudioServerProvider provider = null;
        String lib = libName.isEmpty() ? DEFAULT_LIB : libName;
        for (AudioServerProvider p : ServiceLoader.load(AudioServerProvider.class)) {
            if (lib.equals(p.getLibraryName())) {
                provider = p;
                break;
            }
        }
        
        if (provider == null && libName.isEmpty()) {
            for (AudioServerProvider p : ServiceLoader.load(AudioServerProvider.class)) {
                provider = p;
                break;
            }
        }
        
        if (provider == null) {
            throw new IllegalStateException("No AudioServer found that matches : " + lib);
        }
        
        return provider.createServer(config, client);
    }
    
    
    private void handleConfigure(AudioConfiguration context) throws Exception {
        try {
            graph.blockSize = blockSize;
            graph.sampleRate = context.getSampleRate();
            connectIO();
            handleInjection();
            graph.handleInit();
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception thrown configuring graph", ex);
            throw ex;
        }
    }
    
    private void handleProcess() {
        graph.handleUpdate();
    }
    
    private void connectIO() {
        for (int i = 0; i < graph.inputs.length; i++) {
            graph.inputs[i].addSource(client.getSource(i));
        }
        for (int i = 0; i < graph.outputs.length; i++) {
            client.getSink(i).addSource(graph.outputs[i]);
        }
    }
    
    private void handleInjection() throws Exception {
        for (Field field : graph.getClass().getDeclaredFields()) {
            if (Pipe.class.isAssignableFrom(field.getType())
                    && (field.isAnnotationPresent(UGen.class)
                    || field.isAnnotationPresent(Inject.class))) {
                injectUnit(field, field.getType());
            } else if (Graph.Dependent.class.isAssignableFrom(field.getType())
                        && field.isAnnotationPresent(Inject.class)) {
                injectDependent(field, field.getType());
            }
        }
    }
    
    
    
    private void injectUnit(Field field, Class<?> unitClass) throws Exception {
        field.setAccessible(true);
        field.set(graph, unitClass.getDeclaredConstructor().newInstance());
    }
    
    private void injectDependent(Field field, Class<?> depClass) throws Exception {
        field.setAccessible(true);
        Graph.Dependent dep = (Graph.Dependent) depClass.getDeclaredConstructor().newInstance();
        graph.addDependent(dep);
        field.set(graph, dep);
    }
    
    private class ClientListener implements PipesAudioClient.Listener {

        @Override
        public void configure(AudioConfiguration context) throws Exception {
            handleConfigure(context);
        }
        
        @Override
        public void process() {
            handleProcess();
        }
        
    }
    
    public static Builder create(Graph graph) {
        return new Builder(graph);
    }
    
    public static class Builder {
        
        private final Graph graph;
        
        private String libName = "";
        private float sampleRate = DEFAULT_SAMPLERATE;
        private int bufferSize = DEFAULT_BUFFERSIZE;
        private int blockSize = DEFAULT_BLOCKSIZE;
        
        private Builder(Graph graph) {
            this.graph = graph;
        }
        
        public Builder library(String libName) {
            this.libName = Objects.requireNonNull(libName);
            return this;
        }
        
        public Builder sampleRate(float sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }
        
        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }
        
        public Builder blockSize(int blockSize) {
            this.blockSize = blockSize;
            return this;
        }
        
        public GraphPlayer build() {
            return new GraphPlayer(this);
        }
        
    }
    
}
