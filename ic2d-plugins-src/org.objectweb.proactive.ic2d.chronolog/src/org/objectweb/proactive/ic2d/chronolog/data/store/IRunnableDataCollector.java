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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 */
package org.objectweb.proactive.ic2d.chronolog.data.store;

/**
 * A separate <code>Thread</code> will run this collector and ask each model
 * element to provide a value that will be inserted in the underlying data
 * store.
 * 
 * @author <a href="mailto:support@activeeon.com">ActiveEon Team</a>.
 */
public interface IRunnableDataCollector extends Runnable {

    /**
     * Cancels this collector.
     */
    public void cancel();

    /**
     * The result is <code>true</code> if and only if the collector is running
     * i.e. is collecting some data.
     * 
     * @return <code>true</code> if the collector is running;
     *         <code>false</code> otherwise.
     */
    public boolean isRunning();

}
