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
package org.ow2.proactive.scheduler.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.Map.Entry;

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.net.SocketAppender;
import org.apache.log4j.spi.LoggingEvent;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PAException;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.request.RequestFilter;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.remoteobject.RemoteObjectAdapter;
import org.objectweb.proactive.core.remoteobject.RemoteObjectExposer;
import org.objectweb.proactive.core.remoteobject.RemoteObjectHelper;
import org.objectweb.proactive.core.remoteobject.RemoteRemoteObject;
import org.objectweb.proactive.core.remoteobject.exception.UnknownProtocolException;
import org.objectweb.proactive.core.util.ProActiveInet;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.scheduler.common.AdminMethodsInterface;
import org.ow2.proactive.scheduler.common.SchedulerInitialState;
import org.ow2.proactive.scheduler.common.SchedulerState;
import org.ow2.proactive.scheduler.common.TaskTerminateNotification;
import org.ow2.proactive.scheduler.common.UserSchedulerInterface_;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobDescriptor;
import org.ow2.proactive.scheduler.common.job.JobEvent;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.JobType;
import org.ow2.proactive.scheduler.common.policy.Policy;
import org.ow2.proactive.scheduler.common.task.EligibleTaskDescriptor;
import org.ow2.proactive.scheduler.common.task.Log4JTaskLogs;
import org.ow2.proactive.scheduler.common.task.RestartMode;
import org.ow2.proactive.scheduler.common.task.SimpleTaskLogs;
import org.ow2.proactive.scheduler.common.task.TaskDescriptor;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskLogs;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.common.util.SchedulerLoggers;
import org.ow2.proactive.scheduler.common.util.Tools;
import org.ow2.proactive.scheduler.core.db.Condition;
import org.ow2.proactive.scheduler.core.db.ConditionComparator;
import org.ow2.proactive.scheduler.core.db.DatabaseManager;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.exception.RunningProcessException;
import org.ow2.proactive.scheduler.exception.StartProcessException;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.job.InternalJobWrapper;
import org.ow2.proactive.scheduler.job.JobEventImpl;
import org.ow2.proactive.scheduler.job.JobIdImpl;
import org.ow2.proactive.scheduler.job.JobResultImpl;
import org.ow2.proactive.scheduler.resourcemanager.ResourceManagerProxy;
import org.ow2.proactive.scheduler.task.JavaExecutableContainer;
import org.ow2.proactive.scheduler.task.ProActiveTaskLauncher;
import org.ow2.proactive.scheduler.task.TaskIdImpl;
import org.ow2.proactive.scheduler.task.TaskLauncher;
import org.ow2.proactive.scheduler.task.TaskResultImpl;
import org.ow2.proactive.scheduler.task.internal.InternalNativeTask;
import org.ow2.proactive.scheduler.task.internal.InternalTask;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.scheduler.util.classloading.TaskClassServer;
import org.ow2.proactive.scheduler.util.logforwarder.SimpleLoggerServer;
import org.ow2.proactive.scripting.SelectionScript;
import org.ow2.proactive.utils.NodeSet;


/**
 * <i><font size="2" color="#FF0000">** Scheduler core ** </font></i>
 * This is the main active object of the scheduler implementation,
 * it communicates with the entity manager to acquire nodes and with a policy
 * to insert and get jobs from the queue.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
public class SchedulerCore implements UserSchedulerInterface_, AdminMethodsInterface,
        TaskTerminateNotification, RunActive {

    /** Scheduler logger */
    public static final Logger logger = ProActiveLogger.getLogger(SchedulerLoggers.CORE);
    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.CORE);

    /** Scheduler main loop time out */
    private static final int SCHEDULER_TIME_OUT = PASchedulerProperties.SCHEDULER_TIME_OUT.getValueAsInt();

    /** Scheduler node ping frequency in second. */
    private static final long SCHEDULER_NODE_PING_FREQUENCY = PASchedulerProperties.SCHEDULER_NODE_PING_FREQUENCY
            .getValueAsInt() * 1000;

    /** Delay to wait for between getting a job result and removing the job concerned */
    private static final long SCHEDULER_REMOVED_JOB_DELAY = PASchedulerProperties.SCHEDULER_REMOVED_JOB_DELAY
            .getValueAsInt() * 1000;

    /** Host name of the scheduler for logger system. */
    private String host = null;

    /** Selected port for connection logger system */
    private int port;

    /** Implementation of Resource Manager */
    private ResourceManagerProxy resourceManager;

    /** Scheduler front-end. */
    private SchedulerFrontend frontend;

    /** Direct link to the current job to submit. */
    private InternalJobWrapper currentJobToSubmit;

    /** Scheduler current policy */
    private Policy policy;

    /** list of all jobs managed by the scheduler */
    private Map<JobId, InternalJob> jobs = new HashMap<JobId, InternalJob>();

    /** list of pending jobs among the managed jobs */
    private Vector<InternalJob> pendingJobs = new Vector<InternalJob>();

    /** list of running jobs among the managed jobs */
    private Vector<InternalJob> runningJobs = new Vector<InternalJob>();

    /** list of finished jobs among the managed jobs */
    private Vector<InternalJob> finishedJobs = new Vector<InternalJob>();

    /** Scheduler current state */
    private SchedulerState state = SchedulerState.STOPPED;

    /** Thread that will ping the running nodes */
    private Thread pinger;

    /** Timer used for remove result method (transient because Timer is not serializable)*/
    private transient Timer timer = new Timer();

    /** Jobs that must be logged into the corresponding appenders */
    private Hashtable<JobId, AsyncAppender> jobsToBeLogged = new Hashtable<JobId, AsyncAppender>();
    /** jobs that must be logged into a file */
    //TODO cdelbe : file are logged on core side...
    private Hashtable<JobId, FileAppender> jobsToBeLoggedinAFile = new Hashtable<JobId, FileAppender>();
    private static final String FILEAPPENDER_SUFFIX = "_FILE";

    /** Currently running tasks for a given jobId*/
    private Hashtable<JobId, Hashtable<TaskId, TaskLauncher>> currentlyRunningTasks = new Hashtable<JobId, Hashtable<TaskId, TaskLauncher>>();

    /** ClassLoading */
    // contains taskCLassServer for currently running jobs
    private static Hashtable<JobId, TaskClassServer> classServers = new Hashtable<JobId, TaskClassServer>();
    private static Hashtable<JobId, RemoteObjectExposer<TaskClassServer>> remoteClassServers = new Hashtable<JobId, RemoteObjectExposer<TaskClassServer>>();

    /**
     * Return the task classserver for the job jid
     * @param jid the job id 
     * @return the task classserver for the job jid
     */
    public static TaskClassServer getTaskClassServer(JobId jid) {
        return classServers.get(jid);
    }

    /**
     * Create a new taskClassServer for the job jid
     * @param jid the job id
     * @param userClasspathJarFile the contents of the classpath as a serialized jar file
     * @param deflateJar if true, the jar file is deflated in the tmpJarFilesDir
     */
    private static void addTaskClassServer(JobId jid, byte[] userClasspathJarFile, boolean deflateJar)
            throws SchedulerException {
        if (getTaskClassServer(jid) != null) {
            throw new SchedulerException("ClassServer already exists for job " + jid);
        }
        try {
            // create remote task classserver 
            logger_dev.info("Create remote task classServer on job '" + jid + "'");
            TaskClassServer localReference = new TaskClassServer(jid);
            RemoteObjectExposer<TaskClassServer> remoteExposer = new RemoteObjectExposer<TaskClassServer>(
                TaskClassServer.class.getName(), localReference);
            URI uri = RemoteObjectHelper.generateUrl(jid.toString());
            RemoteRemoteObject rro = remoteExposer.createRemoteObject(uri);
            // must activate through local ref to avoid copy of the classpath content !
            logger_dev.info("Active local reference");
            localReference.activate(userClasspathJarFile, deflateJar);
            // store references
            classServers.put(jid, (TaskClassServer) new RemoteObjectAdapter(rro).getObjectProxy());
            remoteClassServers.put(jid, remoteExposer);// stored to be unregistered later
        } catch (FileNotFoundException e) {
            logger_dev.error(e);
            throw new SchedulerException("Unable to create class server for job " + jid + " because " +
                e.getMessage());
        } catch (IOException e) {
            logger_dev.error(e);
            throw new SchedulerException("Unable to create class server for job " + jid + " because " +
                e.getMessage());
        } catch (UnknownProtocolException e) {
            logger_dev.error(e);
            throw new SchedulerException("Unable to create class server for job " + jid + " because " +
                e.getMessage());
        } catch (ProActiveException e) {
            logger_dev.error(e);
            throw new SchedulerException("Unable to create class server for job " + jid + " because " +
                e.getMessage());
        }
    }

    /**
     * Remove the taskClassServer for the job jid.
     * Delete the classpath associated in SchedulerCore.tmpJarFilesDir.
     * @return true if a taskClassServer has been removed, false otherwise.
     */
    private static boolean removeTaskClassServer(JobId jid) {
        logger_dev.info("Removing taskClassServer for Job '" + jid + "'");
        // desactivate tcs
        TaskClassServer tcs = classServers.remove(jid);
        if (tcs != null) {
            logger_dev.info("Desactivate taskClassServer for Job '" + jid + "'");
            tcs.desactivate();
        }
        // unexport remote object
        logger_dev.info("Removing remote ClassServer for Job '" + jid + "'");
        RemoteObjectExposer<TaskClassServer> roe = remoteClassServers.remove(jid);
        if (roe != null) {
            try {
                logger_dev.info("Unregister all (related to Job '" + jid + "'");
                roe.unregisterAll();
            } catch (ProActiveException e) {
                logger.error("Unable to unregister remote taskClassServer because : " + e);
                logger_dev.error(e);
            }
        }
        return (tcs != null);
    }

    /**
     * Terminate some job handling at the end of a job
     */
    private void terminateJobHandling(JobId jid) {
        //remove loggers
        logger_dev.info("Cleaning loggers for Job '" + jid + "'");
        Logger l = Logger.getLogger(Log4JTaskLogs.JOB_LOGGER_PREFIX + jid);
        l.removeAllAppenders();
        this.jobsToBeLogged.remove(jid);
        this.jobsToBeLoggedinAFile.remove(jid);
        // remove current running tasks
        // TODO cdelbe : When a job can be removed on failure ??
        // Other tasks' logs should remain available...
        this.currentlyRunningTasks.remove(jid);
        removeTaskClassServer(jid);
    }

    /**
     * ProActive empty constructor
     */
    public SchedulerCore() {
    }

    /**
     * Create a new scheduler Core with the given arguments.<br>
     * 
     * @param imp the resource manager on which the scheduler will interact.
     * @param frontend a reference to the frontend.
     * @param policyFullName the fully qualified name of the policy to be used.
     */
    public SchedulerCore(ResourceManagerProxy imp, SchedulerFrontend frontend, String policyFullName,
            InternalJobWrapper jobSubmitLink) {
        try {
            this.resourceManager = imp;
            this.frontend = frontend;
            this.currentJobToSubmit = jobSubmitLink;
            //logger
            host = ProActiveInet.getInstance().getInetAddress().getHostName();

            try {
                logger_dev.info("Create Simple logger server");
                // redirect event only into JobLogs
                SimpleLoggerServer slf = SimpleLoggerServer.createLoggerServer();
                this.port = slf.getPort();
            } catch (IOException e) {
                logger.error("Cannot create logger server : " + e.getMessage());
                logger_dev.error(e);
                throw new RuntimeException(e);
            }
            logger_dev.info("Instanciating policy : " + policyFullName);
            this.policy = (Policy) Class.forName(policyFullName).newInstance();
            logger.info("Scheduler Core ready !");
        } catch (InstantiationException e) {
            logger.error("The policy class cannot be found : " + e.getMessage());
            logger_dev.error(e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.error("The method cannot be accessed " + e.getMessage());
            logger_dev.error(e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            logger.error("The class definition cannot be found, it might be due to case sentivity : " +
                e.getMessage());
            logger_dev.error(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create the pinger thread to detect unActivity on nodes.
     */
    private void createPingThread() {
        logger_dev.debug("Creating thread that will ping down node");
        pinger = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(SCHEDULER_NODE_PING_FREQUENCY);

                        if (runningJobs.size() > 0) {
                            logger_dev.info("Ping deployed nodes (Number of running jobs : " +
                                runningJobs.size() + ")");
                            pingDeployedNodes();
                        }
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        logger_dev.info("Starting thread that will ping down node (ping frequency is : " +
            SCHEDULER_NODE_PING_FREQUENCY + "ms )");
        pinger.start();
    }

    /**
     * - Create the list of taskEvent containing in the given job.
     * - Clear the task event modify status. It is used to change all status of all tasks
     * with only one request. It has to be cleared after sending events.
     * - Send the change to the data base.
     *
     * @param currentJob the job where the task event are.
     */
    private void updateTaskEventsList(InternalJob currentJob) {
        Map<TaskId, TaskState> tsm = ((JobEventImpl) currentJob.getJobInfo()).getTaskStatusModify();
        for (Entry<TaskId, TaskState> e : tsm.entrySet()) {
            if (e.getValue() != TaskState.RUNNING) {
                DatabaseManager.synchronize(currentJob.getHMTasks().get(e.getKey()).getTaskInfo());
            }
        }
        // don't forget to set the task status modify to null
        currentJob.setTaskStatusModify(null);
        // used when a job has failed
        currentJob.setTaskFinishedTimeModify(null);
        // and to database
        DatabaseManager.synchronize(currentJob.getJobInfo());
    }

    /**
     * @see org.objectweb.proactive.RunActive#runActivity(org.objectweb.proactive.Body)
     */
    public void runActivity(Body body) {

        //Start DB and rebuild the scheduler if needed.
        recover();

        //Scheduler started
        ProActiveLogger.getLogger(SchedulerLoggers.CONSOLE).info(
                "Scheduler successfully created on " +
                    Tools.getHostURL(PAActiveObject.getActiveObjectNodeUrl(PAActiveObject.getStubOnThis())));

        if (state != SchedulerState.KILLED) {

            //listen log as immediate Service.
            //PAActiveObject.setImmediateService("listenLog");
            Service service = new Service(body);

            //used to read the enumerate schedulerState in order to know when submit is possible.
            //have to be immediate service
            body.setImmediateService("isSubmitPossible");
            body.setImmediateService("getTaskResult");
            body.setImmediateService("getJobResult");
            logger_dev.debug("Core immediate services : isSubmitPossible,getTaskResult,getJobResult");

            //set the filter for serveAll method (user action are privileged)
            RequestFilter filter = new MainLoopRequestFilter("submit", "terminate", "listenLog",
                "getSchedulerInitialState");
            createPingThread();

            // default scheduler state is started
            ((SchedulerCore) PAActiveObject.getStubOnThis()).start();

            do {
                service.blockingServeOldest();

                while ((state == SchedulerState.STARTED) || (state == SchedulerState.PAUSED)) {
                    try {
                        //block the loop until a method is invoked and serve it
                        service.blockingServeOldest(SCHEDULER_TIME_OUT);
                        //serve all important methods
                        service.serveAll(filter);
                        //schedule
                        schedule();
                    } catch (Exception e) {
                        //this point is reached in case of big problem, sometimes unknown
                        logger
                                .fatal("\nSchedulerCore.runActivity(MAIN_LOOP) caught an EXCEPTION - it will not terminate the body !");
                        logger_dev.error(e);
                        //trying to check if RM is alive
                        try {
                            logger_dev.error("Check if Resource Manager is alive");
                            resourceManager.isAlive();
                        } catch (Exception rme) {
                            logger_dev.error("Resource Manager seems to be dead");
                            resourceManager.shutdownProxy();
                            //if failed
                            freeze();
                            //scheduler functionality are reduced until now
                            state = SchedulerState.UNLINKED;
                            logger
                                    .fatal("******************************\n"
                                        + "Resource Manager is no more available, Scheduler has been paused waiting for a resource manager to be reconnect\n"
                                        + "Scheduler is in critical state and its functionalities are reduced : \n"
                                        + "\t-> use the linkResourceManager() method to reconnect a new one.\n"
                                        + "******************************");
                            frontend.schedulerRMDownEvent();
                        }
                    }
                }
            } while ((state != SchedulerState.SHUTTING_DOWN) && (state != SchedulerState.KILLED));

            logger.info("Scheduler is shutting down...");

            logger_dev.info("Unpause all jobs !");
            for (InternalJob job : jobs.values()) {
                if (job.getState() == JobState.PAUSED) {
                    job.setUnPause();

                    JobEvent event = job.getJobInfo();
                    updateTaskEventsList(job);
                    //send event to front_end
                    frontend.jobResumedEvent(event);
                }
            }

            //terminating jobs...
            if ((runningJobs.size() + pendingJobs.size()) > 0) {
                logger.info("Terminating jobs...");
            }

            while ((runningJobs.size() + pendingJobs.size()) > 0) {
                try {
                    service.serveAll(filter);
                    schedule();
                    //block the loop until a method is invoked and serve it
                    service.blockingServeOldest(SCHEDULER_TIME_OUT);
                } catch (Exception e) {
                    logger.debug(" ");
                    logger_dev.error(e);
                }
            }

            //stop the pinger thread.
            pinger.interrupt();
        }

        logger.info("Terminating...");
        //shutdown resource manager proxy
        resourceManager.shutdownProxy();
        logger_dev.info("Resource Manager proxy shutdown");

        if (state == SchedulerState.SHUTTING_DOWN) {
            frontend.schedulerShutDownEvent();
        }

        //destroying scheduler active objects
        frontend.terminate();
        //closing data base
        logger.debug("Closing Scheduler data base !");
        DatabaseManager.close();
        //terminate this active object
        PAActiveObject.terminateActiveObject(false);
        logger.info("Scheduler is now shutdown !");
        //exit
        System.exit(0);
    }

    /**
     * Schedule computing method
     */
    private void schedule() {
        //get job Descriptor list with eligible jobs (running and pending)
        ArrayList<JobDescriptor> jobDescriptorList = new ArrayList<JobDescriptor>();

        for (InternalJob j : runningJobs) {
            jobDescriptorList.add(j.getJobDescriptor());
        }

        //if scheduler is paused it only finishes running jobs
        if (state != SchedulerState.PAUSED) {
            for (InternalJob j : pendingJobs) {
                jobDescriptorList.add(j.getJobDescriptor());
            }
        }

        if (jobDescriptorList.size() > 0) {
            logger_dev.info("Number of jobs containing tasks to be scheduled : " + jobDescriptorList.size());
        }

        //ask the policy all the tasks to be schedule according to the jobs list.
        Vector<EligibleTaskDescriptor> taskRetrivedFromPolicy = policy.getOrderedTasks(jobDescriptorList);

        if (taskRetrivedFromPolicy == null) {
            logger_dev.info("No task to schedule");
            return;
        }

        if (taskRetrivedFromPolicy.size() > 0) {
            logger_dev.info("Number of tasks ready to be scheduled : " + taskRetrivedFromPolicy.size());
        }

        while (!taskRetrivedFromPolicy.isEmpty()) {
            int nbNodesToAskFor = 0;
            RMState rmState = resourceManager.getRMState();
            policy.RMState = rmState;
            int freeResourcesNb = rmState.getNumberOfFreeResources().intValue();
            logger_dev.info("Number of free resources : " + freeResourcesNb);
            if (freeResourcesNb == 0) {
                break;
            }
            int taskToCheck = 0;
            //select first task to define the selection script ID
            TaskDescriptor taskDescriptor = taskRetrivedFromPolicy.get(taskToCheck);
            InternalJob currentJob = jobs.get(taskDescriptor.getJobId());
            InternalTask internalTask = currentJob.getHMTasks().get(taskDescriptor.getId());
            InternalTask sentinel = internalTask;
            SelectionScript ss = internalTask.getSelectionScript();
            NodeSet ns = internalTask.getNodeExclusion();
            logger_dev.debug("Get the most nodes matching the current selection");
            //if free resources are available and (selection script ID and Node Exclusion) are the same as the first
            while (freeResourcesNb > 0 &&
                (ss == internalTask.getSelectionScript() || (ss != null && ss.equals(internalTask
                        .getSelectionScript()))) &&
                (ns == internalTask.getNodeExclusion() || (ns != null && ns.equals(internalTask
                        .getNodeExclusion())))) {
                //last task to be launched
                sentinel = internalTask;
                if (internalTask.getNumberOfNodesNeeded() > freeResourcesNb) {
                    //TODO what do we want for proActive job?
                    //Wait until enough resources are free or <<- chosen for the moment
                    //get the node until number of needed resources is reached?
                    break;
                } else {
                    //update number of nodes to ask to the RM
                    nbNodesToAskFor += internalTask.getNumberOfNodesNeeded();
                    freeResourcesNb -= internalTask.getNumberOfNodesNeeded();
                }
                //get next task
                taskToCheck++;
                //if there is no task anymore, break
                if (taskToCheck >= taskRetrivedFromPolicy.size()) {
                    break;
                }
                taskDescriptor = taskRetrivedFromPolicy.get(taskToCheck);
                currentJob = jobs.get(taskDescriptor.getJobId());
                internalTask = currentJob.getHMTasks().get(taskDescriptor.getId());
            }

            logger_dev.debug("Number of nodes to ask for : " + nbNodesToAskFor);
            NodeSet nodeSet = null;

            if (nbNodesToAskFor > 0) {
                logger.debug("Asking for " + nbNodesToAskFor + " node(s) with" +
                    ((ss == null) ? "out " : " ") + "verif script");

                //nodeSet = resourceManager.getAtMostNodes(nbNodesToAskFor, ss);
                nodeSet = resourceManager.getAtMostNodes(nbNodesToAskFor, ss, ns);

                logger.debug("Got " + nodeSet.size() + " node(s)");
            }
            if (nbNodesToAskFor <= 0 || nodeSet.size() == 0) {
                //if RM returns 0 nodes, i.e. no nodes satisfy selection script (or no nodes at all)
                //remove these tasks from the tasks list to Schedule, and then prevent infinite loop :
                //always trying to Schedule in vein these tasks (scheduler Core AO stay blocked on this Schedule loop,
                //and can't treat terminate request asked by ended tasks for example).
                //try again to schedule these tasks on next call to schedule() seems reasonable 
                while (!taskRetrivedFromPolicy.get(0).getId().equals(sentinel.getId())) {
                    taskRetrivedFromPolicy.remove(0);
                }
                taskRetrivedFromPolicy.remove(0);
            }

            Node node = null;

            try {
                while (nodeSet != null && !nodeSet.isEmpty()) {
                    taskDescriptor = taskRetrivedFromPolicy.get(0);
                    currentJob = jobs.get(taskDescriptor.getJobId());
                    internalTask = currentJob.getHMTasks().get(taskDescriptor.getId());

                    // load and Initialize the executable container
                    logger_dev.debug("Load and Initialize the executable container for task '" +
                        internalTask.getId() + "'");
                    DatabaseManager.load(internalTask);
                    internalTask.getExecutableContainer().init(currentJob, internalTask);

                    node = nodeSet.get(0);
                    TaskLauncher launcher = null;

                    //if the job is a ProActive job and if all nodes can be launched at the same time
                    if ((currentJob.getType() == JobType.PROACTIVE) &&
                        (nodeSet.size() >= internalTask.getNumberOfNodesNeeded())) {
                        nodeSet.remove(0);
                        launcher = internalTask.createLauncher(node);
                        this.currentlyRunningTasks.get(internalTask.getJobId()).put(internalTask.getId(),
                                launcher);
                        NodeSet nodes = new NodeSet();

                        for (int i = 0; i < (internalTask.getNumberOfNodesNeeded() - 1); i++) {
                            nodes.add(nodeSet.remove(0));
                        }
                        internalTask.getExecuterInformations().addNodes(nodes);

                        // activate loggers for this task if needed
                        if (this.jobsToBeLogged.containsKey(currentJob.getId()) ||
                            this.jobsToBeLoggedinAFile.containsKey(currentJob.getId())) {
                            launcher.activateLogs(host, port);
                        }
                        logger_dev.info("Starting deployment of task '" + internalTask.getName() +
                            "' for job '" + currentJob.getId() + "'");
                        ((JobResultImpl) currentJob.getJobResult()).storeFuturResult(internalTask.getName(),
                                ((ProActiveTaskLauncher) launcher).doTask((SchedulerCore) PAActiveObject
                                        .getStubOnThis(), (JavaExecutableContainer) internalTask
                                        .getExecutableContainer(), nodes));
                    } else if (currentJob.getType() != JobType.PROACTIVE) {
                        nodeSet.remove(0);
                        launcher = internalTask.createLauncher(node);
                        this.currentlyRunningTasks.get(internalTask.getJobId()).put(internalTask.getId(),
                                launcher);
                        // activate loggers for this task if needed
                        if (this.jobsToBeLogged.containsKey(currentJob.getId()) ||
                            this.jobsToBeLoggedinAFile.containsKey(currentJob.getId())) {
                            launcher.activateLogs(host, port);
                        }

                        //if job is TASKSFLOW, preparing the list of parameters for this task.
                        int resultSize = taskDescriptor.getParents().size();
                        if ((currentJob.getType() == JobType.TASKSFLOW) && (resultSize > 0)) {
                            TaskResult[] params = new TaskResult[resultSize];

                            for (int i = 0; i < resultSize; i++) {
                                //get parent task number i
                                InternalTask parentTask = currentJob.getHMTasks().get(
                                        taskDescriptor.getParents().get(i).getId());
                                //set the task result in the arguments array.
                                params[i] = currentJob.getJobResult().getResult(parentTask.getName());
                                //if this result has been unloaded, (extremely rare but possible)
                                if (params[i].getOutput() == null) {
                                    //get the result and load the content from database
                                    DatabaseManager.load(params[i]);
                                }
                            }
                            logger_dev.info("Starting deployment of task '" + internalTask.getName() +
                                "' for job '" + currentJob.getId() + "'");
                            //TODO if the next task is a native task, it's no need to pass params
                            ((JobResultImpl) currentJob.getJobResult()).storeFuturResult(internalTask
                                    .getName(), launcher.doTask((SchedulerCore) PAActiveObject
                                    .getStubOnThis(), internalTask.getExecutableContainer(), params));
                        } else {
                            logger_dev.info("Starting deployment of task '" + internalTask.getName() +
                                "' for job '" + currentJob.getId() + "'");
                            ((JobResultImpl) currentJob.getJobResult()).storeFuturResult(internalTask
                                    .getName(), launcher.doTask((SchedulerCore) PAActiveObject
                                    .getStubOnThis(), internalTask.getExecutableContainer()));
                        }
                    }

                    //if a task has been launched
                    if (launcher != null) {
                        logger.info("Task '" + internalTask.getId() + "' started on " +
                            node.getNodeInformation().getVMInformation().getHostName());

                        // set the different informations on job
                        if (currentJob.getStartTime() == -1) {
                            // if it is the first task of this job
                            currentJob.start();
                            pendingJobs.remove(currentJob);
                            runningJobs.add(currentJob);
                            //create tasks events list
                            updateTaskEventsList(currentJob);
                            logger.info("Job '" + currentJob.getId() + "' started");
                            // send job event to front-end
                            frontend.jobPendingToRunningEvent(currentJob.getJobInfo());
                        }

                        // set the different informations on task
                        currentJob.startTask(internalTask);
                        // send task event to front-end
                        frontend.taskPendingToRunningEvent(internalTask.getTaskInfo());
                        //no need to set this state in database
                    }
                    //if everything were OK (or if the task could not be launched, 
                    //removed this task from the processed task.
                    taskRetrivedFromPolicy.remove(0);
                    //if every task that should be launched have been removed
                    if (internalTask == sentinel) {
                        //get back unused nodes to the RManager
                        if (!nodeSet.isEmpty())
                            resourceManager.freeNodes(nodeSet);
                        //and leave the loop
                        break;
                    }
                }

            } catch (Exception e1) {
                logger_dev.debug(e1);
                //if we are here, it is that something append while launching the current task.
                logger.warn("Current node (" + node + ") has failed : " + e1.getMessage());
                //so try to get back the node to the resource manager
                try {
                    resourceManager.freeDownNode(internalTask.getExecuterInformations().getNodeName());
                } catch (Exception e2) {
                }
            }

        }

    }

    /**
     * Ping every nodes on which a task is currently running and repair the task if need.
     */
    private void pingDeployedNodes() {
        logger_dev.info("Search for down nodes !");

        for (int i = 0; i < runningJobs.size(); i++) {
            InternalJob job = runningJobs.get(i);

            for (InternalTask td : job.getTasks()) {
                if ((td.getStatus() == TaskState.RUNNING) &&
                    !PAActiveObject.pingActiveObject(td.getExecuterInformations().getLauncher())) {
                    logger_dev.info("Node failed on job '" + job.getId() + "', task '" + td.getId() + "'");

                    try {
                        String nodeName = td.getExecuterInformations().getNodeName();
                        logger_dev.info("Try to free this failed node : " + nodeName);
                        //free execution node even if it is dead
                        resourceManager.freeDownNode(nodeName);
                    } catch (Exception e) {
                        //just save the rest of the method execution
                    }

                    td.decreaseNumberOfExecutionOnFailureLeft();
                    logger_dev.info("Number of retry on Failure left for the task '" + td.getId() + "' : " +
                        td.getNumberOfExecutionOnFailureLeft());
                    if (td.getNumberOfExecutionOnFailureLeft() > 0) {
                        td.setStatus(TaskState.WAITING_ON_FAILURE);
                        job.newWaitingTask();
                        job.reStartTask(td);
                        frontend.taskWaitingForRestart(td.getTaskInfo());
                        logger_dev.info("Task '" + td.getId() + "' is waiting to restart");
                    } else {
                        endJob(
                                job,
                                td,
                                "An error has occurred due to a node failure and the maximum amout of retries property has been reached.",
                                JobState.FAILED);
                        i--;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Return true if a submit is possible, false if not.
     * 
     * @return true if a submit is possible, false if not.
     */
    public boolean isSubmitPossible() {
        return !((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.STOPPED));
    }

    /**
     * Submit a new job to the scheduler.
     *
     * @param job the job to be scheduled.
     * @throws SchedulerException
     */
    public void submit() throws SchedulerException {
        InternalJob job = currentJobToSubmit.getJob();
        logger_dev.info("Trying to submit new Job '" + job.getId() + "'");
        // TODO cdelbe : create classserver only when job is running ?
        // create taskClassLoader for this job
        if (job.getEnvironment().getJobClasspath() != null) {
            SchedulerCore.addTaskClassServer(job.getId(), job.getEnvironment().getJobClasspathContent(), job
                    .getEnvironment().containsJarFile());
            // if the classserver creation fails, the submit is aborted
        }

        job.submitAction();

        //create job result storage
        JobResult jobResult = new JobResultImpl(job.getId());
        //store the job result until user get it  (MUST BE SET BEFORE DB STORAGE)
        job.setJobResult(jobResult);

        //Add to data base
        DatabaseManager.register(job);

        //If register OK : add job to core
        jobs.put(job.getId(), job);
        pendingJobs.add(job);
        logger_dev.info("New job added to Scheduler lists : '" + job.getId() + "'");

        // create a running task table for this job
        this.currentlyRunningTasks.put(job.getId(), new Hashtable<TaskId, TaskLauncher>());

        //create appender for this job if required 
        if (job.getLogFile() != null) {
            logger_dev.info("Create logger for job '" + job.getId() + "'");
            Logger l = Logger.getLogger(Log4JTaskLogs.JOB_LOGGER_PREFIX + job.getId());
            l.setAdditivity(false);
            if (l.getAppender(Log4JTaskLogs.JOB_APPENDER_NAME + FILEAPPENDER_SUFFIX) == null) {
                try {
                    FileAppender fa = new FileAppender(Log4JTaskLogs.getTaskLogLayout(), job.getLogFile(),
                        false);
                    fa.setName(Log4JTaskLogs.JOB_APPENDER_NAME + FILEAPPENDER_SUFFIX);
                    l.addAppender(fa);
                    this.jobsToBeLoggedinAFile.put(job.getId(), fa);
                } catch (IOException e) {
                    logger.warn("Cannot open log file " + job.getLogFile() + " : " + e.getMessage());
                }
            }
        }

        //unload job environment that potentially contains classpath as byte[]
        DatabaseManager.unload(job.getEnvironment());
        //unload heavy object
        for (InternalTask it : job.getTasks()) {
            DatabaseManager.unload(it);
        }
        logger_dev.info("JobEnvironment and internalTask unloaded for job '" + job.getId() + "'");
    }

    /**
     * End the given job due to the given task failure.
     *
     * @param job the job to end.
     * @param task the task who has been the caused of failing. **This argument can be null only if jobState is killed**
     * @param errorMsg the error message to send in the task result.
     * @param jobState the type of the end for this job. (failed/canceled/killed)
     */
    private void endJob(InternalJob job, InternalTask task, String errorMsg, JobState jobState) {
        TaskResult taskResult = null;

        logger_dev.info("Job ending request for job '" + job.getId() + "' - cause by task '" + task.getId() +
            "' - state : " + jobState);

        for (InternalTask td : job.getTasks()) {
            if (td.getStatus() == TaskState.RUNNING) {
                //get the nodes that are used for this descriptor
                NodeSet nodes = td.getExecuterInformations().getNodes();

                //try to terminate the task
                try {
                    logger_dev.debug("Force terminating task '" + td.getId() + "'");
                    td.getExecuterInformations().getLauncher().terminate();
                } catch (Exception e) { /* (nothing to do) */
                }

                try {
                    //free every execution nodes
                    resourceManager.freeNodes(nodes, td.getCleaningScript());
                } catch (Exception e) {
                    try {
                        // try to get the node back to the IM
                        resourceManager.freeNodes(td.getExecuterInformations().getNodes());
                    } catch (Exception e1) {
                        resourceManager.freeDownNode(td.getExecuterInformations().getNodeName());
                    }
                }

                //If not killed
                if (jobState != JobState.KILLED) {
                    //deleting tasks results except the one that causes the error
                    if (!td.getId().equals(task.getId())) {
                        job.getJobResult().removeResult(td.getName());
                    }
                    //if canceled, get the result of the canceled task
                    if ((jobState == JobState.CANCELED) && td.getId().equals(task.getId())) {
                        taskResult = job.getJobResult().getResult(task.getName());
                    }
                }
            }
        }

        //if job has been killed
        if (jobState == JobState.KILLED) {
            job.failed(null, jobState);
            //the next line will try to remove job from each list.
            //once removed, it won't be removed from remaining list, but we ensure that the job is in only one of the list.
            if (runningJobs.remove(job) || pendingJobs.remove(job)) {
                finishedJobs.add(job);
            }
        } else {
            //failed the job
            job.failed(task.getId(), jobState);

            //store the exception into jobResult
            if (jobState == JobState.FAILED) {
                taskResult = new TaskResultImpl(task.getId(), new Throwable(errorMsg), new SimpleTaskLogs("",
                    errorMsg));
                ((JobResultImpl) job.getJobResult()).addTaskResult(task.getName(), taskResult, task
                        .isPreciousResult());
            } else if (jobState == JobState.CANCELED) {
                taskResult = (TaskResult) PAFuture.getFutureValue(taskResult);
                ((JobResultImpl) job.getJobResult()).addTaskResult(task.getName(), taskResult, task
                        .isPreciousResult());
            }

            //add the result in database
            DatabaseManager.update(job.getJobResult());
            //unload the result to improve memory usage
            DatabaseManager.unload(taskResult);
            //move the job
            runningJobs.remove(job);
            finishedJobs.add(job);

            //send task event
            frontend.taskRunningToFinishedEvent(task.getTaskInfo());
        }

        terminateJobHandling(job.getId());

        //create tasks events list
        updateTaskEventsList(job);

        logger.info("Job '" + job.getId() + "' terminated (" + jobState + ")");

        //send event to listeners.
        frontend.jobRunningToFinishedEvent(job.getJobInfo());
    }

    /**
     * Invoke by a task when it is about to finish.
     * This method can be invoke just a little amount of time before the result arrival.
     * That's why it can block the execution but only for short time.
     *
     * @param taskId the identification of the executed task.
     */
    public void terminate(TaskId taskId) {
        int nativeIntegerResult = 0;
        JobId jobId = taskId.getJobId();
        logger_dev.info("Received terminate task request for task '" + taskId + "' - job '" + jobId + "'");
        InternalJob job = jobs.get(jobId);

        //if job has been canceled or failed, it is possible that a task has finished just before
        //the failure of the job. In this rare case, the job may not exist anymore.
        if (job == null) {
            logger_dev.info("Job '" + jobId + "' does not exist anymore");
            return;
        }

        InternalTask descriptor = job.getHMTasks().get(taskId);
        // job might have already been removed if job has failed...
        Hashtable<TaskId, TaskLauncher> runningTasks = this.currentlyRunningTasks.get(jobId);
        if (runningTasks != null) {
            runningTasks.remove(taskId);
        } else {
            return;
        }
        try {
            //first unload the executable container that we don't need until next execution (if re-execution)
            DatabaseManager.unload(descriptor);
            //The task is terminated but it's possible to have to
            //wait for the future of the task result (TaskResult).
            //accessing to the taskResult could block current execution but for a very little time.
            //it is the time between the end of the task and the arrival of the future from the task.
            //
            //check if the task result future has an error due to node death.
            //if the node has died, a runtimeException is sent instead of the result
            TaskResult res = null;
            res = ((JobResultImpl) job.getJobResult()).getAnyResult(descriptor.getName());
            //unwrap future
            res = (TaskResult) PAFuture.getFutureValue(res);
            logger_dev.info("Task '" + taskId + "' futur result unwrapped");

            updateJobIdReference(job.getJobResult(), res);

            if (res != null) {
                // HANDLE DESCIPTORS
                res.setPreviewerClassName(descriptor.getResultPreview());
                res.setJobClasspath(job.getEnvironment().getJobClasspath()); // can be null
                if (PAException.isException(res)) {
                    //in this case, it is a node error. (should never come)
                    //this is not user exception or usage,
                    //so we restart independently of user or admin execution property
                    logger_dev.info("Node failed on job '" + jobId + "', task '" + taskId + "'");
                    //change status and update GUI
                    descriptor.setStatus(TaskState.WAITING_ON_FAILURE);
                    job.newWaitingTask();
                    frontend.taskWaitingForRestart(descriptor.getTaskInfo());
                    job.reStartTask(descriptor);
                    //update job and task events
                    DatabaseManager.synchronize(job.getJobInfo());
                    DatabaseManager.synchronize(descriptor.getTaskInfo());
                    //free execution node even if it is dead
                    resourceManager.freeDownNode(descriptor.getExecuterInformations().getNodeName());
                    return;
                }
            }

            logger.info("Task '" + taskId + "' on job '" + jobId + "' terminated");

            //Check if an exception or error occurred during task execution...
            boolean errorOccurred = false;
            if (descriptor instanceof InternalNativeTask) {
                logger_dev.debug("Terminated task '" + taskId + "' is a native task");
                try {
                    // try to get the result, res.value can throw an exception,
                    // it means that the process has failed before the end.
                    nativeIntegerResult = ((Integer) res.value());
                    // an error occurred if res is not 0
                    errorOccurred = (nativeIntegerResult != 0);
                } catch (RunningProcessException rpe) {
                    //if res.value throws a RunningProcessException, user is not responsible
                    //change status and update GUI
                    descriptor.setStatus(TaskState.WAITING_ON_FAILURE);
                    job.newWaitingTask();
                    frontend.taskWaitingForRestart(descriptor.getTaskInfo());
                    job.reStartTask(descriptor);
                    //update job and task events
                    DatabaseManager.synchronize(job.getJobInfo());
                    DatabaseManager.synchronize(descriptor.getTaskInfo());
                    //free execution node even if it is dead
                    resourceManager.freeNodes(descriptor.getExecuterInformations().getNodes(), descriptor
                            .getCleaningScript());
                    return;
                } catch (StartProcessException spe) {
                    //if res.value throws a StartProcessException, it can be due to an IOException thrown by the process
                    //ie:command not found
                    //just note that an error occurred.
                    errorOccurred = true;
                } catch (Throwable e) {
                    //in any other case, note that an error occurred but the user must be informed.
                    errorOccurred = true;
                }
            } else {
                logger_dev.debug("Terminated task '" + taskId + "' is a java task");
                errorOccurred = res.hadException();
            }

            logger_dev.info("Task '" + taskId + "' terminate with" + (errorOccurred ? "" : "out") + " error");

            //if an error occurred
            if (errorOccurred) {
                //the task threw an exception OR the result is an error code (1-255)
                //if the task has to restart
                descriptor.decreaseNumberOfExecutionLeft();
                //check the number of execution left and fail the job if it is cancelOnError
                if (descriptor.getNumberOfExecutionLeft() <= 0 && descriptor.isCancelJobOnError()) {
                    //if no rerun left, failed the job
                    endJob(job, descriptor,
                            "An error occurred in your task and the maximum number of executions has been reached. "
                                + "You also ask to cancel the job in such a situation !", JobState.CANCELED);
                    return;
                }
                if (descriptor.getNumberOfExecutionLeft() > 0) {
                    if (descriptor.getRestartTaskOnError().equals(RestartMode.ELSEWHERE)) {
                        //if the task restart ELSEWHERE
                        descriptor.setNodeExclusion(descriptor.getExecuterInformations().getNodes());
                    }
                    try {
                        resourceManager.freeNodes(descriptor.getExecuterInformations().getNodes(), descriptor
                                .getCleaningScript());
                    } catch (Exception e) {
                        //cannot get back the node, RM take care about that.
                    }
                    //change status and update GUI
                    descriptor.setStatus(TaskState.WAITING_ON_ERROR);
                    job.newWaitingTask();
                    //store this task result in the job result.
                    ((JobResultImpl) job.getJobResult()).addTaskResult(descriptor.getName(), res, descriptor
                            .isPreciousResult());
                    //and update database
                    //update job and task events
                    DatabaseManager.synchronize(job.getJobInfo());
                    DatabaseManager.synchronize(descriptor.getTaskInfo());
                    DatabaseManager.update(job.getJobResult());
                    //send event to user
                    frontend.taskWaitingForRestart(descriptor.getTaskInfo());

                    //the job is not restarted directly
                    RestartJobTimerTask jtt = new RestartJobTimerTask(job, descriptor);
                    new Timer().schedule(jtt, job.getNextWaitingTime(descriptor.getMaxNumberOfExecution() -
                        descriptor.getNumberOfExecutionLeft()));

                    return;
                }
            }

            //to be done before terminating the task, once terminated it is not running anymore..
            TaskDescriptor currentTD = job.getRunningTaskDescriptor(taskId);
            descriptor = job.terminateTask(errorOccurred, taskId);
            //store this task result in the job result.
            ((JobResultImpl) job.getJobResult()).addTaskResult(descriptor.getName(), res, descriptor
                    .isPreciousResult());
            logger_dev.info("TaskResult added to job '" + job.getId() + "' - task name is '" +
                descriptor.getName() + "'");
            //and update database
            DatabaseManager.synchronize(job.getJobInfo());
            DatabaseManager.synchronize(descriptor.getTaskInfo());
            DatabaseManager.update(job.getJobResult());

            //clean the result to improve memory usage
            if (!job.getJobDescriptor().hasChildren(descriptor.getId())) {
                DatabaseManager.unload(res);
            }
            for (TaskDescriptor td : currentTD.getParents()) {
                if (td.getChildrenCount() == 0) {
                    DatabaseManager.unload(job.getJobResult().getResult(td.getId().getReadableName()));
                }
            }
            //send event
            frontend.taskRunningToFinishedEvent(descriptor.getTaskInfo());

            //if this job is finished (every task have finished)
            logger_dev.info("Number of finished tasks : " + job.getNumberOfFinishedTask() +
                " - Number of tasks : " + job.getTotalNumberOfTasks());
            if (job.getNumberOfFinishedTask() == job.getTotalNumberOfTasks()) {
                //terminating job
                job.terminate();
                runningJobs.remove(job);
                finishedJobs.add(job);
                logger.info("Job '" + jobId + "' terminated");

                terminateJobHandling(job.getId());

                //and to data base
                DatabaseManager.synchronize(job.getJobInfo());
                //clean every task result
                for (TaskResult tr : job.getJobResult().getAllResults().values()) {
                    DatabaseManager.unload(tr);
                }
                //send event to client
                frontend.jobRunningToFinishedEvent(job.getJobInfo());
            }

            //free every execution nodes
            resourceManager.freeNodes(descriptor.getExecuterInformations().getNodes(), descriptor
                    .getCleaningScript());
        } catch (NullPointerException eNull) {
            //the task has been killed. Nothing to do anymore with this one.
        }
    }

    /**
     * For Hibernate use : a Hibernate session cannot accept two different java objects with the same
     * Hibernate identifier.
     * To avoid this duplicate object (due to serialization),
     * this method will join JobId references in the Job result graph object.
     *
     * @param jobResult the result in which to join cross dependences
     * @param res the current result to check. (avoid searching for any)
     */
    private void updateJobIdReference(JobResult jobResult, TaskResult res) {
        try {
            logger_dev.info("jobResult : " + jobResult.getJobId() + " - taskResult : " + res.getTaskId());
            //find the jobId field
            for (Field f : TaskIdImpl.class.getDeclaredFields()) {
                if (f.getType().equals(JobId.class)) {
                    f.setAccessible(true);
                    //set to the existing reference
                    f.set(res.getTaskId(), jobResult.getJobId());
                    break;
                }
            }
        } catch (Exception e) {
            logger_dev.error(e);
        }
    }

    /**
     * Return the scheduler current state with the pending, running, finished jobs list.
     *
     * @return the scheduler current state with the pending, running, finished jobs list.
     */
    public SchedulerInitialState<? extends Job> getSchedulerInitialState() {
        SchedulerInitialState<InternalJob> sState = new SchedulerInitialState<InternalJob>();
        sState.setPendingJobs(pendingJobs);
        sState.setRunningJobs(runningJobs);
        sState.setFinishedJobs(finishedJobs);
        sState.setState(state);
        return sState;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#listenLog(org.ow2.proactive.scheduler.common.job.JobId, java.lang.String, int)
     */
    public void listenLog(JobId jobId, String hostname, int logPort) {
        logger_dev.info("listen logs of job '" + jobId + "'");
        AsyncAppender bufferForJobId = this.jobsToBeLogged.get(jobId);
        Logger l = null;
        if (bufferForJobId == null) {
            // can be not null if a log file has been defined for this job
            // or created by previous call to listenLog
            bufferForJobId = new AsyncAppender();
            bufferForJobId.setName(Log4JTaskLogs.JOB_APPENDER_NAME);
            this.jobsToBeLogged.put(jobId, bufferForJobId);
            l = Logger.getLogger(Log4JTaskLogs.JOB_LOGGER_PREFIX + jobId);
            l.setAdditivity(false);
            l.addAppender(bufferForJobId);
        }
        SocketAppender socketToListener = new SocketAppender(hostname, logPort);
        // should add the socket appender before activating logs on running tasks !
        bufferForJobId.addAppender(socketToListener);
        InternalJob target = this.jobs.get(jobId);
        if ((target != null) && !this.pendingJobs.contains(target)) {
            // this jobs contains running and finished tasks
            // for running tasks, activate loggers on taskLauncher side
            Hashtable<TaskId, TaskLauncher> curRunning = this.currentlyRunningTasks.get(jobId);
            // for finished tasks, add logs events "manually"
            Collection<TaskResult> allRes = target.getJobResult().getAllResults().values();
            for (TaskResult tr : allRes) {
                // if taskResult is not awaited, task is terminated
                TaskLogs logs = null;
                // try to look in the DB
                DatabaseManager.load(tr);

                logs = tr.getOutput();

                // avoid race condition if any...
                if (logs == null) {
                    // the logs has been deleted and stored in the DB during the previous getOutput
                    // should not be null now !
                    DatabaseManager.load(tr);
                    logs = tr.getOutput();
                }

                // TODO cdelbe : ok, cmathieu, I know it's ugly. I'll fix it asap. Need more genericity for logging mechanism.
                if (logs instanceof Log4JTaskLogs) {
                    for (LoggingEvent le : ((Log4JTaskLogs) logs).getAllEvents()) {
                        // write into socket appender directly to avoid double lines on other listeners
                        socketToListener.doAppend(le);
                    }
                } else {
                    l.info(logs.getStdoutLogs(false));
                    l.error(logs.getStderrLogs(false));
                }
            }
            // for running tasks
            if (curRunning != null) {
                for (TaskId tid : curRunning.keySet()) {
                    TaskLauncher tl = curRunning.get(tid);
                    tl.activateLogs(this.host, this.port);
                }
            }
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#getJobResult(org.ow2.proactive.scheduler.common.job.JobId)
     */
    public JobResult getJobResult(JobId jobId) throws SchedulerException {
        final InternalJob job = jobs.get(jobId);
        final SchedulerCore schedulerStub = (SchedulerCore) PAActiveObject.getStubOnThis();

        if (job == null) {
            throw new SchedulerException("The job does not exist !");
        }

        logger_dev.info("Trying to get JobResult of job '" + jobId + "'");
        //result = null if not in DB (ie: not yet available)
        JobResult result = DatabaseManager.recover(job.getJobResult().getClass(),
                new Condition("id", ConditionComparator.EQUALS_TO, job.getJobResult().getJobId())).get(0);

        if (!job.getJobInfo().isToBeRemoved() && SCHEDULER_REMOVED_JOB_DELAY > 0) {

            //remember that this job is to be removed
            job.setToBeRemoved();
            DatabaseManager.synchronize(job.getJobInfo());

            try {
                //remove job after the given delay
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        schedulerStub.remove(job.getId());
                    }
                };
                timer.schedule(tt, SCHEDULER_REMOVED_JOB_DELAY);
                logger_dev.info("Job '" + jobId + "' will be removed in " +
                    (SCHEDULER_REMOVED_JOB_DELAY / 1000) + "sec");
            } catch (Exception e) {
                logger_dev.error(e);
            }
        }

        return result;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#getTaskResult(org.ow2.proactive.scheduler.common.job.JobId, java.lang.String)
     */
    public TaskResult getTaskResult(JobId jobId, String taskName) throws SchedulerException {
        logger_dev.info("Trying to get TaskResult of task '" + taskName + "' for job '" + jobId + "'");
        InternalJob job = jobs.get(jobId);

        if (job == null) {
            logger_dev.info("Job '" + jobId + "' does not exist");
            throw new SchedulerException("The job does not exist !");
        }

        //extract taskResult reference from memory (weak instance)
        //useful to get the task result with the task name
        TaskResult result = ((JobResultImpl) job.getJobResult()).getResult(taskName);
        if (result == null) {
            //the task is unknown
            logger_dev.info("Task '" + taskName + "' does not exist");
            throw new SchedulerException("The task does not exist !");
        }
        if (PAFuture.isAwaited(result)) {
            //the result is not yet available
            logger_dev.info("Task '" + taskName + "' is running");
            return null;
        }
        //extract full taskResult from DB
        //use the previous result to get the task Id matching the given name.
        //extract full copy from DB to avoid load, unload operation
        result = DatabaseManager.recover(result.getClass(),
                new Condition("id", ConditionComparator.EQUALS_TO, result.getTaskId())).get(0);

        if ((result != null)) {
            logger_dev.info("Get '" + taskName + "' task result for job '" + jobId + "'");
        }

        return result;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#remove(org.ow2.proactive.scheduler.common.job.JobId)
     */
    public void remove(JobId jobId) {
        InternalJob job = jobs.get(jobId);

        logger_dev.info("Request to remove job '" + jobId + "'");

        if (job != null && finishedJobs.contains(job)) {
            jobs.remove(jobId);
            job.setRemovedTime(System.currentTimeMillis());
            finishedJobs.remove(job);
            //and to data base
            DatabaseManager.synchronize(job.getJobInfo());
            // close log buffer
            AsyncAppender jobLog = this.jobsToBeLogged.remove(jobId);
            if (jobLog != null) {
                jobLog.close();
            }
            FileAppender jobFile = this.jobsToBeLoggedinAFile.remove(jobId);
            if (jobFile != null) {
                jobFile.close();
            }
            //remove from DataBase
            boolean rfdb = PASchedulerProperties.JOB_REMOVE_FROM_DB.getValueAsBoolean();
            logger_dev.info("Remove job '" + jobId + "' also from  dataBase : " + rfdb);
            if (rfdb) {
                DatabaseManager.delete(job);
            }
            logger.info("Job " + jobId + " removed !");
            //send event to front-end
            frontend.jobRemoveFinishedEvent(job.getJobInfo());
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#start()
     */
    public BooleanWrapper start() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if (state != SchedulerState.STOPPED) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.STARTED;
        logger.info("Scheduler has just been started !");
        frontend.schedulerStartedEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#stop()
     */
    public BooleanWrapper stop() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.STOPPED) || (state == SchedulerState.SHUTTING_DOWN) ||
            (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.STOPPED;
        logger.info("Scheduler has just been stopped, no tasks will be launched until start.");
        frontend.schedulerStoppedEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#pause()
     */
    public BooleanWrapper pause() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        if ((state != SchedulerState.FROZEN) && (state != SchedulerState.STARTED)) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.PAUSED;
        logger.info("Scheduler has just been paused !");
        frontend.schedulerPausedEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#freeze()
     */
    public BooleanWrapper freeze() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        if ((state != SchedulerState.PAUSED) && (state != SchedulerState.STARTED)) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.FROZEN;
        logger.info("Scheduler has just been frozen !");
        frontend.schedulerFrozenEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#resume()
     */
    public BooleanWrapper resume() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        if ((state != SchedulerState.PAUSED) && (state != SchedulerState.FROZEN)) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.STARTED;
        logger.info("Scheduler has just been resumed !");
        frontend.schedulerResumedEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#shutdown()
     */
    public BooleanWrapper shutdown() {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.KILLED) || (state == SchedulerState.SHUTTING_DOWN)) {
            return new BooleanWrapper(false);
        }

        state = SchedulerState.SHUTTING_DOWN;
        logger.info("Scheduler is shutting down, this make take time to finish every jobs !");
        frontend.schedulerShuttingDownEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#kill()
     */
    public synchronized BooleanWrapper kill() {
        if (state == SchedulerState.KILLED) {
            return new BooleanWrapper(false);
        }

        logger_dev.info("Killing all running task processes...");
        //destroying running active object launcher
        for (InternalJob j : runningJobs) {
            for (InternalTask td : j.getTasks()) {
                if (td.getStatus() == TaskState.RUNNING) {
                    try {
                        NodeSet nodes = td.getExecuterInformations().getNodes();

                        try {
                            td.getExecuterInformations().getLauncher().terminate();
                        } catch (Exception e) { /* Tested, nothing to do */
                        }

                        try {
                            resourceManager.freeNodes(nodes, td.getCleaningScript());
                        } catch (Exception e) {
                            try {
                                // try to get the node back to the IM
                                resourceManager.freeNodes(td.getExecuterInformations().getNodes());
                            } catch (Exception e1) {
                                resourceManager.freeDownNode(td.getExecuterInformations().getNodeName());
                            }
                        }
                    } catch (Exception e) {
                        //do nothing, the task is already terminated.
                    }
                }
            }
        }

        logger_dev.info("Cleaning all lists...");
        //cleaning all lists
        jobs.clear();
        pendingJobs.clear();
        runningJobs.clear();
        finishedJobs.clear();
        jobsToBeLogged.clear();
        jobsToBeLoggedinAFile.clear();
        currentlyRunningTasks.clear();
        //finally : shutdown
        state = SchedulerState.KILLED;
        logger.info("Scheduler has just been killed !");
        frontend.schedulerKilledEvent();

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#pause(org.ow2.proactive.scheduler.common.job.JobId)
     */
    public BooleanWrapper pause(JobId jobId) {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        InternalJob job = jobs.get(jobId);

        if (finishedJobs.contains(job)) {
            return new BooleanWrapper(false);
        }

        boolean change = job.setPaused();
        JobEvent event = job.getJobInfo();

        if (change) {
            logger.debug("Job " + jobId + " has just been paused !");
        }

        //create tasks events list
        updateTaskEventsList(job);
        //send event to user
        frontend.jobPausedEvent(event);

        return new BooleanWrapper(change);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#resume(org.ow2.proactive.scheduler.common.job.JobId)
     */
    public BooleanWrapper resume(JobId jobId) {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if ((state == SchedulerState.SHUTTING_DOWN) || (state == SchedulerState.KILLED)) {
            return new BooleanWrapper(false);
        }

        InternalJob job = jobs.get(jobId);

        if (finishedJobs.contains(job)) {
            return new BooleanWrapper(false);
        }

        boolean change = job.setUnPause();
        JobEvent event = job.getJobInfo();

        if (change) {
            logger.debug("Job " + jobId + " has just been resumed !");
        }

        //create tasks events list
        updateTaskEventsList(job);
        //send event to user
        frontend.jobResumedEvent(event);

        return new BooleanWrapper(change);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#kill(org.ow2.proactive.scheduler.common.job.JobId)
     */
    public synchronized BooleanWrapper kill(JobId jobId) {
        if (state == SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }

        if (state == SchedulerState.KILLED) {
            return new BooleanWrapper(false);
        }

        logger_dev.info("Request sent to kill job '" + jobId + "'");

        InternalJob job = jobs.get(jobId);

        if (job == null || job.getState() == JobState.KILLED) {
            return new BooleanWrapper(false);
        }

        endJob(job, null, "", JobState.KILLED);

        terminateJobHandling(job.getId());

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.UserSchedulerInterface_#changePriority(org.ow2.proactive.scheduler.common.job.JobId, org.ow2.proactive.scheduler.common.job.JobPriority)
     */
    public void changePriority(JobId jobId, JobPriority priority) {
        logger_dev
                .info("Request sent to change priority on job '" + jobId + "' - new priority : " + priority);
        InternalJob job = jobs.get(jobId);
        job.setPriority(priority);
        DatabaseManager.synchronize(job.getJobInfo());
        frontend.jobChangePriorityEvent(job.getJobInfo());
    }

    /**
     * Change the policy of the scheduler.<br>
     * This method will immediately change the policy and so the whole scheduling process.
     *
     * @param newPolicyFile the new policy file as a string.
     * @return true if the policy has been correctly change, false if not.
     * @throws SchedulerException (can be due to insufficient permission)
     */
    public BooleanWrapper changePolicy(Class<? extends Policy> newPolicyFile) throws SchedulerException {
        try {
            policy = newPolicyFile.newInstance();
            frontend.schedulerPolicyChangedEvent(newPolicyFile.getName());
            logger_dev.info("New policy changed ! new policy name : " + newPolicyFile.getName());
        } catch (InstantiationException e) {
            logger_dev.error(e);
            throw new SchedulerException("Exception occurs while instanciating the policy !");
        } catch (IllegalAccessException e) {
            logger_dev.error(e);
            throw new SchedulerException("Exception occurs while accessing the policy !");
        }

        return new BooleanWrapper(true);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.AdminMethodsInterface#linkResourceManager(java.lang.String)
     */
    public BooleanWrapper linkResourceManager(String rmURL) throws SchedulerException {
        //only if unlink
        if (state != SchedulerState.UNLINKED) {
            return new BooleanWrapper(false);
        }
        try {
            ResourceManagerProxy imp = ResourceManagerProxy.getProxy(new URI(rmURL.trim()));
            //re-link the RM
            resourceManager = imp;
            state = SchedulerState.FROZEN;
            logger
                    .info("New resource manager has been linked to the scheduler.\n\t-> Resume to continue the scheduling.");
            frontend.schedulerRMUpEvent();
            return new BooleanWrapper(true);
        } catch (Exception e) {
            throw new SchedulerException("Error while connecting the new Resource Manager !", e);
        }
    }

    /**
     * Rebuild the scheduler after a crash.
     * Get data base instance, connect it and ask if a rebuild is needed.
     * The steps to recover the core are visible below.
     */
    private void recover() {
        try {
            //Start Hibernate
            logger.info("Starting Hibernate...");
            DatabaseManager.build();
            logger.info("Hibernate successfully started !");
        } catch (Exception e) {
            //if the database doesn't exist
            logger.error("*********  ERROR ********** " + e.getMessage());
            logger_dev.error(e);
            kill();
            return;
        }

        //create condition of recovering : recover only non-removed job
        //Condition condition = new Condition("jobInfo.removedTime", ConditionComparator.LESS_EQUALS_THAN,(long) 0);
        //list of internal job to recover
        //List<InternalJob> recovering = DatabaseManager.recover(InternalJob.class, condition);
        List<InternalJob> recovering = DatabaseManager.recoverAllJobs();

        logger_dev.info("Number of job to recover : " + recovering.size());

        if (recovering.size() == 0) {
            logger_dev.info("No Job to recover.");
            frontend.recover(null);
            return;
        }

        // Recover the scheduler core
        //------------------------------------------------------------------------
        //----------------------    Re-build jobs lists  -------------------------
        //------------------------------------------------------------------------
        logger.info("Re-build jobs lists");

        JobId maxId = JobIdImpl.makeJobId("0");

        for (InternalJob job : recovering) {
            jobs.put(job.getId(), job);

            //search last JobId
            if (job.getId().compareTo(maxId) > 0) {
                maxId = job.getId();
            }
        }

        //------------------------------------------------------------------------
        //--------------------    Initialize jobId count   ----------------------
        //------------------------------------------------------------------------
        logger_dev.info("Initialize jobId count");
        JobIdImpl.setInitialValue((JobIdImpl) maxId);

        //------------------------------------------------------------------------
        //-----------    Re-build pending/running/finished lists  ----------------
        //------------------------------------------------------------------------
        logger_dev.info("Re-build jobs pending/running/finished lists");

        for (InternalJob job : jobs.values()) {
            //rebuild job descriptor if needed (needed because not stored in database)
            job.getJobDescriptor();
            switch (job.getState()) {
                case PENDING:
                    pendingJobs.add(job);
                    currentlyRunningTasks.put(job.getId(), new Hashtable<TaskId, TaskLauncher>());
                    // restart classserver if needed
                    if (job.getEnvironment().getJobClasspath() != null) {
                        try {
                            SchedulerCore.addTaskClassServer(job.getId(), job.getEnvironment()
                                    .getJobClasspathContent(), job.getEnvironment().containsJarFile());
                        } catch (SchedulerException e) {
                            // TODO cdelbe : exception handling ?
                            logger_dev.error(e);
                        }
                    }
                    break;
                case STALLED:
                case RUNNING:
                    runningJobs.add(job);
                    currentlyRunningTasks.put(job.getId(), new Hashtable<TaskId, TaskLauncher>());

                    //reset the finished events in the order they have occurred
                    ArrayList<InternalTask> tasksList = copyAndSort(job.getTasks(), true);

                    for (InternalTask task : tasksList) {
                        job.update(task.getTaskInfo());
                        //if the task was in waiting for restart state, restart it
                        if (task.getStatus() == TaskState.WAITING_ON_ERROR ||
                            task.getStatus() == TaskState.WAITING_ON_FAILURE) {
                            job.newWaitingTask();
                            job.reStartTask(task);
                        }
                    }

                    // restart classServer if needed
                    if (job.getEnvironment().getJobClasspath() != null) {
                        try {
                            SchedulerCore.addTaskClassServer(job.getId(), job.getEnvironment()
                                    .getJobClasspathContent(), job.getEnvironment().containsJarFile());
                        } catch (SchedulerException e) {
                            // TODO cdelbe : exception handling ?
                            logger_dev.error(e);
                        }
                    }

                    break;
                case FINISHED:
                case CANCELED:
                case FAILED:
                case KILLED:
                    finishedJobs.add(job);
                    break;
                case PAUSED:
                    if ((job.getNumberOfPendingTask() + job.getNumberOfRunningTask() + job
                            .getNumberOfFinishedTask()) == 0) {
                        pendingJobs.add(job);
                    } else {
                        runningJobs.add(job);

                        //reset the finished events in the order they have occurred
                        ArrayList<InternalTask> tasksListP = copyAndSort(job.getTasks(), true);

                        for (InternalTask task : tasksListP) {
                            job.update(task.getTaskInfo());
                        }
                    }
                    // restart classserver if needed
                    if (job.getEnvironment().getJobClasspath() != null) {
                        try {
                            SchedulerCore.addTaskClassServer(job.getId(), job.getEnvironment()
                                    .getJobClasspathContent(), job.getEnvironment().containsJarFile());
                        } catch (SchedulerException e) {
                            // TODO cdelbe : exception handling ?
                            logger_dev.error(e);
                        }
                    }
            }
            //unload job environment once handled
            DatabaseManager.unload(job.getEnvironment());
        }

        //------------------------------------------------------------------------
        //------------------    Re-create task dependences   ---------------------
        //------------------------------------------------------------------------
        logger_dev.info("Re-create task dependences");

        for (InternalJob job : runningJobs) {
            ArrayList<InternalTask> tasksList = copyAndSort(job.getTasks(), true);

            //simulate the running execution to recreate the tree.
            for (InternalTask task : tasksList) {
                job.simulateStartAndTerminate(task.getId());
            }

            if ((job.getState() == JobState.RUNNING) || (job.getState() == JobState.PAUSED)) {
                //set the state to stalled because the scheduler start in stopped mode.
                if (job.getState() == JobState.RUNNING) {
                    job.setState(JobState.STALLED);
                }

                //set the task to pause inside the job if it is paused.
                if (job.getState() == JobState.PAUSED) {
                    job.setState(JobState.STALLED);
                    job.setPaused();
                    job.setTaskStatusModify(null);
                }

                //update the count of pending and running task.
                job.setNumberOfPendingTasks(job.getNumberOfPendingTask() + job.getNumberOfRunningTask());
                job.setNumberOfRunningTasks(0);
            }
        }

        for (InternalJob job : pendingJobs) {
            //set the task to pause inside the job if it is paused.
            if (job.getState() == JobState.PAUSED) {
                job.setState(JobState.STALLED);
                job.setPaused();
                job.setTaskStatusModify(null);
            }
        }

        //------------------------------------------------------------------------
        //---------    Removed non-managed jobs (result has been sent)   ---------
        //----    Set remove waiting time to job where result has been sent   ----
        //------------------------------------------------------------------------
        logger_dev.info("Removing non-managed jobs");
        Iterator<InternalJob> iterJob = jobs.values().iterator();

        final SchedulerCore schedulerStub = (SchedulerCore) PAActiveObject.getStubOnThis();
        while (iterJob.hasNext()) {
            final InternalJob job = iterJob.next();
            //re-set job removed delay (if job result has been sent to user)
            if (job.isToBeRemoved()) {
                if (SCHEDULER_REMOVED_JOB_DELAY > 0) {
                    try {
                        //remove job after the given delay
                        TimerTask tt = new TimerTask() {
                            @Override
                            public void run() {
                                schedulerStub.remove(job.getId());
                            }
                        };
                        timer.schedule(tt, SCHEDULER_REMOVED_JOB_DELAY);
                        logger.debug("Job " + job.getId() + " will be removed in " +
                            (SCHEDULER_REMOVED_JOB_DELAY / 1000) + "sec");
                    } catch (Exception e) {
                    }
                }
            }
        }

        //------------------------------------------------------------------------
        //-----------------    Recover the scheduler front-end   -----------------
        //------------------------------------------------------------------------
        logger.debug("Recover the scheduler front-end");

        frontend.recover(jobs);

    }

    /**
     * Make a copy of the given argument with the restriction 'onlyFinished'.
     * Then sort the array according to finished time order.
     *
     * @param tasks the list of internal tasks to copy.
     * @param onlyFinished true if the copy must contains only the finished task,
     *                                                 false to contains every tasks.
     * @return the sorted copy of the given argument.
     */
    private ArrayList<InternalTask> copyAndSort(ArrayList<InternalTask> tasks, boolean onlyFinished) {
        ArrayList<InternalTask> tasksList = new ArrayList<InternalTask>();

        //copy the list with only the finished task.
        for (InternalTask task : tasks) {
            if (onlyFinished) {
                switch (task.getStatus()) {
                    case ABORTED:
                    case CANCELED:
                    case FAILED:
                    case FINISHED:
                    case FAULTY:
                        tasksList.add(task);
                }
            } else {
                tasksList.add(task);
            }
        }
        //sort the finished task according to their finish time.
        //to be sure to be in the right tree browsing.
        Collections.sort(tasksList, new FinishTimeComparator());

        return tasksList;
    }

    /**
     * FinishTimeComparator will compare the internal task on their finished time.
     *
     * @author The ProActive Team
     * @date 25 oct. 07
     *
     */
    private static class FinishTimeComparator implements Comparator<InternalTask> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         * @param o1 First InternalTask to be compared.
         * @param o2 Second InternalTask to be compared with the first.
         * @return a negative integer, zero, or a positive integer as the
         * 	       first argument is less than, equal to, or greater than the
         *	       second. 
         */
        public int compare(InternalTask o1, InternalTask o2) {
            return (int) (o1.getFinishedTime() - o2.getFinishedTime());
        }
    }

}
