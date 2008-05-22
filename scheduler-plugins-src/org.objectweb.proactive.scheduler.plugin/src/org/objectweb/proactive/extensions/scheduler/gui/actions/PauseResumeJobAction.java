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
package org.objectweb.proactive.extensions.scheduler.gui.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.TableItem;
import org.objectweb.proactive.extensions.scheduler.common.job.JobId;
import org.objectweb.proactive.extensions.scheduler.common.job.JobState;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.SchedulerState;
import org.objectweb.proactive.extensions.scheduler.gui.data.JobsController;
import org.objectweb.proactive.extensions.scheduler.gui.data.SchedulerProxy;
import org.objectweb.proactive.extensions.scheduler.gui.data.TableManager;


/**
 * @author The ProActive Team
 */
public class PauseResumeJobAction extends SchedulerGUIAction {

    public PauseResumeJobAction() {
        setPauseResumeMode();
        this.setEnabled(false);
    }

    @Override
    public void run() {
        TableItem item = TableManager.getInstance().getLastSelectedItem();
        if (item != null) {
            JobId jobId = (JobId) item.getData();
            JobState jobState = JobsController.getLocalView().getJobById(jobId).getState();
            if (jobState.equals(JobState.PAUSED)) {
                SchedulerProxy.getInstance().resume(jobId);
                setPauseMode();
            } else if (jobState.equals(JobState.RUNNING) || jobState.equals(JobState.PENDING) ||
                jobState.equals(JobState.STALLED)) {
                SchedulerProxy.getInstance().pause(jobId);
                setResumeMode();
            } else {
                setPauseResumeMode();
            }
        }
    }

    public void setPauseMode() {
        this.setText("Pause job");
        this.setToolTipText("To pause this job (this will finish all running tasks)");
    }

    public void setResumeMode() {
        this.setText("Resume job");
        this.setToolTipText("To resume this job (this will restart all paused tasks)");
    }

    public void setPauseResumeMode() {
        this.setEnabled(false);
        this.setText("Pause/Resume job");
        this.setToolTipText("To pause or resume a job");
        this
                .setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(),
                        "icons/job_pause_resume.gif"));
    }

    @Override
    public void setEnabled(boolean connected, SchedulerState schedulerState, boolean admin,
            boolean jobSelected, boolean owner, boolean jobInFinishQueue) {
        if (connected && jobSelected && !jobInFinishQueue && (admin || owner))
            setEnabled(true);
        else
            setEnabled(false);
    }
}
