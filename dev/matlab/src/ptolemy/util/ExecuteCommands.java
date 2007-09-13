/* Execute commands in a subprocess.

 Copyright (c) 2006 The Regents of the University of California.
 All rights reserved.
 Permission is hereby granted, without written agreement and without
 license or royalty fees, to use, copy, modify, and distribute this
 software and its documentation for any purpose, provided that the above
 copyright notice and the following two paragraphs appear in all copies
 of this software.

 IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY
 FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 THE UNIVERSITY OF CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE
 PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND THE UNIVERSITY OF
 CALIFORNIA HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

 PT_COPYRIGHT_VERSION_2
 COPYRIGHTENDKEY
 */
package ptolemy.util;

import java.io.File;
import java.util.List;

/** Interface for classes execute commands in a subprocess.

 <p>Loosely based on Example1.java from
 http://java.sun.com/products/jfc/tsc/articles/threads/threads2.html
 <p>See also
 http://developer.java.sun.com/developer/qow/archive/135/index.jsp
 and
 http://jw.itworld.com/javaworld/jw-12-2000/jw-1229-traps.html

 @see StreamExec

 @author Christopher Brooks
 @version $Id: ExecuteCommands.java,v 1.11 2006/03/29 20:59:41 cxh Exp $
 @since Ptolemy II 5.2
 @Pt.ProposedRating Red (cxh)
 @Pt.AcceptedRating Red (cxh)
 */
public interface ExecuteCommands {

    ///////////////////////////////////////////////////////////////////
    ////                         public methods                    ////

    /** Cancel any running commands. */
    public void cancel();

    /** Clear the text area, status bar and progress bar. */
    public void clear();

    /** Return the return code of the last subprocess that was executed.
     *  @return the return code of the last subprocess that was executed.
     */
    public int getLastSubprocessReturnCode();

    /** Set the list of commands.
     *  @param commands A list of Strings, where each element is a command.
     */
    public void setCommands(List commands);

    /** Set the working directory of the subprocess.
     *  @param workingDirectory The working directory of the
     *  subprocess.  If this argument is null, then the subprocess is
     *  executed in the working directory of the current process.
     */
    public void setWorkingDirectory(File workingDirectory);

    /** Start running the commands. */
    public void start();

    /** Append the text message to stderr.  Classes that implement
     *  this method could append to a StringBuffer or JTextArea.
     *  The output automatically gets a trailing newline  appended.
     *  @param text The text to append to standard error.
     */
    public void stderr(final String text);

    /** Append the text message to stderr.  Classes that implement
     *  this method could append to a StringBuffer or JTextArea.
     *  The output automatically gets a trailing newline  appended.
     *  @param text The text to append to standard out.
     */
    public void stdout(final String text);

    /** Set the text of the status bar.  In this base class, do
     *  nothing, derived classes may update a status bar.
     *  @param text The text with which the status bar is updated.
     */
    public void updateStatusBar(final String text);
}
