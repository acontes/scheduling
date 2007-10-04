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
package org.objectweb.proactive.extra.scheduler.job;

import java.io.Serializable;


/**
 * This class will be able to authenticate a user/admin.
 * Two userIdentification are equals if there username are the same.
 *
 * @author ProActive Team
 * @version 1.0, Jul 4, 2007
 * @since ProActive 3.2
 */
public class UserIdentification implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -1382908192253223167L;

    /** user name */
    private String username;

    /** is this user an admin */
    private boolean admin = false;

    /**
     * Constructor of user identification using user name.
     *
     * @param username the user name.
     */
    public UserIdentification(String username) {
        this.username = username;
    }

    /**
     * Constructor of user identification using user name and admin property.
     *
     * @param username the user name.
     * @param admin true if the user is an administrator, false if not.
     */
    public UserIdentification(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
    }

    /**
     * To get the username
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * To know if this user is an administrator or a user.
     *
     * @return true if this user is admin, false if not.
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     *
     * @return true if the username of this and obj are equals.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UserIdentification) {
            return username.equals(((UserIdentification) obj).username);
        }
        return false;
    }
}
