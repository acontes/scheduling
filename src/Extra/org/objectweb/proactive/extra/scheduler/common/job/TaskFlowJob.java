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
package org.objectweb.proactive.extra.scheduler.common.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.proactive.extra.scheduler.common.exception.UserException;
import org.objectweb.proactive.extra.scheduler.common.task.Task;


/**
 * Definition of a task flow job for the user.
 * A task flow job or data flow job, is a job that can contain
 * one or more task with the dependencies you want.
 * To make this type of job, just use the default no params constructor,
 * and set the properties you want to set.
 * Then add tasks with the given method in order to fill the job with your own tasks.
 *
 * @author ProActive Team
 * @version 1.0, Sept 14, 2007
 * @since ProActive 3.2
 */
public class TaskFlowJob extends Job {

    /** Serial Version UID */
    private static final long serialVersionUID = 1623955669459590983L;

    /** List of task for the task flow job */
    private HashMap<String, Task> tasks = new HashMap<String, Task>();

    /** Proactive Empty Constructor */
    public TaskFlowJob() {
    }

    /**
     * @see org.objectweb.proactive.extra.scheduler.common.job.Job#getType()
     */
    @Override
    public JobType getType() {
        return JobType.TASKSFLOW;
    }

    /**
     * Add a task to this task flow job.
     *
     * @param task the task to add.
     */
    public void addTask(Task task) throws UserException {
        if (task.getName() == null) {
            throw new UserException("The name of the task must not be null !");
        }
        if (tasks.containsKey(task.getName())) {
            throw new UserException("The name of the task is already used !");
        }
        tasks.put(task.getName(), task);
    }

    /**
     * Add a list of tasks to this task flow job.
     *
     * @param tasks the list of tasks to add.
     */
    public void addTasks(List<Task> tasks) throws UserException {
        for (Task task : tasks) {
            addTask(task);
        }
    }

    /**
     * To get the list of tasks.
     *
     * @return the list of tasks.
     */
    public ArrayList<Task> getTasks() {
        return new ArrayList<Task>(tasks.values());
    }

    /**
         * @see org.objectweb.proactive.extra.scheduler.common.job.Job#getId()
         */
    @Override
    public JobId getId() {
        // Not yet assigned
        return null;
    }
}
