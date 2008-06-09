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
package org.objectweb.proactive.extensions.masterworker.interfaces.internal;

import org.objectweb.proactive.extensions.masterworker.core.IsClearingException;

import java.io.Serializable;
import java.util.Queue;


/**
 * <i><font size="-1" color="#FF0000">**For internal use only** </font></i><br>
 * A Task Provider provides tasks to be executed and excepts results of these tasks (i.e. the Master from the worker point of view)
 * @author The ProActive Team
 *
 * @param <R> the type of the result client object
 */
public interface TaskProvider<R extends Serializable> {
    /**
     * Returns an array of tasks which need to be executed
     * @param worker the worker object which asks the tasks (stub)
     * @param workerName the name of the worker which asks the tasks
     * @return a list of new tasks to compute
     */
    Queue<TaskIntern<R>> getTasks(Worker worker, String workerName, boolean reflooding);

    /**
     * Returns the result of a task to the provider and ask for new ones
     * @param result the result of the completed task
     * @param workerName the name of the worker sending the result
     * @param reflooding that means the worker's stack is empty and it asks for a set a tasks bigger than one
     * @return a list of new tasks to compute
     */
    Queue<TaskIntern<R>> sendResultAndGetTasks(ResultIntern<R> result, String workerName, boolean reflooding);

    /**
     * Returns the result of a task to the provider
     * @param result the result of the completed task
     * @param workerName the name of the worker sending the result
     */
    boolean sendResult(ResultIntern<R> result, String workerName);

    /**
     * Callback function sent from the workers to the master to acknowledge
     * That their activity is cleared
     * @param worker worker sending the message
     */
    void isCleared(Worker worker);
}
