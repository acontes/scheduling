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
package org.objectweb.proactive.extra.scheduler.job;

import java.io.Serializable;

import org.objectweb.proactive.extra.scheduler.common.job.JobId;


/**
 * This class represented an authentificate job.
 * It is what the scheduler should be able to managed.
 *
 * @author ProActive Team
 * @version 1.0, Jul 4, 2007
 * @since ProActive 3.2
 */
public class IdentifyJob implements Serializable {

    /** serial version UID */
    private static final long serialVersionUID = 9100796464303741891L;

    /** Job Identification */
    private JobId jobId;

    /** User identification */
    private UserIdentification userIdentification;

    /** is this job finished */
    private boolean finished = false;

    /**
     * Identify job constructor with a given job and Identification.
     *
     * @param jobId a job identification.
     * @param userIdentification a user identification that should be able to identify the job user.
     */
    public IdentifyJob(JobId jobId, UserIdentification userIdentification) {
        this.jobId = jobId;
        this.userIdentification = userIdentification;
    }

    /**
     * To get the jobId
     *
     * @return the jobId
     */
    public JobId getJobId() {
        return jobId;
    }

    /**
     * To get the userIdentification
     *
     * @return the userIdentification
     */
    public UserIdentification getUserIdentification() {
        return userIdentification;
    }

    /**
     * Check if the given user identification can managed this job.
     *
     * @param userId the user identification to check.
     * @return true if userId has permission to managed this job.
     */
    public boolean hasRight(UserIdentification userId) {
        if (userIdentification == null) {
            return false;
        }
        return userId.isAdmin() || userIdentification.equals(userId);
    }

    /**
     * Return true if the job isFinished, false otherwise.
     *
     * @return the finished state of the job.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Set the finish state of the job.
     *
     * @param finished the finish state to set.
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return jobId.hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IdentifyJob) {
            return jobId.equals(((IdentifyJob) obj).jobId);
        }
        return false;
    }
}
