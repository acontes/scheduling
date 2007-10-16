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
package org.objectweb.proactive.extensions.calcium.system;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.calcium.environment.FileServerClient;
import org.objectweb.proactive.extensions.calcium.environment.RemoteFile;
import org.objectweb.proactive.extensions.calcium.statistics.Timer;


/**
 * This class is a Proxy for the real File class.
 *
 * @author The ProActive Team (mleyton)
 */
public class ProxyFile extends File {
    static Logger logger = ProActiveLogger.getLogger(Loggers.SKELETONS_SYSTEM);
    File wspace;
    File relative;
    File current; //current = wspace+"/"+relative
    RemoteFile remote;
    FileServerClient fserver;
    long lastmodified;
    long cachedSize;
    public long blockedFetchingTime;
    public long downloadedBytes;
    public long uploadedBytes;
    public int refCBefore;
    public int refCAfter;

    public ProxyFile(File wspace, File relative) {
        super(relative.getName());

        this.current = new File(wspace, relative.getPath());
        this.relative = relative;
        this.remote = null;
        setWSpace(null, wspace);
        this.lastmodified = this.cachedSize = 0;
        this.blockedFetchingTime = this.uploadedBytes = this.downloadedBytes = 0;

        this.refCBefore = this.refCAfter = 0;
    }

    public ProxyFile(File wspace, String name) {
        this(wspace, new File(name));
    }

    public void setWSpace(FileServerClient fserver, File wspace) {
        this.fserver = fserver;
        this.wspace = wspace;
    }

    public void store(FileServerClient fserver, int refCount)
        throws IOException {
        this.cachedSize = current.length();
        this.uploadedBytes += current.length();
        this.remote = fserver.store(current, refCount);
        this.lastmodified = current.lastModified();
    }

    public void saveRemoteDataInWSpace() throws IOException {
        int i = 1;

        if (isLocallyStored()) {
            return; //Nothing to do, data already donwloaded
        }

        current = new File(wspace, relative.getPath());
        while (current.exists()) {
            current = new File(wspace, relative.getPath() + "-" + i++);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Fetching data into:" + this.current);
        }
        fserver.fetch(remote, this.current);
        this.lastmodified = this.current.lastModified();
        this.downloadedBytes += current.length();
    }

    public boolean isRemotelyStored() {
        return remote != null;
    }

    public boolean isLocallyStored() {
        return current != null;
    }

    public boolean hasBeenModified() {

        /* TODO improve by:
             *
             *  1. Also considering the hashcode.
             *  2. Keeping track of lastModified access.
             */
        if (current == null) {
            return false;
        }

        return lastmodified != current.lastModified();
    }

    public void handleStageOut(FileServerClient fserver)
        throws IOException {
        if (isNew()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Storing new file: [" + refCBefore + "," +
                    refCAfter + "] " + relative + " (" + current + ")");
            }

            store(fserver, refCAfter);

            return;
        }

        if (isModified()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Storing modified file: [" + refCBefore + "," +
                    refCAfter + "] " + relative + " (" + current + ")");
            }

            fserver.commit(remote.fileId, -refCBefore);

            store(fserver, refCAfter);

            return;
        }

        if (isNormal()) { //&& (refCAfter - refCBefore !=0)){
            if (logger.isDebugEnabled()) {
                logger.debug("Updating normal file: [" + refCBefore + "," +
                    refCAfter + "] " + relative + " (" + current + ")");
            }

            fserver.commit(remote.fileId, refCAfter - refCBefore);
            return;
        }

        logger.error("Illegal ProxyFile state: " + refCAfter + "a " +
            refCBefore + "b" + " data modified=" + hasBeenModified());

        //Reached here, not good!
        throw new IOException("ProxyFile reached illegal state:" + this);
    }

    public void setStageOutState() {
        this.current = null;
        this.wspace = null;
        this.fserver = null;
        this.blockedFetchingTime = this.uploadedBytes = this.downloadedBytes = 0;
        this.refCBefore = this.refCAfter = 0;
    }

    private boolean isNew() {
        return (this.refCBefore == 0) && (this.refCAfter != 0);
    }

    private boolean isNormal() {
        return (this.refCBefore != 0) && !hasBeenModified();
    }

    private boolean isModified() {
        return (this.refCBefore != 0) && (this.refCAfter != 0) &&
        hasBeenModified();
    }

    public File getWSpaceFile() {
        return wspace;
    }

    public File getCurrent() {
        if (current == null) {
            Timer t = new Timer();
            t.start();

            logger.debug("Blocking waiting for file's data:" + wspace + "/" +
                relative);

            /*
                try{
                        throw new Exception();
                }catch(Exception e){
                        e.printStackTrace();
                }
                */
            try {
                saveRemoteDataInWSpace();
            } catch (IOException e) {
                throw new IllegalArgumentException(e); //TODO change this exception type
            }
            t.stop();
            this.blockedFetchingTime = 1 + t.getTime();
        }
        return current;
    }

    /* ***********************************************************
     *
     *         BEGIN EXTENDED FILE METHODS
     *
     * ***********************************************************/
    public String getName() {
        return relative.getName();
    }

    public String getParent() {
        return getCurrent().getParent();
    }

    public File getParentFile() {
        return getCurrent().getParentFile();
    }

    public String getPath() {
        return getCurrent().getPath();
    }

    public boolean isAbsolute() {
        return getCurrent().isAbsolute();
    }

    public String getAbsolutePath() {
        return getCurrent().getAbsolutePath();
    }

    public File getAbsoluteFile() {
        return getCurrent().getAbsoluteFile();
    }

    public String getCanonicalPath() throws IOException {
        return getCurrent().getCanonicalPath();
    }

    public File getCanonicalFile() throws IOException {
        return getCurrent().getCanonicalFile();
    }

    public URL toURL() throws MalformedURLException {
        return getCurrent().toURL();
    }

    public URI toURI() {
        return getCurrent().toURI();
    }

    public boolean canRead() {
        return getCurrent().canRead();
    }

    public boolean canWrite() {
        return getCurrent().canWrite();
    }

    public boolean exists() {
        return getCurrent().exists();
    }

    public boolean isDirectory() {
        return getCurrent().isDirectory();
    }

    public boolean isFile() {
        return getCurrent().isFile();
    }

    public boolean isHidden() {
        return getCurrent().isHidden();
    }

    public long lastModified() {
        return getCurrent().lastModified();
    }

    public long length() {
        if (!isLocallyStored()) {
            return cachedSize;
        }

        return getCurrent().length();
    }

    public boolean createNewFile() throws IOException {
        return getCurrent().createNewFile();
    }

    public boolean delete() {
        return getCurrent().delete();
    }

    public void deleteOnExit() {
        getCurrent().deleteOnExit();
    }

    public String[] list() {
        return getCurrent().list();
    }

    public String[] list(FilenameFilter filter) {
        return getCurrent().list();
    }

    public File[] listFiles() {
        return getCurrent().listFiles();
    }

    public File[] listFiles(FilenameFilter filter) {
        return getCurrent().listFiles(filter);
    }

    public File[] listFiles(FileFilter filter) {
        return getCurrent().listFiles(filter);
    }

    public boolean mkdir() {
        return getCurrent().mkdir();
    }

    public boolean mkdirs() {
        return getCurrent().mkdirs();
    }

    public boolean renameTo(File dest) {
        //TODO check this method
        boolean res = getCurrent().renameTo(new File(wspace, dest.getPath()));

        if (res) {
            relative = dest;
        }

        return res;
    }

    public boolean setLastModified(long time) {
        return getCurrent().setLastModified(time);
    }

    public boolean setReadOnly() {
        return getCurrent().setReadOnly();
    }

    public int compareTo(File pathname) {
        return getCurrent().compareTo(pathname);
    }

    public boolean equals(Object obj) {
        return getCurrent().equals(obj);
    }

    public int hashCode() {
        return getCurrent().hashCode();
    }

    public String toString() {
        return getCurrent().toString();
    }

    public static void main(String[] args) throws Exception {
        WSpaceImpl wspace = new WSpaceImpl(new File("/tmp/calcium"));
        File f = wspace.copyInto(new File("/home/mleyton/IMG00070.jpg"));
    }
}
