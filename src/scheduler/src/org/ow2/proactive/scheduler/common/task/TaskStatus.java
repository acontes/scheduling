/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.scheduler.common.task;

import org.objectweb.proactive.annotation.PublicAPI;


/**
 * This class represents every status that a task is able to be in.<br>
 * Each status are best describe below.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@PublicAPI
public enum TaskStatus implements java.io.Serializable {

    /**
     * The task has just been submitted by the user.
     */
    SUBMITTED("Submitted"),
    /**
     * The task is in the scheduler pending queue.
     */
    PENDING("Pending"),
    /**
     * The task is paused.
     */
    PAUSED("Paused"),
    /**
     * The task is executing.
     */
    RUNNING("Running"),
    /**
     * The task is waiting for restart after an error. (ie:native code != 0 or exception)
     */
    WAITING_ON_ERROR("Faulty..."),
    /**
     * The task is waiting for restart after a failure. (ie:node down)
     */
    WAITING_ON_FAILURE("Failed..."),
    /**
     * The task is failed
     * (only if max execution time has been reached and the node on which it was started is down).
     */
    FAILED("Resource down"),
    /**
     * The task could not be started.<br>
     * It means that the task could not be started due to
     * dependences failure.
     */
    NOT_STARTED("Could not start"),
    /**
     * The task could not be restarted.<br>
     * It means that the task could not be restarted after an error
     * during the previous execution
     */
    NOT_RESTARTED("Could not restart"),
    /**
     * The task has been aborted by an exception on an other task. (job is cancelOnError=true)
     * Can be also in this status if the job is killed while the concerned task was running.
     */
    ABORTED("Aborted"),
    /**
     * The task has finished execution with error code (!=0) or exception.
     */
    FAULTY("Faulty"),
    /**
     * The task has finished execution.
     */
    FINISHED("Finished"),
    /**
     * The task was not executed: it was the non-selected branch of an IF/ELSE control flow action
     */
    SKIPPED("Skipped");

    /** The name of the current status. */
    private String name;

    /**
     * Implicit constructor of a status.
     *
     * @param name the name of the status.
     */
    TaskStatus(String name) {
        this.name = name;
    }

    /**
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return name;
    }
}
