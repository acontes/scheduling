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

import org.objectweb.proactive.extra.scheduler.common.job.JobId;
import org.objectweb.proactive.extra.scheduler.common.scripting.Script;
import org.objectweb.proactive.extra.scheduler.common.task.ExecutableTask;
import org.objectweb.proactive.extra.scheduler.common.task.TaskId;
import org.objectweb.proactive.extra.scheduler.common.task.TaskResult;
import org.objectweb.proactive.extra.scheduler.core.SchedulerCore;


/**
 * Native Task Launcher.
 * This launcher is the class that will launch a native class.
 *
 * @author ProActive Team
 * @version 1.0, Jul 10, 2007
 * @since ProActive 3.2
 */
public class NativeTaskLauncher extends TaskLauncher {

    /** Serial version UID */
    private static final long serialVersionUID = 8574369410634220047L;
    private Process process;

    /**
     * ProActive Empty Constructor
     */
    public NativeTaskLauncher() {
    }

    /**
     * Constructor of the native task launcher.
     *
     * @param taskId the task identification.
     * @param jobId the job identification.
     * @param host the host on which the task is launched.
     * @param port the port on which the task is launched.
     */
    public NativeTaskLauncher(TaskId taskId, JobId jobId, String host,
        Integer port) {
        super(taskId, jobId, host, port);
    }

    /**
     * Constructor with native task identification
     *
     * @param taskId represents the task the launcher will execute.
     * @param jobId represents the job where the task is located.
     */
    public NativeTaskLauncher(TaskId taskId, JobId jobId, Script<?> pre,
        String host, Integer port) {
        super(taskId, jobId, pre, host, port);
    }

    /**
     * Execute the user task as an active object.
     *
     * @param core The scheduler core to be notify
     * @param executableTask the task to execute
     * @param results the possible results from parent tasks.(if task flow)
     * @return a task result representing the result of this task execution.
     */
    @Override
    @SuppressWarnings("unchecked")
    public TaskResult doTask(SchedulerCore core, ExecutableTask executableTask,
        TaskResult... results) {
        this.initLoggers();
        try {
            //launch pre script
            if (pre != null) {
                this.executePreScript(null);
            }
            //get process
            process = ((ExecutableNativeTask) executableTask).getProcess();
            //launch task
            TaskResult result = new TaskResultImpl(taskId,
                    executableTask.execute(results));

            //return result
            return result;
        } catch (Throwable ex) {
            // exceptions are always handled at scheduler core level
            return new TaskResultImpl(taskId, ex);
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
            core.terminate(taskId, jobId);
        }
    }

    /**
     * Kill all launched nodes/tasks and terminate the launcher.
     *
     * @see org.objectweb.proactive.extra.scheduler.task.TaskLauncher#terminate()
     */
    @Override
    public void terminate() {
        process.destroy();
        super.terminate();
    }
}
