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
 *  Initial developer(s):               The ActiveEon Team
 *                        http://www.activeeon.com/
 *  Contributor(s):
 *
 * ################################################################
 * $ACTIVEEON_INITIAL_DEV$
 */
package org.ow2.proactive.tests.performance.scheduler;

import java.util.HashSet;
import java.util.Set;

import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.tests.performance.utils.AbstractEventsMonitor;


public class SchedulerEventsMonitor extends AbstractEventsMonitor<SchedulerWaitCondition> implements
        SchedulerEventListener {

    public static Set<JobStatus> completedJobStatus;

    static {
        completedJobStatus = new HashSet<JobStatus>();
        completedJobStatus.add(JobStatus.CANCELED);
        completedJobStatus.add(JobStatus.FAILED);
        completedJobStatus.add(JobStatus.FINISHED);
        completedJobStatus.add(JobStatus.KILLED);
    }

    public void schedulerStateUpdatedEvent(SchedulerEvent eventType) {
        synchronized (waitConditions) {
            for (SchedulerWaitCondition waitCondition : waitConditions) {
                waitCondition.schedulerStateUpdatedEvent(eventType);
            }
        }
    }

    public void jobSubmittedEvent(JobState job) {
        synchronized (waitConditions) {
            for (SchedulerWaitCondition waitCondition : waitConditions) {
                waitCondition.jobSubmittedEvent(job);
            }
        }
    }

    public void jobStateUpdatedEvent(NotificationData<JobInfo> notification) {
        synchronized (waitConditions) {
            for (SchedulerWaitCondition waitCondition : waitConditions) {
                waitCondition.jobStateUpdatedEvent(notification);
            }
        }
    }

    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        synchronized (waitConditions) {
            for (SchedulerWaitCondition waitCondition : waitConditions) {
                waitCondition.taskStateUpdatedEvent(notification);
            }
        }
    }

    public void usersUpdatedEvent(NotificationData<UserIdentification> notification) {
        synchronized (waitConditions) {
            for (SchedulerWaitCondition waitCondition : waitConditions) {
                waitCondition.usersUpdatedEvent(notification);
            }
        }
    }

}
