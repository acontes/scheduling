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

import java.util.HashMap;
import java.util.Map;


/**
 * Definition of a java task for the user.
 * A java task includes an executable task that can be set as
 * a class or instance.
 * It also provides a method to add arguments to the task.
 *
 * @author jlscheef - ProActiveTeam
 * @version 1.0, Sept 14, 2007
 * @since ProActive 3.2
 */
public class JavaTask extends Task {

    /** Serial version UID */
    private static final long serialVersionUID = -2327189450547547292L;

    /** Task as an instance */
    private JavaExecutable taskInstance = null;

    /** or as a class */
    private Class<JavaExecutable> taskClass = null;

    /** Arguments of the task as a map */
    private Map<String, Object> args = new HashMap<String, Object>();

    /**
     * Empty constructor.
     */
    public JavaTask() {
    }

    /**
     * To get the executable task as a class.
     *
     * @return the task Class.
     */
    public Class<JavaExecutable> getTaskClass() {
        return taskClass;
    }

    /**
     * To set the executable task class.
     * It may be a class that extends {@link JavaExecutable}.
     *
     * @param taskClass the task Class to set.
     */
    public void setTaskClass(Class<JavaExecutable> taskClass) {
        this.taskClass = taskClass;
        this.taskInstance = null;
    }

    /**
     * To get the executable task as an instance.
     *
     * @return the task Instance.
     */
    public JavaExecutable getTaskInstance() {
        return taskInstance;
    }

    /**
     * To set the executable task instance.
     * It may be an instance that extends {@link JavaExecutable}.
     *
     * @param taskInstance the task Instance to set.
     */
    public void setTaskInstance(JavaExecutable taskInstance) {
        this.taskInstance = taskInstance;
        this.taskClass = null;
    }

    /**
     * Return the task arguments list as an hash map.
     *
     * @return the arguments list.
     */
    public Map<String, Object> getArguments() {
        return args;
    }

    /**
     * Add an argument to the list of arguments.
     *
     * @param name the name of the argument to add.
     * @param value the associated value to add.
     */
    public void addArgument(String name, String value) {
        args.put(name, value);
    }
}
