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
package org.objectweb.proactive.extra.scheduler.common.task;

import java.io.Serializable;

import org.objectweb.proactive.extra.scheduler.common.job.JobEvent;
import org.objectweb.proactive.extra.scheduler.common.job.JobId;


/**
 * Informations about the task that is able to change.
 * These informations are in an other class in order to permit
 * the scheduler listener to send this class as event.
 *
 * @author jlscheef - ProActiveTeam
 * @version 1.0, Jun 25, 2007
 * @since ProActive 3.2
 */
public class TaskEvent implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -7625483185225564284L;

    /** id of the task */
    private TaskId taskId = null;

    /** informations about the job */
    private JobEvent jobEvent = null;

    /** task submitted time */
    private long submitTime = -1;

    /** task started time */
    private long startTime = -1;

    /** task finished time : HAS TO BE SET TO -1 */
    private long finishedTime = -1;

    /** Number of reRunnable left */
    private int rerunnableLeft = 1;

    /** Current taskState of the task */
    private TaskState taskState = TaskState.SUBMITTED;

    /** name of the host where the task is executed */
    private String executionHostName;

    /**
     * To get the jobEvent
     *
     * @return the jobEvent
     */
    public JobEvent getJobEvent() {
        return jobEvent;
    }

    /**
     * To set the jobEvent
     *
     * @param jobEvent the jobEvent to set
     */
    public void setJobEvent(JobEvent jobEvent) {
        this.jobEvent = jobEvent;
    }

    /**
     * To get the finishedTime
     *
     * @return the finishedTime
     */
    public long getFinishedTime() {
        return finishedTime;
    }

    /**
     * To set the finishedTime
     *
     * @param finishedTime the finishedTime to set
     */
    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * To get the jobId
     *
     * @return the jobId
     */
    public JobId getJobId() {
        if (jobEvent != null) {
            return jobEvent.getJobId();
        }

        return null;
    }

    /**
     * To set the jobId
     *
     * @param jobId the jobId to set
     */
    public void setJobId(JobId jobId) {
        if (jobEvent != null) {
            jobEvent.setJobId(jobId);
        }
    }

    /**
     * To get the startTime
     *
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * To set the startTime
     *
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * To get the taskId
     *
     * @return the taskId
     */
    public TaskId getTaskId() {
        return taskId;
    }

    /**
     * To set the taskId
     *
     * @param taskID the taskId to set
     */
    public void setTaskId(TaskId taskId) {
        this.taskId = taskId;
    }

    /**
     * To get the submitTime
     *
     * @return the submitTime
     */
    public long getSubmitTime() {
        return submitTime;
    }

    /**
     * To set the submitTime
     *
     * @param submitTime the submitTime to set
     */
    public void setSubmitTime(long submitTime) {
        this.submitTime = submitTime;
    }

    /**
     * To get the taskState
     *
     * @return the taskState
     */
    public TaskState getStatus() {
        return taskState;
    }

    /**
     * To set the taskState
     *
     * @param taskState the taskState to set
     */
    public void setStatus(TaskState taskState) {
        this.taskState = taskState;
    }

    /**
     * To get the executionHostName
     *
     * @return the executionHostName
     */
    public String getExecutionHostName() {
        return executionHostName;
    }

    /**
     * To set the executionHostName
     *
     * @param executionHostName the executionHostName to set
     */
    public void setExecutionHostName(String executionHostName) {
        this.executionHostName = executionHostName;
    }

    /**
     * Get the number of rerun left.
     *
     * @return the rerunnableLeft
     */
    public int getRerunnableLeft() {
        return rerunnableLeft;
    }

    /**
     * Set the number of rerun left.
     *
     * @param rerunnableLeft the rerunnableLeft to set
     */
    public void setRerunnableLeft(int rerunnableLeft) {
        this.rerunnableLeft = rerunnableLeft;
    }
}
