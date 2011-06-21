/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $ACTIVEEON_INITIAL_DEV$
 */
package org.ow2.proactive.scheduler.exception;

/**
 * IllegalProgressException is thrown when user has overridden the getProgress() method and it throws an exception.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 3.1
 */
public class IllegalProgressException extends ProgressPingerException {

    /**
     * Create a new instance of IllegalProgressException
     *
     * @param msg the message that causes the exception
     */
    public IllegalProgressException(String msg) {
        super(msg);
    }

    /**
     * Create a new instance of IllegalProgressException
     *
     * @param msg the message that causes the exception
     * @param cause the optionnal cause of the exception
     */
    public IllegalProgressException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
