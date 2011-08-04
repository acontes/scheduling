/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package functionaltests;

import java.io.File;
import java.net.URL;

import org.junit.Assert;
import org.objectweb.proactive.utils.OperatingSystem;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskStatus;

import org.ow2.tests.FunctionalTest;


/**
 * Test whether attribute 'workingDir' set in native task element in a job descriptor
 * set properly the launching directory of the native executable (equivalent to linux PWD)
 *
 * @author The ProActive Team
 * @date 2 jun 08
 */
public class TestWorkingDirDynamicCommand extends FunctionalTest {

    private static URL jobDescriptor = TestWorkingDirDynamicCommand.class
            .getResource("/functionaltests/descriptors/Job_test_workingDir_dynamic_Command.xml");

    private static String executablePathPropertyName = "EXEC_PATH";

    private static String beginCommand = "WCOM";

    private static URL executablePath = TestWorkingDirDynamicCommand.class
            .getResource("/functionaltests/executables/test_working_dir.sh");

    private static URL executablePathWindows = TestWorkingDirDynamicCommand.class
            .getResource("/functionaltests/executables/test_working_dir.bat");

    private static String WorkingDirPropertyName = "WDIR";

    private static URL workingDirPath = TestWorkingDirDynamicCommand.class
            .getResource("/functionaltests/executables");

    /**
     * Tests start here.
     *
     * @throws Throwable any exception that can be thrown during the test.
     */
    @org.junit.Test
    public void run() throws Throwable {
        String task1Name = "task1";
        //set system Property for executable path
        switch (OperatingSystem.getOperatingSystem()) {
            case windows:
                System.setProperty(executablePathPropertyName, new File(executablePathWindows.toURI())
                        .getAbsolutePath().replace("\\", "\\\\"));
                System.setProperty(beginCommand, "cmd /C");
                System.setProperty(WorkingDirPropertyName, new File(workingDirPath.toURI()).getAbsolutePath()
                        .replace("\\", "\\\\"));
                break;
            case unix:
                System.setProperty(executablePathPropertyName, new File(executablePath.toURI())
                        .getAbsolutePath());
                System.setProperty(beginCommand, "");
                SchedulerTHelper.setExecutable(new File(executablePath.toURI()).getAbsolutePath());
                System
                        .setProperty(WorkingDirPropertyName, new File(workingDirPath.toURI())
                                .getAbsolutePath());
                break;
            default:
                throw new IllegalStateException("Unsupported operating system");
        }

        //test submission and event reception
        JobId id = SchedulerTHelper.submitJob(new File(jobDescriptor.toURI()).getAbsolutePath());

        SchedulerTHelper.log("Job submitted, id " + id.toString());

        SchedulerTHelper.log("Waiting for jobSubmitted Event");
        JobState receivedState = SchedulerTHelper.waitForEventJobSubmitted(id);

        Assert.assertEquals(receivedState.getId(), id);

        SchedulerTHelper.log("Waiting for job running");
        JobInfo jInfo = SchedulerTHelper.waitForEventJobRunning(id);
        Assert.assertEquals(jInfo.getJobId(), id);
        Assert.assertEquals(JobStatus.RUNNING, jInfo.getStatus());

        SchedulerTHelper.waitForEventTaskRunning(id, task1Name);
        TaskInfo tInfo = SchedulerTHelper.waitForEventTaskFinished(id, task1Name);

        SchedulerTHelper.log(SchedulerTHelper.getSchedulerInterface().getTaskResult(id, "task1").getOutput()
                .getAllLogs(false));

        Assert.assertEquals(TaskStatus.FINISHED, tInfo.getStatus());

        SchedulerTHelper.waitForEventJobFinished(id);
        JobResult res = SchedulerTHelper.getJobResult(id);

        //check that there is one exception in results
        Assert.assertTrue(res.getExceptionResults().size() == 0);

        //remove job
        SchedulerTHelper.removeJob(id);
        SchedulerTHelper.waitForEventJobRemoved(id);
    }
}
