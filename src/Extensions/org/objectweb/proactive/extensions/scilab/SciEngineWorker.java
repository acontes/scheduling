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
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.extensions.scilab;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


public class SciEngineWorker implements Serializable {

    /**
         *
         */
    private static final long serialVersionUID = -815964410313302240L;
    private GeneralTask genTask;
    private GeneralResult genResult;
    private long dateStart;
    private long dateEnd;
    private boolean isStateFull = false;
    private static Logger logger = ProActiveLogger.getLogger(Loggers.SCILAB_TASK);

    public SciEngineWorker() {
    }

    /**
     * execute a task
     * @param sciTask Scilab task
     * @return result of the computation
     * @throws TaskException
     */
    public GeneralResult execute(GeneralTask genTask) {
        if (logger.isDebugEnabled()) {
            logger.debug("->SciEngineWorker In:execute:" + genTask.getId());
        }

        this.genTask = genTask;
        this.genResult = new GeneralResultImpl(genTask.getId());

        this.dateStart = System.currentTimeMillis();
        try {
            this.genTask.init();
            this.sendInputDataToEngine();
            if (executeJob()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("->SciEngineWorker :execute : success\n");
                }
                this.genResult.setMessage(genTask.getLastMessage());
                receiveOutputFromEngine();
                this.genResult.setState(GeneralResult.SUCCESS);
            } else {
                this.genResult.setState(GeneralResult.ABORT);
                this.genResult.setException(new MatlabException(
                        "The MATLAB engine is closed"));
                if (logger.isDebugEnabled()) {
                    logger.debug("->SciEngineWorker :execute : abort\n");
                }
            }
        } catch (TaskException e) {
            e.printStackTrace();
            this.genResult.setState(GeneralResult.ABORT);
            this.genResult.setException(e);
        }

        this.dateEnd = System.currentTimeMillis();
        this.genResult.setTimeExecution(this.dateEnd - this.dateStart);
        return genResult;
    }

    /**
     * set In data in Scilab environment
     * @throws TaskException
     *
     */
    private void sendInputDataToEngine() throws TaskException {
        genTask.sendListDataIn();
    }

    /**
     * clear Scilab environment
     *
     */
    private void clearWorkspace() {
        try {
            genTask.clearWorkspace();
        } catch (TaskException e) {
            this.genResult.setState(GeneralResult.ABORT);
            this.genResult.setException(e);
        }
    }

    /**
     * retrieve results of the computation
     * @throws TaskException
     *
     */
    private void receiveOutputFromEngine() throws TaskException {
        List<AbstractData> datas = null;
        datas = genTask.receiveDataOut();

        for (AbstractData data : datas) {
            if (logger.isDebugEnabled()) {
                logger.debug("->SciEngineWorker :ReceiveData : " +
                    data.getName());
            }

            this.genResult.add(data);
        }

        if (!isStateFull) {
            clearWorkspace();
        }
    }

    /**
     * @return true if is a valid execution, otherwise false
     * @throws TaskException
     * @throws Exception
     */
    private boolean executeJob() throws TaskException {
        return genTask.execute();
    }

    public void exit() {
        System.exit(0);
    }
}
