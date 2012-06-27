package functionaltests.schedulerdb;

import junit.framework.Assert;

import org.junit.Test;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.TaskStatus;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.task.internal.InternalTask;


public class TestJobRuntimeData extends BaseSchedulerDBTest {

    @Test
    public void testChangePriority() throws Exception {
        TaskFlowJob job = new TaskFlowJob();
        job.setPriority(JobPriority.LOW);
        InternalJob jobData = defaultSubmitJobAndLoadInternal(false, new TaskFlowJob());

        dbManager.changeJobPriority(jobData.getId(), JobPriority.HIGH);

        jobData = loadInternalJob(false, jobData.getId());
        Assert.assertEquals(JobPriority.HIGH, jobData.getPriority());
    }

    @Test
    public void testJobRuntimeData() throws Exception {
        TaskFlowJob job = new TaskFlowJob();
        job.setName("test job");
        job.addTask(createDefaultTask("task1"));
        job.setPriority(JobPriority.LOW);

        System.out.println("Submit and load job");
        InternalJob runtimeData = defaultSubmitJobAndLoadInternal(true, job);
        Assert.assertEquals(JobStatus.PENDING, runtimeData.getStatus());
        Assert.assertEquals(DEFAULT_USER_NAME, runtimeData.getOwner());
        Assert.assertEquals(JobPriority.LOW, runtimeData.getPriority());
        Assert.assertEquals(0, runtimeData.getNumberOfPendingTasks());
        Assert.assertEquals(1, runtimeData.getTotalNumberOfTasks());
        Assert.assertEquals(0, runtimeData.getNumberOfFinishedTasks());
        Assert.assertEquals(0, runtimeData.getNumberOfRunningTasks());
        Assert.assertEquals("test job", runtimeData.getName());
        Assert.assertNotNull(runtimeData.getCredentials());
        Assert.assertEquals(1, runtimeData.getITasks().size());

        runtimeData.start();
        InternalTask internalTask = startTask(runtimeData, runtimeData.getITasks().get(0));

        System.out.println("Update started task data");
        dbManager.jobTaskStarted(runtimeData, internalTask, false);

        System.out.println("Load internal job");
        runtimeData = loadInternalJob(true, runtimeData.getId());

        Assert.assertEquals("test job", runtimeData.getJobInfo().getJobId().getReadableName());
        Assert.assertEquals(JobStatus.RUNNING, runtimeData.getStatus());
        Assert.assertEquals(1, runtimeData.getNumberOfRunningTasks());
        Assert.assertEquals(0, runtimeData.getNumberOfFinishedTasks());
        Assert.assertEquals(0, runtimeData.getNumberOfPendingTasks());
        Assert.assertTrue(runtimeData.getStartTime() > 0);

        internalTask = runtimeData.getITasks().get(0);
        Assert.assertEquals(TaskStatus.RUNNING, internalTask.getStatus());
        Assert.assertTrue(internalTask.getStartTime() > 0);
        Assert.assertNotNull(internalTask.getExecutionHostName());
    }

}
