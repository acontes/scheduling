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
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extensions.calcium.environment.proactive;

import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.calcium.environment.FileServer;
import org.objectweb.proactive.extensions.calcium.environment.Interpreter;
import org.objectweb.proactive.extensions.calcium.task.Task;
import org.objectweb.proactive.extensions.calcium.task.TaskPool;


public class AOInterpreter {
    static Logger logger = ProActiveLogger.getLogger(Loggers.SKELETONS_ENVIRONMENT);
    private AOInterpreter me;
    private Interpreter interp;
    private TaskPool taskpool;
    private ActiveInterpreterPool intpool;
    private FileServer fserver;

    public AOInterpreter() {
    }

    public void init(AOInterpreter me, TaskPool taskpool, FileServer fserver,
        ActiveInterpreterPool intpool) {
        this.me = me;
        this.taskpool = taskpool;
        this.fserver = fserver;
        this.intpool = intpool;
        interp = new Interpreter();
    }

    public void interpret(Task task) {
        task = interp.interpret(fserver, task);
        intpool.registerAsAvailable(me);
        taskpool.putProcessedTask(task);
    }

    public String sayHello() {
        String localhost = "unkown";
        try {
            localhost = InetAddress.getLocalHost().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Hello from " + localhost;
    }
}
