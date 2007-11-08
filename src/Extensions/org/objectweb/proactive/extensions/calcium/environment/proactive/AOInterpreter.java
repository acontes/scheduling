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

import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.ProActiveObject;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;


public class AOInterpreter {
    AOStageIn stageIn;
    AOStageOut stageOut;
    AOStageCompute stageCompute;

    /**
     * Empty constructor for ProActive  MOP
     * Do not use directly!!!
     */
    @Deprecated
    public AOInterpreter() {
    }

    public AOInterpreter(AOTaskPool taskpool, FileServerClientImpl fserver)
        throws NodeException, ActiveObjectCreationException {
        Node localnode = NodeFactory.getDefaultNode();

        this.stageOut = (AOStageOut) ProActiveObject.newActive(AOStageOut.class.getName(),
                new Object[] { taskpool, fserver }, localnode);

        this.stageCompute = (AOStageCompute) ProActiveObject.newActive(AOStageCompute.class.getName(),
                new Object[] { taskpool, stageOut }, localnode);

        this.stageIn = (AOStageIn) ProActiveObject.newActive(AOStageIn.class.getName(),
                new Object[] { taskpool, fserver, stageCompute }, localnode);
    }

    public AOStageIn getStageIn(AOInterpreterPool interpool) {
        stageOut.setStageInAndInterPool(stageIn, interpool);

        return stageIn;
    }
}
