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
package org.objectweb.proactive.core.filetransfer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.objectweb.proactive.annotation.PublicAPI;
import org.objectweb.proactive.core.node.Node;


/**
 * This class represent the result of a file transfer operation.
 *
 * When a file transfer operation is invoked, for example by using the
 * {@link org.objectweb.proactive.api.PAFileTransfer  PAFileTransfer} API, a RemoteFile instance
 * is returned. A RemoteFile can be used, among others, to determine
 * if the file transfer operation has been completed, wait for the
 * file transfer operation to finish, obtain a reference on the node
 * where the file is stored, etc...
 *
 * Additionally, new file transfer operations can be triggered from
 * this class, for example to push/pull a remote file to another node.
 */
@PublicAPI
public interface RemoteFile extends Serializable {

    /**
     * @return true if the file transfer operation that spawned this RemoteFile instance has finished, or an exception. false otherwise.
     */
    public boolean isFinished();

    /**
     * This method blocks the calling thread until the file transfer operation is
     * finished, or failed. If the operation failed, the exception is raised.
     * @throws IOException The cause of the file transfer operation failure.
     */
    public void waitFor() throws IOException;

    /**
     * Pulls the remote file represented by this instance into the local destination.
     * The result of this operation yields a new RemoteFile instance.
     *
     * @param localDst The local destination where the file will be stored.
     * @return A new RemoteFile instance representing the file transfer operation on the local node.
     * @throws IOException  If an error is detected during the initialization phase of the file transfer.
     */
    public RemoteFile pull(File localDst) throws IOException;

    /**
     * Push the RemoteFile represented by this instance to the Node and File location provided as parameters.
     * @param dstNode The destination node.
     * @param dstRemote The destination file on the destination node.
     * @return A new RemoteFile instance representing this file transfer operation.
     * @throws IOException If an error was detected during the initialization phase of the file transfer.
     */
    public RemoteFile push(Node dstNode, File dstRemote) throws IOException;

    /**
     * @return The node where the file is stored (or was meant to be stored, if an error took place).
     */
    public Node getRemoteNode();

    /**
     * @return The destination File where the data is stored on the remote node (or was meant to be stored, if an error took place).
     */
    public File getRemoteFilePath();

    /**
     * Deletes a remote file or directory recursively (i.e. deletes non-empty directories) represented by this RemoteFile instance.
     * This methods is synchronous.
     *
     * @return true if the delete was successful, false otherwise.
     * @throws IOException If an error was encountered while deleting the file
     */
    public boolean delete() throws IOException;

    /**
     * Queries the existence of a RemoteFile. This method is synchronous (blocking).
     *
     * @return true if the remote file exists, false otherwise.
     * @throws IOException If an error is encountered while querying the remote file.
     */
    public boolean exists() throws IOException;

    /**
     * Queries if the RemoteFile is a directory. This method is synchronous (blocking).
     *
     * @return true if the remote file is a directory, false otherwise.
     * @throws IOException If an error is encountered while querying the remote file.
     */
    public boolean isDirectory() throws IOException;

    /**
     * Queries if the RemoteFile is a File. This method is synchronous (blocking).
     *
     * @return true if the remote file is a file, false otherwise
     * @throws IOException If an error is encountered while querying the remote file.
     */
    public boolean isFile() throws IOException;
}
