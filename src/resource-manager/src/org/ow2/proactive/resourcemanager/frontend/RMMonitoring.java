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
package org.ow2.proactive.resourcemanager.frontend;

import org.objectweb.proactive.annotation.PublicAPI;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;


/**
 * An interface Front-End for the Monitoring of the Resource Manager.
 * This interface provides a way for a monitor to ask at
 * Resource Manager to throw events
 * generated by nodes and nodes sources management.
 * 
 * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener
 * @author The ProActive Team
 * @since ProActive 3.9
 */
@PublicAPI
public interface RMMonitoring {

    /** Echo function */
    /**
     * @return a String representing the RMmonitoring ID
     */
    public StringWrapper echo();

    /** Register a new Resource manager listener.
     * Way to a monitor object to ask at RMMonitoring to throw
     * RM events to it.
     * @param listener a listener object which implements {@link RMEventListener}
     * interface.
     * @param events list of wanted events that must be received.
     * @return RMInitialState snapshot of RM's current state : nodes and node sources.
     */
    public RMInitialState addRMEventListener(RMEventListener listener, RMEventType... events);
}
