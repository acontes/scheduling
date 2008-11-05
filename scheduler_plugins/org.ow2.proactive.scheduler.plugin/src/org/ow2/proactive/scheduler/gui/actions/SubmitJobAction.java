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
package org.ow2.proactive.scheduler.gui.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobFactory;
import org.ow2.proactive.scheduler.common.scheduler.SchedulerState;
import org.ow2.proactive.scheduler.gui.data.SchedulerProxy;


/**
 * @author The ProActive Team
 */
public class SubmitJobAction extends SchedulerGUIAction {
    private Composite parent = null;

    public SubmitJobAction(Composite parent) {
        this.parent = parent;
        this.setText("Submit an XML job file");
        this.setToolTipText("Submit job from an XML file containing a job description");
        this.setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(), "icons/job_submit.gif"));
        this.setEnabled(false);
    }

    @Override
    public void run() {
        FileDialog fileDialog = new FileDialog(parent.getShell(), SWT.OPEN);
        fileDialog.setFilterExtensions(new String[] { "*.xml" });
        String fileName = fileDialog.open();

        if (fileName != null) {
            try {
                // CREATE JOB
                Job job = JobFactory.getFactory().createJob(fileName);
                // SUBMIT JOB
                SchedulerProxy.getInstance().submit(job);
            } catch (JobCreationException e) {
                MessageDialog.openError(parent.getShell(), "Couldn't submit job",
                        "Couldn't submit job due to : " + e.getCause());
            } catch (SchedulerException e) {
                MessageDialog.openError(parent.getShell(), "Couldn't submit job",
                        "Couldn't submit job due to : " + e.getCause());
            }
        }
    }

    @Override
    public void setEnabled(boolean connected, SchedulerState schedulerState, boolean admin,
            boolean jobSelected, boolean owner, boolean jobInFinishQueue) {
        if (connected && (schedulerState != SchedulerState.KILLED) &&
            (schedulerState != SchedulerState.SHUTTING_DOWN) && (schedulerState != SchedulerState.STOPPED))
            setEnabled(true);
        else
            setEnabled(false);
    }
}
