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
package org.objectweb.proactive.extra.scheduler.common.scheduler;

import java.io.IOException;
import java.io.Serializable;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.ProActiveObject;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extra.scheduler.common.exception.SchedulerException;


/**
 * Scheduler connection class provide method to join an existing scheduler.
 * The method {@link #join(String)} returns a {@link SchedulerAuthenticationInterface} in order to permit the scheduler
 * to authenticate user that want to connect a scheduler.
 *
 * @author jlscheef - ProActiveTeam
 * @version 1.0, Jul 24, 2007
 * @since ProActive 3.2
 */
public class SchedulerConnection implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = 278178831821342953L;

    /** default scheduler node name */
    public static final String SCHEDULER_DEFAULT_NAME = "SCHEDULER";

    /** Scheduler logger */
    private static Logger logger = ProActiveLogger.getLogger(Loggers.SCHEDULER);

    /**
     * Return the scheduler authentication at the specified URL.
     *
     * @param schedulerURL the URL of the scheduler to join.
     * @return the scheduler authentication at the specified URL.
     * @throws SchedulerException thrown if the connection to the scheduler cannot be established.
     */
    public static SchedulerAuthenticationInterface join(String schedulerURL)
        throws SchedulerException {
        // Get the scheduler authentication at the specified URL
        SchedulerAuthenticationInterface schedulerAuth = null;
        logger.info(
            "******************* TRYING TO JOIN EXISTING SCHEDULER *****************");

        if (schedulerURL == null) {
            logger.info(
                "Scheduler URL was null, looking for scheduler on localhost with the default scheduler name...");
            schedulerURL = "//localhost/" + SCHEDULER_DEFAULT_NAME;
        }

        try {
            schedulerAuth = (SchedulerAuthenticationInterface) (ProActiveObject.lookupActive(SchedulerAuthenticationInterface.class.getName(),
                    schedulerURL));

            return schedulerAuth;
        } catch (ActiveObjectCreationException e) {
            throw new SchedulerException("Error while getting scheduler interface !",
                e);
        } catch (IOException e) {
            throw new SchedulerException("Error while connecting the scheduler !",
                e);
        }
    }
}
