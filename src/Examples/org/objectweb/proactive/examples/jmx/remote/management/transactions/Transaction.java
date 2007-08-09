/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2006 INRIA/University of Nice-Sophia Antipolis
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
package org.objectweb.proactive.examples.jmx.remote.management.transactions;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.NotificationBroadcasterSupport;

import org.objectweb.proactive.examples.jmx.remote.management.command.CommandMBean;
import org.objectweb.proactive.examples.jmx.remote.management.exceptions.TransactionNotActiveException;
import org.objectweb.proactive.examples.jmx.remote.management.status.Status;


public abstract class Transaction extends NotificationBroadcasterSupport {
    public static final long ACTIVE = 0;
    public static final long COMMITED = 1;
    public static final long DEAD = 2;
    protected long idTransaction;
    protected ConcurrentLinkedQueue<CommandMBean> commandQueue;
    protected long state;

    public abstract Status executeCommand(CommandMBean c);

    public abstract long getId();

    public abstract Status commit() throws TransactionNotActiveException;

    public abstract Status rollback() throws TransactionNotActiveException;

    public long getState() {
        return this.state;
    }

    public abstract Vector<CommandMBean> getCommands();

    public abstract void removeInCompensation(Vector<CommandMBean> commands);

    public abstract ArrayList<CommandMBean> getCompensation();

    public abstract void compensate();
}
