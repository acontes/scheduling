/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2008 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.util.process;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Case-insensitive string comparator.
 *
 * @author Kohsuke Kawaguchi
 */
public final class CaseInsensitiveComparator implements Comparator<String>, Serializable {
    /**  */
    public static final Comparator<String> INSTANCE = new CaseInsensitiveComparator();

    private CaseInsensitiveComparator() {
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     * @param lhs first string.
     * @param rhs second string.
     * @return a negative integer, zero, or a positive integer as the
     * 	       first argument is less than, equal to, or greater than the
     *	       second.  
     */
    public int compare(String lhs, String rhs) {
        return lhs.compareToIgnoreCase(rhs);
    }
}
