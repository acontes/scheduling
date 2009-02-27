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
package org.ow2.proactive.scheduler.util.classloading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.jar.JarFile;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.util.JarUtils;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.utils.FileToBytesConverter;


/**
 * This class defines a classserver based on ProActive remote objects. It creates classpath files in
 * the scheduler temporary directory (see pa.scheduler.classserver.tmpdir property), and serves classes
 * contained in these files.
 * @author The ProActive team 
 * @since ProActive Scheduling 0.9
 */
public class TaskClassServer {
    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.CORE);

    // temp directory for unjaring classpath : if not defined, java.io.tmpdir is used.
    private static final String tmpTmpJarFilesDir = PASchedulerProperties.SCHEDULER_CLASSSERVER_TMPDIR
            .getValueAsString();
    private static final String tmpJarFilesDir = tmpTmpJarFilesDir != null ? tmpTmpJarFilesDir +
        (tmpTmpJarFilesDir.endsWith(File.separator) ? "" : File.separator) : System
            .getProperty("java.io.tmpdir") +
        File.separator;

    // indicate if cache should be used
    private static final boolean useCache = PASchedulerProperties.SCHEDULER_CLASSSERVER_USECACHE
            .getValueAsBoolean();

    // cache for byte[] classes
    private Hashtable<String, byte[]> cachedClasses;

    // root classpath (directory *or* jar file)
    private File classpath;

    // jobid of the served job
    private JobId servedJobId;

    /**
     * Empty constructor for remote object creation.
     */
    public TaskClassServer() {
    }

    /**
     * Create a new class server. 
     * @param pathToClasspath the path to the jar file containing the classes that 
     * should be served.
     * @throws IOException if the class server cannot be created.
     */
    public TaskClassServer(JobId jid) {
        this.servedJobId = jid;
        this.cachedClasses = useCache ? new Hashtable<String, byte[]>() : null;
    }

    /**
     * Activate this TaskClassServer. The activation creates all needed files (jar file, classes directory and crc file)
     * in the defined temporary directory (see pa.scheduler.classserver.tmpdir property).
     * @param userClasspathJarFile the content of the classpath
     * @param deflateJar true if the classpath contains jar file, false otherwise
     * @throws IOException if the files cannot be created
     */
    public void activate(byte[] userClasspathJarFile, boolean deflateJar) throws IOException {
        // check if the classpath exists already in the deflated classpathes
        // for now, only in case of recovery
        // TODO cdelbe : look for cp only with crc to avoid mutliple tcs for the same classpath

        // open files 
        File jarFile = new File(this.getPathToJarFile());
        File dirClasspath = new File(this.getPathToClassDir());
        File crcFile = new File(this.getPathToCrcFile());

        boolean classpathAlreadyExists = jarFile.exists() || (deflateJar && dirClasspath.exists());
        boolean reuseExistingFiles = false;

        // check if an already classpath can be reused
        if (classpathAlreadyExists) {
            try {
                // the classpath for this job has already been deflated
                reuseExistingFiles = true;
                // check crc ...
                if (crcFile.exists()) {
                    BufferedReader crcReader = new BufferedReader(new FileReader(crcFile));
                    String read = crcReader.readLine();
                    CRC32 actualCrc = new CRC32();
                    actualCrc.update(userClasspathJarFile);
                    if (Long.parseLong(read) != actualCrc.getValue()) {
                        // the classpath cannot be reused
                        reuseExistingFiles = false;
                    }
                } else {
                    // no crc : cancel
                    reuseExistingFiles = false;
                }
                // check deflated cp if any
                if (deflateJar && !dirClasspath.exists()) {
                    reuseExistingFiles = false;
                }
            } catch (Exception e) {
                logger_dev.warn(e);
                // if any exception occurs, cancel 
                reuseExistingFiles = false;
            }
        }

        // delete old classpath if it cannot be reused
        if (classpathAlreadyExists && !reuseExistingFiles) {
            // delete classpath files
            jarFile.delete();
            deleteDirectory(dirClasspath);
            crcFile.delete();
        }

        // if no files can be reused, create new ones.
        if (!reuseExistingFiles) {
            // create jar file
            FileOutputStream fos = new FileOutputStream(jarFile);
            fos.write(userClasspathJarFile);
            fos.flush();
            fos.close();

            //create tmp directory for delfating classpath
            if (deflateJar) {
                dirClasspath.mkdir();
                JarUtils.unjar(new JarFile(jarFile), dirClasspath);
            }

            // create crc file
            FileWriter fosCrc = new FileWriter(crcFile);
            CRC32 crc = new CRC32();
            crc.update(userClasspathJarFile);
            fosCrc.write("" + crc.getValue());
            fosCrc.flush();
            fosCrc.close();
        }

        // set the actual classpath
        this.classpath = deflateJar ? dirClasspath : jarFile;
    }

    /**
     * Desactivate this TaskClassServer. The classpath files are deleted, and the
     * classfiles cache is cleared.
     */
    public void desactivate() {
        // delete classpath files
        File jarFile = new File(this.getPathToJarFile());
        File deflatedJarFile = new File(this.getPathToClassDir());
        File crcFile = new File(this.getPathToCrcFile());
        jarFile.delete();
        deleteDirectory(deflatedJarFile);
        crcFile.delete();
        // delete cache
        if (this.cachedClasses != null) {
            this.cachedClasses.clear();
        }
    }

    /**
     * Return the byte[] representation of the classfile for the class classname.
     * @param classname the name of the looked up class
     * @return the byte[] representation of the classfile for the class classname.
     * @throws ClassNotFoundException if the class classname cannot be found
     */
    public byte[] getClassBytes(String classname) throws ClassNotFoundException {
        byte[] cb = useCache ? this.cachedClasses.get(classname) : null;
        if (cb == null) {
            try {
                cb = this.classpath.isFile() ? this.lookIntoJarFile(classname, new JarFile(classpath)) : this
                        .lookIntoDirectory(classname, classpath);
                if (useCache) {
                    this.cachedClasses.put(classname, cb);
                }
            } catch (IOException e) {
                logger_dev.error(e);
                throw new ClassNotFoundException("Class " + classname + " has not be found in " +
                    classpath.getAbsolutePath() + ". Caused by " + e);
            }
        }
        return cb;
    }

    /**
     * Look for a classfile into a directory.
     * @param classname the looked up class.
     * @param directory the directory to look into.
     * @return the byte[] representation of the class if found, null otherwise.
     * @throws IOException if the jar file cannot be read.
     */
    private byte[] lookIntoDirectory(String classname, File directory) throws IOException {
        String pathToClass = convertNameToPath(classname);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    byte[] resInDir = lookIntoDirectory(classname, files[i]);
                    if (resInDir != null) {
                        return resInDir;
                    }
                } else if (isJarFile(files[i])) {
                    byte[] resInJar = lookIntoJarFile(classname, new JarFile(files[i]));
                    if (resInJar != null) {
                        return resInJar;
                    }
                } else if (isClassFile(files[i]) && files[i].getAbsolutePath().endsWith(pathToClass)) {
                    // TODO cdelbe : conlicts possible ? 
                    return FileToBytesConverter.convertFileToByteArray(files[i]);
                }
            }
            // not found
            return null;
        } else {
            throw new IOException("Directory " + directory.getAbsolutePath() + " does not exist");
        }
    }

    /**
     * Look for a class definition into a jar file.
     * @param classname the looked up class.
     * @param file the jar file.
     * @return the byte[] representation of the class if found, null otherwise.
     * @throws IOException if the jar file cannot be read.
     */
    private byte[] lookIntoJarFile(String classname, JarFile file) throws IOException {
        byte result[] = null;
        ZipEntry entry = file.getEntry(convertNameToPath(classname));
        if (entry != null) {
            InputStream inStream = file.getInputStream(entry);
            result = new byte[inStream.available()];
            inStream.read(result);
            inStream.close();
            return result;
        } else {
            return null;
        }
    }

    /**
     * Return true if f is a jar file.
     */
    private boolean isJarFile(File f) {
        return f.isFile() && f.getName().endsWith(".jar");
    }

    /**
     * Return true if f is a class file.
     */
    private boolean isClassFile(File f) {
        return f.isFile() && f.getName().endsWith(".class");
    }

    /**
     * Convert classname parameter (qualified) into path to the class file 
     * (with the .class suffix)
     */
    private String convertNameToPath(String classname) {
        return classname.replace('.', '/') + ".class";
    }

    /**
     * Convert the path to a class into a qualified classname.
     */
    @SuppressWarnings("unused")
    private String convertPathToName(String path) {
        return path.replace('/', '.').substring(0, path.length() - ".class".length());
    }

    /**
     * Return the path to the associated jar file
     * @return the path to the associated jar file
     */
    private String getPathToJarFile() {
        return tmpJarFilesDir + servedJobId.toString() + ".jar";
    }

    /**
     * Return the path to the associated crc file
     * @return the path to the associated crc file
     */
    private String getPathToCrcFile() {
        return tmpJarFilesDir + servedJobId.toString() + ".crc";
    }

    /**
     * Return the path to the associated classfiles directory
     * @return the path to the associated classfiles directory
     */
    private String getPathToClassDir() {
        return tmpJarFilesDir + servedJobId.toString();
    }

    /**
     * Recursive delete for directories
     * @param path
     */
    private static void deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
            path.delete();
        }
    }

}
