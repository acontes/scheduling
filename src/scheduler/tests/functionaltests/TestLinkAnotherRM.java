package functionaltests;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;


public class TestLinkAnotherRM extends MultipleRMTBase {

    private static final int NODES_NUMBER = 2;

    static final String TASK_NAME1 = "Test task1";

    static final String TASK_NAME2 = "Test task2";

    public static class CommunicationObject {

        private String command;

        public String getCommand() {
            String result = command;
            command = null;
            return result;
        }

        public void setCommand(String command) {
            this.command = command;
        }

    }

    public static class TestJavaTask1 extends JavaExecutable {

        private String communicationObjectUrl;

        @Override
        public Serializable execute(TaskResult... results) throws Throwable {
            CommunicationObject communicationObject = PAActiveObject.lookupActive(CommunicationObject.class,
                    communicationObjectUrl);

            while (true) {
                String command = communicationObject.getCommand();
                if (command == null) {
                    Thread.sleep(1000);
                    continue;
                }
                if (command.equals("stop")) {
                    break;
                } else {
                    throw new IllegalArgumentException(command);
                }
            }

            return "OK";
        }

    }

    private TaskFlowJob createJob(String communicationObjectUrl) throws Exception {
        TaskFlowJob job = new TaskFlowJob();
        job.setName("Test job");

        JavaTask javaTask1 = new JavaTask();
        javaTask1.setExecutableClassName(TestJavaTask1.class.getName());
        javaTask1.addArgument("communicationObjectUrl", communicationObjectUrl);
        javaTask1.setName(TASK_NAME1);
        job.addTask(javaTask1);

        JavaTask javaTask2 = new JavaTask();
        javaTask2.setExecutableClassName(TestJavaTask1.class.getName());
        javaTask2.addArgument("communicationObjectUrl", communicationObjectUrl);
        javaTask2.setName(TASK_NAME2);
        javaTask2.addDependence(javaTask1);
        job.addTask(javaTask2);

        return job;
    }

    @Test
    public void testLink() throws Exception {
        RMTHelper helper1 = RMTHelper.getDefaultInstance();
        RMTHelper helper2 = new RMTHelper();

        // start two resource managers, they must use different RMI and JMX ports

        int rmiPort1 = CentralPAPropertyRepository.PA_RMI_PORT.getValue() + 1;
        int jmxPort1 = PAResourceManagerProperties.RM_JMX_PORT.getValueAsInt() + 1;

        int rmiPort2 = CentralPAPropertyRepository.PA_RMI_PORT.getValue() + 2;
        int jmxPort2 = PAResourceManagerProperties.RM_JMX_PORT.getValueAsInt() + 2;

        String rmUrl1 = helper1.startRM(config1.getAbsolutePath(), rmiPort1,
                PAResourceManagerProperties.RM_JMX_PORT.getCmdLine() + jmxPort1);
        createNodeSource(helper1, rmiPort1, NODES_NUMBER);

        String rmUrl2 = helper2.startRM(config2.getAbsolutePath(), rmiPort2,
                PAResourceManagerProperties.RM_JMX_PORT.getCmdLine() + jmxPort2);
        createNodeSource(helper2, rmiPort2, NODES_NUMBER);

        checkFreeNodes(helper1.getResourceManager(), NODES_NUMBER);
        checkFreeNodes(helper2.getResourceManager(), NODES_NUMBER);

        SchedulerTHelper.startScheduler(false, null, null, rmUrl1);

        Scheduler scheduler = SchedulerTHelper.getSchedulerInterface();

        CommunicationObject communicationObject1 = PAActiveObject.newActive(CommunicationObject.class,
                new Object[] {});
        String communicationObjectUrl1 = PAActiveObject.getUrl(communicationObject1);

        CommunicationObject communicationObject2 = PAActiveObject.newActive(CommunicationObject.class,
                new Object[] {});
        String communicationObjectUrl2 = PAActiveObject.getUrl(communicationObject2);

        System.out.println("Submit job1");
        JobId jobId1 = scheduler.submit(createJob(communicationObjectUrl1));
        SchedulerTHelper.waitForEventJobRunning(jobId1);

        System.out.println("Submit job2");
        JobId jobId2 = scheduler.submit(createJob(communicationObjectUrl2));
        SchedulerTHelper.waitForEventJobRunning(jobId2);

        System.out.println("Wait when nodes are acquired");
        helper1.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        helper1.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        checkFreeNodes(helper1.getResourceManager(), NODES_NUMBER - 2);

        System.out.println("Link another RM");
        if (!scheduler.linkResourceManager(rmUrl2)) {
            Assert.fail("Failed to link another RM");
        }

        System.out.println("Kill job1");
        scheduler.killJob(jobId1);
        SchedulerTHelper.waitForEventJobFinished(jobId1);
        helper1.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        checkFreeNodes(helper1.getResourceManager(), NODES_NUMBER - 1);

        System.out.println("Let first task of job2 finish");
        communicationObject2.setCommand("stop");
        SchedulerTHelper.waitForEventTaskFinished(jobId2, TASK_NAME1);
        helper1.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        checkFreeNodes(helper1.getResourceManager(), NODES_NUMBER);

        System.out.println("Wait when second task of job2 start");
        SchedulerTHelper.waitForEventTaskRunning(jobId2, TASK_NAME2);
        helper2.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        checkFreeNodes(helper2.getResourceManager(), NODES_NUMBER - 1);

        System.out.println("Let second task of job2 finish");
        communicationObject2.setCommand("stop");
        SchedulerTHelper.waitForEventJobFinished(jobId2);
        helper2.waitForAnyNodeEvent(RMEventType.NODE_STATE_CHANGED);
        checkFreeNodes(helper2.getResourceManager(), NODES_NUMBER);
    }

    private void checkFreeNodes(ResourceManager rm, int expectedNumber) {
        Assert.assertEquals("Unexpected number of free nodes", expectedNumber, rm.getState()
                .getFreeNodesNumber());
    }

}
