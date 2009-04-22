/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
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
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.nodesource.policy;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMEventListener;
import org.ow2.proactive.resourcemanager.frontend.RMMonitoring;


/**
 *
 * Resource manages aware node source policy.
 * Implements resource manager listener interface and register itself to
 * RmMonitoring.
 *
 */
public abstract class RMAwareNodeSourcePolicy extends NodeSourcePolicy implements RMEventListener {

    private boolean rmShuttingDown = false;
    protected RMInitialState initialState;
    protected RMMonitoring rmMonitoring;

    /**
     * Activates the policy.
     * @return true if the policy has been activated successfully, false otherwise.
     */
    public BooleanWrapper activate() {
        rmMonitoring = nodeSource.getRMCore().getMonitoring();
        initialState = rmMonitoring.addRMEventListener((RMEventListener) PAActiveObject.getStubOnThis());
        return new BooleanWrapper(true);
    }

    /**
     * Disactivates the policy. Clears all policy states.
     * @return true if the policy has been disactivated successfully, false otherwise.
     */
    public BooleanWrapper disactivate() {
        if (rmShuttingDown) {
            // do not try to unregister monitor in this case
            return new BooleanWrapper(true);
        }

        try {
            rmMonitoring.removeRMEventListener();
        } catch (RMException e) {
            e.printStackTrace();
            return new BooleanWrapper(false);
        }
        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#rmEvent(org.ow2.proactive.resourcemanager.common.event.RMEvent)
     */
    public void rmEvent(RMEvent event) {
        switch (event.getEventType()) {
            case SHUTTING_DOWN:
                rmShuttingDown = true;
                break;
        }
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeSourceEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent)
     */
    public void nodeSourceEvent(RMNodeSourceEvent event) {
    }

    /**
     * @see org.ow2.proactive.resourcemanager.frontend.RMEventListener#nodeEvent(org.ow2.proactive.resourcemanager.common.event.RMNodeEvent)
     */
    public void nodeEvent(RMNodeEvent event) {
    }
}
