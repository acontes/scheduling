package functionaltests.schedulerdb;

import junit.framework.Assert;

import org.junit.Test;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.ForkEnvironment;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.task.NativeTask;
import org.ow2.proactive.scheduler.common.task.TaskStatus;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.task.internal.InternalForkedJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalNativeTask;
import org.ow2.proactive.scheduler.task.internal.InternalTask;


public class TestTaskRuntimeData extends BaseSchedulerDBTest {

    @Test
    public void testTaskTypes() throws Exception {
        TaskFlowJob jobDef = new TaskFlowJob();
        JavaTask taskDef1 = createDefaultTask("task1");
        jobDef.addTask(taskDef1);
        JavaTask taskDef2 = createDefaultTask("task2");
        taskDef2.setForkEnvironment(new ForkEnvironment());
        jobDef.addTask(taskDef2);
        NativeTask taskDef3 = new NativeTask();
        taskDef3.setName("task3");
        taskDef3.setCommandLine("commandline");
        jobDef.addTask(taskDef3);

        InternalJob job = defaultSubmitJobAndLoadInternal(false, jobDef);
        Assert.assertEquals(InternalJavaTask.class, job.getTask("task1").getClass());
        Assert.assertEquals(InternalForkedJavaTask.class, job.getTask("task2").getClass());
        Assert.assertEquals(InternalNativeTask.class, job.getTask("task3").getClass());
    }

    @Test
    public void testTaskRuntimeData() throws Exception {
        TaskFlowJob job = new TaskFlowJob();

        JavaTask task1 = createDefaultTask("task1");
        task1.setMaxNumberOfExecution(5);
        job.addTask(task1);

        InternalJob jobData = defaultSubmitJobAndLoadInternal(true, job);
        Assert.assertEquals(1, jobData.getITasks().size());

        InternalTask runtimeData = jobData.getITasks().get(0);
        Assert.assertEquals("task1", runtimeData.getName());
        Assert.assertEquals(TaskStatus.SUBMITTED, runtimeData.getStatus());
        Assert.assertEquals(5, runtimeData.getNumberOfExecutionLeft());
        Assert.assertEquals(PASchedulerProperties.NUMBER_OF_EXECUTION_ON_FAILURE.getValueAsInt(), runtimeData
                .getNumberOfExecutionOnFailureLeft());
        Assert.assertNull(runtimeData.getDependences());
    }

    @Test
    public void testStartJobExecution() throws Exception {
        TaskFlowJob job = new TaskFlowJob();
        job.addTask(createDefaultTask("task1"));
        job.addTask(createDefaultTask("task2"));
        job.addTask(createDefaultTask("task3"));

        InternalJob internalJob = defaultSubmitJobAndLoadInternal(true, job);
        Assert.assertEquals(TaskStatus.SUBMITTED, internalJob.getTask("task1").getStatus());
        Assert.assertEquals(TaskStatus.SUBMITTED, internalJob.getTask("task2").getStatus());
        Assert.assertEquals(TaskStatus.SUBMITTED, internalJob.getTask("task3").getStatus());

        internalJob.start();
        InternalTask task = startTask(internalJob, internalJob.getTask("task1"));
        System.out.println("Job started");
        dbManager.jobTaskStarted(internalJob, task, true);

        internalJob = loadInternalJob(true, internalJob.getId());
        Assert.assertEquals(TaskStatus.RUNNING, internalJob.getTask("task1").getStatus());
        Assert.assertEquals(TaskStatus.PENDING, internalJob.getTask("task2").getStatus());
        Assert.assertEquals(TaskStatus.PENDING, internalJob.getTask("task3").getStatus());
    }

}
