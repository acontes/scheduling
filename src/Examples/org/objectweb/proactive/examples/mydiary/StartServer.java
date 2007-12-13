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
package org.objectweb.proactive.examples.mydiary;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptor;


/**
 * @author acontes
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StartServer {
    public static void main(String[] args) {
        Diary diary = null;
        try {
            ProActiveDescriptor pad = PADeployment.getProactiveDescriptor(args[0]);

            pad.activateMappings();

            if (args.length == 2) {
                diary = (Diary) PAActiveObject.newActive(
                        "org.objectweb.proactive.examples.mydiary.DiaryImpl", new Object[] {});
            } else {
                diary = (Diary) PAActiveObject.newActive(
                        "org.objectweb.proactive.examples.mydiary.DiaryImpl", new Object[] { args[1] });
            }
            PAActiveObject.register(diary, "//localhost/MyDiary");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
