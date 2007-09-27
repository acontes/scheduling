/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed, Concurrent
 * computing with Security and Mobility
 *
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis Contact:
 * proactive-support@inria.fr
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Initial developer(s): The ProActive Team
 * http://www.inria.fr/oasis/ProActive/contacts.html Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extensions.calcium.environment.proactive;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.ProFuture;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.calcium.environment.FileServer;
import org.objectweb.proactive.extensions.calcium.task.Task;


public class TaskDispatcher extends Thread {
    static Logger logger = ProActiveLogger.getLogger(Loggers.SKELETONS_ENVIRONMENT);
    private ActiveInterpreterPool intpool;
    private ActiveTaskPool taskpool;
    private boolean shutdown;

    public TaskDispatcher(ActiveTaskPool taskpool, FileServer fserver,
        Node[] nodes)
        throws NodeException, ActiveObjectCreationException,
            ClassNotFoundException {
        super();
        shutdown = false;

        Node localnode = NodeFactory.getDefaultNode();

        // Create Active Objects
        this.taskpool = taskpool;
        intpool = Util.createActiveInterpreterPool(localnode);
        AOInterpreter[] aoi = Util.createAOinterpreter(nodes);

        //Instantiate Active Objects
        for (AOInterpreter i : aoi) {
            i.init(i, taskpool, fserver, intpool);
        }

        intpool.init(aoi);
    }

    public void run() {
        shutdown = false;

        while (!shutdown) {
            Task task = taskpool.getReadyTask(0);
            task = (Task) ProFuture.getFutureValue(task);

            if (task != null) {
                //block until there is an available interpreter
                AOInterpreter interpreter = intpool.getAOInterpreter();
                interpreter.interpret(task); //remote async call
            }
        }
    }

    public void shutdown() {
        shutdown = true;
    }
}
