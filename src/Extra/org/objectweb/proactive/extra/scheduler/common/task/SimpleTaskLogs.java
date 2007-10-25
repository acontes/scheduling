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


/**
 * A simple String based implementation of TaskLogs.
 * @author cdelbe
 * @since 2.2
 */
public class SimpleTaskLogs implements TaskLogs {
    // logs on standard output
    private final String standardLogs;

    // logs on error output
    private final String errorlogs;

    /**
     * Create a new SimpleTaskLogs.
     * @param stdLogs the standard output.
     * @param errLogs the error output.
     */
    public SimpleTaskLogs(String stdLogs, String errLogs) {
        this.standardLogs = stdLogs;
        this.errorlogs = errLogs;
    }

    /**
     * Timestamp parameter is not relevant for this TaskLogs implementation.
     * @see org.objectweb.proactive.extra.scheduler.common.task.TaskLogs#getAllLogs(boolean)
     */
    @Override
    public String getAllLogs(boolean timeStamp) {
        return this.standardLogs + this.errorlogs;
    }

    /**
     * Timestamp parameter is not relevant for this TaskLogs implementation.
     * @see org.objectweb.proactive.extra.scheduler.common.task.TaskLogs#getStderrLogs(boolean)
     */
    @Override
    public String getStderrLogs(boolean timeStamp) {
        return this.errorlogs;
    }

    /**
     * Timestamp parameter is not relevant for this TaskLogs implementation.
     * @see org.objectweb.proactive.extra.scheduler.common.task.TaskLogs#getStdoutLogs(boolean)
     */
    @Override
    public String getStdoutLogs(boolean timeStamp) {
        return this.standardLogs;
    }
}
