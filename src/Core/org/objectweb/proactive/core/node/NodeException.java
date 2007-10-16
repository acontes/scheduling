/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.node;

import org.objectweb.proactive.core.ProActiveException;


/**
 * <p>
 * The <code>Node</code> interface offers a generic interface over various
 * implementations of the node such as RMI or HTTP, this exception offer a way
 * to wrap the various exceptions triggered by the implementation.
 * A <code>NodeException</code> is raised if a problem occured due to the remote
 * nature of the concrete implementation of the node.
 * </p>
 *
 * @author  ProActive Team
 * @version 1.0,  2001/10/23
 * @since   ProActive 0.9
 *
 */
public class NodeException extends ProActiveException {

    /**
     * Constructs a <code>NodeException</code> with no specified
     * detail message.
     */
    public NodeException() {
        super();
    }

    /**
     * Constructs a <code>NodeException</code> with the specified detail message.
     * @param s the detail message
     */
    public NodeException(String s) {
        super(s);
    }

    /**
     * Constructs a <code>NodeException</code> with the specified
     * detail message and nested exception.
     * @param s the detail message
     * @param t the nested exception
     */
    public NodeException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * Constructs a <code>NodeException</code> with the specified
     * detail message and nested exception.
     * @param t the nested exception
     */
    public NodeException(Throwable t) {
        super(t);
    }
}
