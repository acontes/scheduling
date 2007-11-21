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
package org.objectweb.proactive.extra.scheduler.task;

import org.objectweb.proactive.Body;
import org.objectweb.proactive.api.ProActiveObject;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.extra.infrastructuremanager.frontend.NodeSet;
import org.objectweb.proactive.extra.scheduler.common.scripting.Script;
import org.objectweb.proactive.extra.scheduler.common.task.Executable;
import org.objectweb.proactive.extra.scheduler.common.task.Log4JTaskLogs;
import org.objectweb.proactive.extra.scheduler.common.task.ProActiveExecutable;
import org.objectweb.proactive.extra.scheduler.common.task.TaskId;
import org.objectweb.proactive.extra.scheduler.common.task.TaskResult;
import org.objectweb.proactive.extra.scheduler.core.SchedulerCore;


/**
 * ProActive task Launcher will be able to launch an application task
 *
 * @author jlscheef - ProActiveTeam
 * @version 1.0, Jul 10, 2007
 * @since ProActive 3.2
 */
public class ProActiveTaskLauncher extends TaskLauncher {

    /** Serial version UID */
    private static final long serialVersionUID = 4655938634771399458L;

    /** execution nodes list */
    private NodeSet nodesList;

    /**
     * ProActive empty constructor.
     */
    public ProActiveTaskLauncher() {
    }

    /**
     * Create a new instance of ProActiveTaskLauncher
     * CONSTRUCTOR USED BY THE SCHEDULER CORE : plz do not remove.
     *
     * @param taskId the identification of the task to launch.
     * @param host the host name on which the task is launched.
     * @param port the port number on which the task is launched.
     */
    public ProActiveTaskLauncher(TaskId taskId, String host, Integer port) {
        super(taskId, host, port);
    }

    /**
     * Create a new instance of ProActiveTaskLauncher
     * CONSTRUCTOR USED BY THE SCHEDULER CORE : plz do not remove.
     *
     * @param taskId the identification of the task to launch.
     * @param host the host name on which the task is launched.
     * @param port the port number on which the task is launched.
     * @param pre the pre-script executed before the task is launched.
     */
    public ProActiveTaskLauncher(TaskId taskId, String host, Integer port,
        Script<?> pre) {
        super(taskId, host, port, pre);
    }

    /**
     * @see org.objectweb.proactive.extra.scheduler.task.TaskLauncher#initActivity(org.objectweb.proactive.Body)
     */
    @Override
    public void initActivity(Body body) {
        super.initActivity(body);
        ProActiveObject.setImmediateService("getNodes");
    }

    /**
     * This method should have NEVER been called in an ProActive task launcher.
     */
    @Override
    public TaskResult doTask(SchedulerCore core, Executable executable,
        TaskResult... results) {
        throw new RuntimeException(
            "This method should have NEVER been called in this context !!");
    }

    /**
     * Execute the user ProActive task as an active object.
     * This will provide the user the number of asked nodes.
     *
     * @param core The scheduler core to be notify
     * @param task the ProActive task to execute.
     * @param nodes the nodes that the user needs to run his ProActive task.
     * @return a task result representing the result of this task execution.
     */
    @SuppressWarnings("unchecked")
    public TaskResult doTask(SchedulerCore core,
        ProActiveExecutable executableTask, NodeSet nodes) {
        nodesList = nodes;

        try {
            nodesList.add(super.getNodes().get(0));
        } catch (NodeException e) {
        }

        this.initLoggers();

        try {
            //launch pre script
            if (pre != null) {
                for (Node node : nodes) {
                    this.executePreScript(node);
                }
            }

            //init task
            executableTask.init();

            //launch task
            TaskResult result = new TaskResultImpl(taskId,
                    executableTask.execute(nodes),
                    new Log4JTaskLogs(this.logBuffer.getBuffer()));

            //return result
            return result;
        } catch (Throwable ex) {
            // exceptions are always handled at scheduler core level
            return new TaskResultImpl(taskId, ex,
                new Log4JTaskLogs(this.logBuffer.getBuffer()));
        } finally {
            // reset stdout/err
            try {
                this.finalizeLoggers();
            } catch (RuntimeException e) {
                // exception should not be thrown to the scheduler core
                // the result has been computed and must be returned !
                // TODO : logger.warn
                System.err.println("WARNING : Loggers are not shut down !");
            }

            //terminate the task
            core.terminate(taskId);
        }
    }

    /**
     * Return the nodes list.
     *
     * @return the nodesList.
     */
    @Override
    public NodeSet getNodes() {
        return nodesList;
    }
}
