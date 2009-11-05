/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of 
 * 						   Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
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
 * If needed, contact us to obtain a release under GPL Version 2. 
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
import org.ow2.proactive.scheduler.common.SchedulerConstants;
import org.ow2.proactive.scheduler.common.job.JobId;


/**
 * This class contains all informations about the state of the task.
 * It also provides methods and static fields to order the tasks if you hold them in a list.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@PublicAPI
public abstract class TaskState extends Task implements Comparable<TaskState> {

    /** Sorting constant, this will allow the user to sort the descriptor. */
    public static final int SORT_BY_ID = 1;
    public static final int SORT_BY_NAME = 2;
    public static final int SORT_BY_STATUS = 3;
    public static final int SORT_BY_DESCRIPTION = 4;
    public static final int SORT_BY_EXECUTIONLEFT = 5;
    public static final int SORT_BY_EXECUTIONONFAILURELEFT = 6;
    public static final int SORT_BY_STARTED_TIME = 8;
    public static final int SORT_BY_FINISHED_TIME = 9;
    public static final int SORT_BY_HOST_NAME = 10;
    public static final int ASC_ORDER = 1;
    public static final int DESC_ORDER = 2;
    protected static int currentSort = SORT_BY_ID;
    protected static int currentOrder = ASC_ORDER;

    /** ProActive default constructor */
    public TaskState() {
    }

    /**
     * To update this taskState using a taskInfo
     *
     * @param taskInfo the taskInfo to set
     */
    public abstract void update(TaskInfo taskInfo);

    /**
     * Set the field to sort on.
     *
     * @param sortBy the field on which the sort will be made.
     */
    public static void setSortingBy(int sortBy) {
        currentSort = sortBy;
    }

    /**
     * Set the order for the next sort.
     *
     * @param order ASC_ORDER or DESC_ORDER
     */
    public static void setSortingOrder(int order) {
        if ((order == ASC_ORDER) || (order == DESC_ORDER)) {
            currentOrder = order;
        } else {
            currentOrder = ASC_ORDER;
        }
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(TaskState task) {
        switch (currentSort) {
            case SORT_BY_DESCRIPTION:
                return (currentOrder == ASC_ORDER) ? (description.compareTo(task.description))
                        : (task.description.compareTo(description));
            case SORT_BY_NAME:
                return (currentOrder == ASC_ORDER) ? (name.compareTo(task.name))
                        : (task.name.compareTo(name));
            case SORT_BY_STATUS:
                return (currentOrder == ASC_ORDER) ? (getStatus().compareTo(task.getStatus())) : (task
                        .getStatus().compareTo(getStatus()));
            case SORT_BY_STARTED_TIME:
                return (currentOrder == ASC_ORDER) ? ((int) (getStartTime() - task.getStartTime()))
                        : ((int) (task.getStartTime() - getStartTime()));
            case SORT_BY_FINISHED_TIME:
                return (currentOrder == ASC_ORDER) ? ((int) (getFinishedTime() - task.getFinishedTime()))
                        : ((int) (task.getFinishedTime() - getFinishedTime()));
            case SORT_BY_EXECUTIONLEFT:
                return (currentOrder == ASC_ORDER) ? (Integer.valueOf(getNumberOfExecutionLeft())
                        .compareTo(Integer.valueOf(task.getNumberOfExecutionLeft()))) : (Integer.valueOf(task
                        .getNumberOfExecutionLeft()).compareTo(Integer.valueOf(getNumberOfExecutionLeft())));
            case SORT_BY_EXECUTIONONFAILURELEFT:
                return (currentOrder == ASC_ORDER) ? (Integer.valueOf(getNumberOfExecutionOnFailureLeft())
                        .compareTo(Integer.valueOf(task.getNumberOfExecutionOnFailureLeft()))) : (Integer
                        .valueOf(task.getNumberOfExecutionOnFailureLeft()).compareTo(Integer
                        .valueOf(getNumberOfExecutionOnFailureLeft())));
            case SORT_BY_HOST_NAME:
                return (currentOrder == ASC_ORDER) ? (getExecutionHostName().compareTo(task
                        .getExecutionHostName())) : (task.getExecutionHostName()
                        .compareTo(getExecutionHostName()));
            default:
                return (currentOrder == ASC_ORDER) ? (getId().compareTo(task.getId())) : (task.getId()
                        .compareTo(getId()));
        }
    }

    /**
     * To get the taskInfo
     *
     * @return the taskInfo
     */
    public abstract TaskInfo getTaskInfo();

    /**
     * To get the finishedTime
     *
     * @return the finishedTime
     */
    public long getFinishedTime() {
        return getTaskInfo().getFinishedTime();
    }

    /**
     * To get the jobID
     *
     * @return the jobID
     */
    public JobId getJobId() {
        return getTaskInfo().getJobId();
    }

    /**
     * To get the startTime
     *
     * @return the startTime
     */
    public long getStartTime() {
        return getTaskInfo().getStartTime();
    }

    /**
     * To get the taskId
     *
     * @return the taskID
     */
    public TaskId getId() {
        return getTaskInfo().getTaskId();
    }

    /**
     * To get the status of this job
     *
     * @return the status of this job
     */
    public TaskStatus getStatus() {
        return getTaskInfo().getStatus();
    }

    /**
     * Get the last execution HostName of the task.
     *
     * @return the last execution HostName.
     */
    public String getExecutionHostName() {
        return getTaskInfo().getExecutionHostName();
    }

    /**
     * To get the list of execution hosts name.
     * The first element of the returned array is the most recent used host.
     *
     * @return the execution Host Name list.
     */
    public String[] getExecutionHostNameList() {
        return getTaskInfo().getExecutionHostNameList();
    }

    /**
     * Get the number of execution left.
     *
     * @return the number of execution left.
     */
    public int getNumberOfExecutionLeft() {
        return getTaskInfo().getNumberOfExecutionLeft();
    }

    /**
     * Get the numberOfExecutionOnFailureLeft value.
     *
     * @return the numberOfExecutionOnFailureLeft value.
     */
    public int getNumberOfExecutionOnFailureLeft() {
        return getTaskInfo().getNumberOfExecutionOnFailureLeft();
    }

    /**
     * Get the number of execution on failure allowed by the task.
     *
     * @return the number of execution on failure allowed by the task
     */
    public abstract int getMaxNumberOfExecutionOnFailure();

    /**
     * @see org.ow2.proactive.scheduler.common.task.Task#getName()
     */
    @Override
    public String getName() {
        if (getId() == null || getId().getReadableName().equals(SchedulerConstants.TASK_DEFAULT_NAME)) {
            return super.getName();
        } else {
            return getId().getReadableName();
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getId() + ")";
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (TaskState.class.isAssignableFrom(obj.getClass())) {
            return ((TaskState) obj).getId().equals(getId());
        }

        return false;
    }

}
