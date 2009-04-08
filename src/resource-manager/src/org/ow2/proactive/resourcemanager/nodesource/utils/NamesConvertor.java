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
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.resourcemanager.nodesource.utils;

/**
 * Converts names to user readable format.
 */
public class NamesConvertor {

    /**
     * Converts names to user readable format.
     * GCMClassName -> GCM Class Name
     */
    public static String beautifyName(String name) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0) {
                buffer.append(Character.toUpperCase(ch));
            } else if (i > 0 && Character.isUpperCase(ch)) {
                boolean nextCharInAupperCase = (i < name.length() - 1) &&
                    Character.isUpperCase(name.charAt(i + 1));
                if (!nextCharInAupperCase) {
                    buffer.append(" " + ch);
                } else {
                    buffer.append(ch);
                }
            } else {
                buffer.append(ch);
            }
        }

        return buffer.toString();
    }
}