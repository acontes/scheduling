/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2006 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.examples.jmx.remote.management.status;

import java.io.Serializable;


/**
 *
 * @author vlegrand
 *
 */
public class Status implements Serializable {

    /**
         *
         */
    private static final long serialVersionUID = -3582190320483420821L;
    public static final int OK = 0;
    public static final int ERR = 1;
    public static final int MULTIPLE = 2;
    public static final int TRANSACTION = 3;
    protected String message;
    protected String command;
    protected int state;
    protected String url;

    /**
     *
     *
     */
    public Status() {
    }

    /**
     *
     * @param state
     * @param url
     * @param command
     * @param message
     */
    public Status(int state, String command, String message, String url) {
        this.command = command;
        this.state = state;
        this.message = message;
        this.url = url;
    }

    /**
     *
     * @return
     */
    public String getMessage() {
        return this.message;
    }

    /**
     *
     * @return
     */
    public int getState() {
        return this.state;
    }

    /**
     *
     * @return
     */
    public String getCommand() {
        return this.command;
    }

    /**
     *
     * @return
     */
    public boolean containsErrors() {
        return (this.state == ERR);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }
}
