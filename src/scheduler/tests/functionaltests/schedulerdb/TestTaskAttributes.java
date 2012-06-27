package functionaltests.schedulerdb;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.task.NativeTask;
import org.ow2.proactive.scheduler.common.task.ParallelEnvironment;
import org.ow2.proactive.scheduler.common.task.RestartMode;
import org.ow2.proactive.scheduler.common.task.Task;
import org.ow2.proactive.scheduler.common.task.flow.FlowActionType;
import org.ow2.proactive.scheduler.common.task.flow.FlowScript;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.task.internal.InternalTask;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.scripting.SimpleScript;
import org.ow2.proactive.topology.descriptor.ThresholdProximityDescriptor;
import org.ow2.proactive.topology.descriptor.TopologyDescriptor;


public class TestTaskAttributes extends BaseSchedulerDBTest {

    @Test
    public void testFlowBlock() throws Throwable {
        TaskFlowJob jobToSubmit = (TaskFlowJob) JobFactory.getFactory().createJob(
                new File(TestTaskAttributes.class.getResource(
                        "/functionaltests/workflow/descriptors/flow_crash_int_2.xml").toURI())
                        .getAbsolutePath());

        InternalJob job = defaultSubmitJobAndLoadInternal(true, jobToSubmit);

        for (Task task : jobToSubmit.getTasks()) {
            int expectedDeps = task.getDependencesList() != null ? task.getDependencesList().size() : 0;
            InternalTask internalTask = job.getTask(task.getName());
            int actualDeps = internalTask.getIDependences() != null ? internalTask.getIDependences().size()
                    : 0;
            Assert.assertEquals("Wrong dependecies for " + task.getName(), expectedDeps, actualDeps);
            Assert.assertEquals("Wrong flow block for " + task.getName(), task.getFlowBlock(), internalTask
                    .getFlowBlock());
        }
    }

    @Test
    public void testScripts() throws Exception {
        TaskFlowJob jobDef = new TaskFlowJob();
        JavaTask task1 = createDefaultTask("task1");

        task1.addSelectionScript(new SelectionScript("selection1", "js"));
        task1.addSelectionScript(new SelectionScript("selection2", "js"));
        task1.addSelectionScript(new SelectionScript("selection3", "js"));

        task1.setCleaningScript(new SimpleScript("cleanscript", "js"));
        task1.setPreScript(new SimpleScript("prescript", "js"));
        task1.setPostScript(new SimpleScript("postscript", "js"));
        task1.setFlowScript(FlowScript.createContinueFlowScript());

        jobDef.addTask(task1);

        InternalJob job = defaultSubmitJobAndLoadInternal(true, jobDef);

        InternalTask task = job.getTask("task1");

        Assert.assertEquals("cleanscript", task.getCleaningScript().getScript());
        Assert.assertEquals("prescript", task.getPreScript().getScript());
        Assert.assertEquals("postscript", task.getPostScript().getScript());
        Assert.assertEquals(FlowActionType.CONTINUE.toString(), task.getFlowScript().getActionType());

        Assert.assertEquals(3, task.getSelectionScripts().size());

        Set<String> scripts = new HashSet<String>();
        for (SelectionScript script : task.getSelectionScripts()) {
            scripts.add(script.getScript());
        }
        Set<String> expected = new HashSet<String>();
        expected.add("selection1");
        expected.add("selection2");
        expected.add("selection3");

        Assert.assertEquals(expected, scripts);
    }

    @Test
    public void testRestartMode() throws Exception {
        JavaTask task = createDefaultTask("task1");
        task.setRestartTaskOnError(RestartMode.ANYWHERE);

        InternalTask taskData = saveSingleTask(task).getTask("task1");
        Assert.assertSame(RestartMode.ANYWHERE, taskData.getRestartTaskOnError());

        task.setRestartTaskOnError(RestartMode.ELSEWHERE);
        taskData = saveSingleTask(task).getTask("task1");
        Assert.assertSame(RestartMode.ELSEWHERE, taskData.getRestartTaskOnError());
    }

    @Test
    public void testEmptyAttributes() throws Exception {
        JavaTask javaTask = createDefaultTask("task1");
        InternalTask taskData = saveSingleTask(javaTask).getTask("task1");
        Assert.assertNotNull(taskData.getId());

        NativeTask nativeTask = new NativeTask();
        nativeTask.setName("task1");
        nativeTask.setCommandLine("commandline");
        taskData = saveSingleTask(nativeTask).getTask("task1");
        Assert.assertNotNull(taskData.getId());
    }

    @Test
    public void testAttributes() throws Exception {
        JavaTask task = createDefaultTask("task");
        task.setCancelJobOnError(true);
        task.setDescription("desc");
        // TODO: create test using valid flow
        // task.setFlowBlock(FlowBlock.START);
        task.setMaxNumberOfExecution(7);
        task.setPreciousLogs(true);
        task.setPreciousResult(true);
        task.setRunAsMe(true);
        task.setWallTime(123);
        InternalTask taskData = saveSingleTask(task).getTask(task.getName());
        Assert.assertEquals(true, taskData.isCancelJobOnError());
        Assert.assertEquals("desc", taskData.getDescription());
        // Assert.assertEquals(FlowBlock.START, taskData.getFlowBlock());
        Assert.assertEquals(7, taskData.getMaxNumberOfExecution());
        Assert.assertEquals("task", taskData.getName());
        Assert.assertEquals(true, taskData.isPreciousLogs());
        Assert.assertEquals(true, taskData.isPreciousResult());
        Assert.assertEquals(true, taskData.isRunAsMe());
        Assert.assertEquals(123, taskData.getWallTime());
    }

    @Test
    public void testParallelEnv() throws Exception {
        JavaTask task = createDefaultTask("task");
        ParallelEnvironment env = new ParallelEnvironment(5);
        task.setParallelEnvironment(env);

        InternalTask taskData = saveSingleTask(task).getTask(task.getName());
        Assert.assertEquals(5, taskData.getParallelEnvironment().getNodesNumber());
        Assert.assertNull(taskData.getParallelEnvironment().getTopologyDescriptor());

        TopologyDescriptor[] descs = { TopologyDescriptor.ARBITRARY, TopologyDescriptor.BEST_PROXIMITY,
                TopologyDescriptor.DIFFERENT_HOSTS_EXCLUSIVE, TopologyDescriptor.MULTIPLE_HOSTS_EXCLUSIVE,
                TopologyDescriptor.SINGLE_HOST, TopologyDescriptor.SINGLE_HOST_EXCLUSIVE,
                new ThresholdProximityDescriptor(123) };

        for (TopologyDescriptor desc : descs) {
            task = createDefaultTask("task");
            env = new ParallelEnvironment(10, desc);
            task.setParallelEnvironment(env);
            taskData = saveSingleTask(task).getTask(task.getName());
            Assert.assertEquals(10, taskData.getParallelEnvironment().getNodesNumber());
            Assert.assertEquals(taskData.getParallelEnvironment().getTopologyDescriptor().getClass(), desc
                    .getClass());
            if (desc instanceof ThresholdProximityDescriptor) {
                Assert.assertEquals(((ThresholdProximityDescriptor) taskData.getParallelEnvironment()
                        .getTopologyDescriptor()).getThreshold(), 123);
            }
        }
    }

    @Test
    public void testGenericInfo() throws Exception {
        JavaTask task = createDefaultTask("task");
        Map<String, String> genericInfo;
        InternalTask taskData;

        genericInfo = new HashMap<String, String>();
        task.setGenericInformations(genericInfo);
        taskData = saveSingleTask(task).getTask(task.getName());

        Assert.assertNotNull(taskData.getGenericInformations());
        Assert.assertTrue(taskData.getGenericInformations().isEmpty());

        genericInfo = new HashMap<String, String>();
        genericInfo.put("p1", "v1");
        genericInfo.put("p2", "v2");
        task.setGenericInformations(genericInfo);
        taskData = saveSingleTask(task).getTask(task.getName());
        Assert.assertEquals(2, taskData.getGenericInformations().size());
        Assert.assertEquals("v1", taskData.getGenericInformations().get("p1"));
        Assert.assertEquals("v2", taskData.getGenericInformations().get("p2"));

        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longString.append("0123456789abcdefghijklmnopqrstuvwxyz");
        }
        genericInfo = new HashMap<String, String>();
        genericInfo.put("longProperty", longString.toString());
        task.setGenericInformations(genericInfo);
        taskData = saveSingleTask(task).getTask(task.getName());
        Assert.assertEquals(1, taskData.getGenericInformations().size());
        Assert.assertEquals(longString.toString(), taskData.getGenericInformations().get("longProperty"));
    }

}
