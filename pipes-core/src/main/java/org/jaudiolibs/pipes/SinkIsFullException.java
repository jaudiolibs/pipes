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

/**
 * Exception thrown if no more sources can be added to a Pipe.
 */
public class SinkIsFullException extends RuntimeException {

    /**
     * Creates a new instance of <code>SinkIsFullException</code> without detail message.
     */
    public SinkIsFullException() {
    }


    /**
     * Constructs an instance of <code>SinkIsFullException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public SinkIsFullException(String msg) {
        super(msg);
    }
}
