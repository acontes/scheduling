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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.objectweb.proactive.extensions.scheduler.gui.data.SchedulerProxy;


/**
 * @author FRADJ Johann
 */
public class FreezeSchedulerAction extends Action {
    public static final boolean ENABLED_AT_CONSTRUCTION = false;
    private static FreezeSchedulerAction instance = null;

    private FreezeSchedulerAction() {
        this.setText("Freeze scheduler");
        this.setToolTipText("To freeze the scheduler (Only all running Tasks will be terminated)");
        this
                .setImageDescriptor(ImageDescriptor.createFromFile(this.getClass(),
                        "icons/scheduler_freeze.gif"));
        this.setEnabled(ENABLED_AT_CONSTRUCTION);
    }

    @Override
    public void run() {
        SchedulerProxy.getInstance().pauseImmediate();
    }

    public static FreezeSchedulerAction newInstance() {
        instance = new FreezeSchedulerAction();
        return instance;
    }

    public static FreezeSchedulerAction getInstance() {
        return instance;
    }
}
