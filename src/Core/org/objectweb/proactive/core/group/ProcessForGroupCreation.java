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
package org.objectweb.proactive.core.group;

import org.objectweb.proactive.api.ProActiveObject;
import org.objectweb.proactive.core.node.Node;


/**
 * This class provides multithreading for the creation of active objects.
 *
 * @author Laurent Baduel
 */
public class ProcessForGroupCreation extends AbstractProcessForGroup
    implements Runnable {
    private ProxyForGroup proxyGroup;
    private String className;
    private Class[] genericParameters;
    private Object[] param;
    private Node node;
    private int index;

    public ProcessForGroupCreation(ProxyForGroup proxyGroup, String className,
        Class[] genericParameters, Object[] param, Node node, int index) {
        this.proxyGroup = proxyGroup;
        this.className = className;
        this.genericParameters = genericParameters;
        this.param = param;
        this.node = node;
        this.index = index;
    }

    public void run() {
        try {
            this.proxyGroup.set(this.index,
                ProActiveObject.newActive(className, genericParameters, param,
                    node));
            //			this.proxyGroup.decrementWaitedAndNotifyAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMemberListSize() {
        return 1;
    }
}
