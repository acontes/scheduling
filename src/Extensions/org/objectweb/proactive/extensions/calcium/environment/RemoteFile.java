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
package org.objectweb.proactive.extensions.calcium.environment;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


public interface RemoteFile extends java.io.Serializable {
    static Logger logger = ProActiveLogger.getLogger(Loggers.SKELETONS_SYSTEM);

    /**
     * Stores a copy of the file in the specified location.
     *
     * @param f The location to store the file.
     * @throws IOException
     */
    public boolean saveAs(File f) throws IOException;

    /**
     * The remote file is scheduled for deletion.
     *
     * The file will be deleted when the File Server
     * implementation feels adecuate.
     */
    public void discountReference(FileServer fserver);

    public void countReference(FileServer fserver);

    public String getMD5Hash();

    public long length();
}
