/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2008 INRIA/University of Nice-Sophia Antipolis
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
package org.ow2.proactive.scheduler.core;

import java.util.Vector;

import org.ow2.proactive.scheduler.common.SchedulerState;
import org.ow2.proactive.scheduler.common.SchedulerStatus;
import org.ow2.proactive.scheduler.common.SchedulerUsers;
import org.ow2.proactive.scheduler.common.job.JobState;


/**
 * This class is a representation of the whole scheduler initial jobs list state.<br>
 * It is basically represented by 3 lists of jobs, and its scheduling status.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
public final class SchedulerStateImpl implements SchedulerState {

    /**
     *
     */
    private static final long serialVersionUID = 10L;

    /** Pending jobs */
    private Vector<JobState> pendingJobs = new Vector<JobState>();

    /** Running jobs */
    private Vector<JobState> runningJobs = new Vector<JobState>();

    /** Finished jobs */
    private Vector<JobState> finishedJobs = new Vector<JobState>();

    /** Scheduler status */
    private SchedulerStatus status = SchedulerStatus.STOPPED;

    /** List of connected user. */
    private SchedulerUsers sUsers;

    /**
     * ProActive Empty constructor.
     */
    public SchedulerStateImpl() {
    }

    /**
     * To get the finishedJobs
     *
     * @return the finishedJobs
     */
    public Vector<JobState> getFinishedJobs() {
        return finishedJobs;
    }

    /**
     * To set the finishedJobs
     *
     * @param finishedJobs the finishedJobs to set
     */
    public void setFinishedJobs(Vector<JobState> finishedJobs) {
        this.finishedJobs = finishedJobs;
    }

    /**
     * To get the pendingJobs
     *
     * @return the pendingJobs
     */
    public Vector<JobState> getPendingJobs() {
        return pendingJobs;
    }

    /**
     * To set the pendingJobs
     *
     * @param pendingJobs the pendingJobs to set
     */
    public void setPendingJobs(Vector<JobState> pendingJobs) {
        this.pendingJobs = pendingJobs;
    }

    /**
     * To get the runningJobs
     *
     * @return the runningJobs
     */
    public Vector<JobState> getRunningJobs() {
        return runningJobs;
    }

    /**
     * To set the runningJobs
     *
     * @param runningJobs the runningJobs to set
     */
    public void setRunningJobs(Vector<JobState> runningJobs) {
        this.runningJobs = runningJobs;
    }

    /**
     * @return the status
     */
    public SchedulerStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setState(SchedulerStatus status) {
        this.status = status;
    }

    /**
     * Returns the list of connected users.
     *
     * @return the list of connected users.
     */
    public SchedulerUsers getUsers() {
        return sUsers;
    }

    /**
     * Sets the list of connected users to the given users value.
     *
     * @param users the list of connected users to set.
     */
    public void setUsers(SchedulerUsers users) {
        sUsers = users;
    }
}
