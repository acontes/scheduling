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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.scheduler.ext.matlab.embedded;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.*;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.body.request.RequestFilter;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.UserSchedulerInterface;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.UserException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.job.TaskFlowJob;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.util.SchedulerLoggers;
import org.ow2.proactive.scheduler.ext.matlab.exception.MatlabTaskException;
import org.ow2.proactive.scripting.InvalidScriptException;
import org.ow2.proactive.scripting.SelectionScript;

import ptolemy.data.Token;


/**
 * This active object handles the connection between Matlab and the Scheduler directly from the Matlab environment
 *
 * @author The ProActive Team
 */
public class AOMatlabEnvironment implements Serializable, SchedulerEventListener, InitActive, RunActive {

    /**
     * URL to the scheduler
     */
    private String schedulerUrl;

    /**
     * Connection to the scheduler
     */
    private UserSchedulerInterface scheduler;

    /**
     * Id of the current jobs running
     */
    private HashMap<JobId, MatlabJobInfo> currentJobIds = new HashMap<JobId, MatlabJobInfo>();
    /**
     * job id of the last submitted job (useful for runactivity method)
     */
    private JobId lastSubJobId;

    private JobId waitAllResultsJobID;

    /**
     * Internal job id for job description only
     */
    private long lastGenJobId = 0;

    /**
     * Id of the last task created + 1
     */
    private long lastTaskId = 0;

    /**
     * log4j logger
     */
    protected static Logger logger = ProActiveLogger.getLogger(SchedulerLoggers.MATLAB);
    private static final boolean debug = logger.isDebugEnabled();
    private boolean debugCurrentJob;

    /**
     * Proactive stub on this AO
     */
    private AOMatlabEnvironment stubOnThis;

    /**
     * Is the AO terminated ?
     */
    private boolean terminated;

    private boolean schedulerStopped = false;

    private SchedulerAuthenticationInterface auth;

    /**
     * Constructs the environment AO
     */
    public AOMatlabEnvironment() {

    }

    /**
     * Trys to log into the scheduler, using the provided user and password
     *
     * @param user   username
     * @param passwd password
     * @throws LoginException     if the login fails
     * @throws SchedulerException if an other error occurs
     */
    public void login(String user, String passwd) throws LoginException, SchedulerException {

        this.scheduler = auth.logAsUser(user, passwd);

        this.scheduler.addSchedulerEventListener((AOMatlabEnvironment) stubOnThis, false,
                SchedulerEvent.JOB_RUNNING_TO_FINISHED, SchedulerEvent.KILLED, SchedulerEvent.SHUTDOWN,
                SchedulerEvent.SHUTTING_DOWN);

    }

    /**
     * Tells if we are connected to the scheduler or not
     *
     * @return answer
     */
    public boolean isConnected() {
        return this.scheduler != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.proactive.InitActive#initActivity(org.objectweb.proactive.Body)
     */
    @SuppressWarnings("unchecked")
    public void initActivity(Body body) {
        stubOnThis = (AOMatlabEnvironment) PAActiveObject.getStubOnThis();
    }

    /**
     * Request to join the scheduler at the given url
     *
     * @param url url of the scheduler
     * @return true if success, false otherwise
     */
    public boolean join(String url) {
        try {
            auth = SchedulerConnection.join(url);
        } catch (Exception e1) {
            System.err.println(e1.getMessage());
            return false;
        }
        this.schedulerUrl = url;
        return true;
    }

    /**
     * Returns all the results in an array or throw a RuntimeException in case of error
     *
     * @return array of ptolemy tokens
     */
    public ArrayList<Token> waitAllResults() {
        ArrayList<Token> answer = null;
        MatlabJobInfo jinfo = currentJobIds.get(waitAllResultsJobID);
        if (debugCurrentJob) {
            System.out.println("[AOMatlabEnvironment] Sending the results of job " +
                waitAllResultsJobID.value() + " back...");
        }
        try {
            if (schedulerStopped) {
                System.err.println("[AOMatlabEnvironment] The scheduler has been stopped");
                answer = new ArrayList<Token>();
            } else if (jinfo.getStatus() == JobStatus.KILLED || jinfo.getStatus() == JobStatus.CANCELED) {
                // Job killed 
                System.err.println("[AOMatlabEnvironment] The job has been killed");
                answer = new ArrayList<Token>();
            } else if (jinfo.getErrorToThrow() != null) {
                // Error inside job
                if (jinfo.getErrorToThrow() instanceof MatlabTaskException) {
                    System.err.println(jinfo.getErrorToThrow().getMessage());
                    answer = new ArrayList<Token>();
                } else {

                    throw new RuntimeException(jinfo.getErrorToThrow());
                }
            } else {
                // Normal termination
                answer = new ArrayList<Token>(jinfo.getResults());
            }
        } finally {
            currentJobIds.remove(waitAllResultsJobID);
        }

        return answer;
    }

    /**
     * Submit a new bunch of tasks to the scheduler, throws a runtime exception if a job is currently running
     *
     * @param inputScripts input scripts (scripts executed before the main one)
     * @param mainScripts  main scripts
     * @param priority     priority of the job
     */
    public ArrayList<Token> solve(String[] inputScripts, String[] mainScripts, URL scriptURL,
            JobPriority priority, boolean debug) {
        debugCurrentJob = debug;

        if (schedulerStopped) {
            System.err.println("[AOMatlabEnvironment] the Scheduler is stopped");
            return new ArrayList<Token>();
        }
        // We store the script selecting the nodes to use it later at termination.

        if (debugCurrentJob) {
            System.out.println("[AOMatlabEnvironment] Submitting job of " + mainScripts.length + " tasks...");
        }

        // We verify that the script is available (otherwise we just ignore it)
        URL availableScript = null;
        try {
            InputStream is = scriptURL.openStream();
            is.close();
            availableScript = scriptURL;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Creating a task flow job
        TaskFlowJob job = new TaskFlowJob();
        job.setName("Matlab Environment Job " + lastGenJobId++);
        job.setPriority(priority);
        job.setCancelJobOnError(true);
        job.setDescription("Set of parallel matlab tasks");
        for (int i = 0; i < mainScripts.length; i++) {

            JavaTask schedulerTask = new JavaTask();

            schedulerTask.setName("" + lastTaskId++);
            schedulerTask.setPreciousResult(true);
            schedulerTask.addArgument("input", inputScripts[i]);
            schedulerTask.addArgument("script", mainScripts[i]);
            schedulerTask.setDescription(mainScripts[i]);
            if (debugCurrentJob) {
                schedulerTask.addArgument("debug", "true");
            }
            schedulerTask.setExecutableClassName("org.ow2.proactive.scheduler.ext.matlab.MatlabTask");
            if (availableScript != null) {
                SelectionScript sscript = null;
                try {
                    sscript = new SelectionScript(availableScript, new String[] {}, true);
                } catch (InvalidScriptException e1) {
                    throw new RuntimeException(e1);
                }
                schedulerTask.setSelectionScript(sscript);
            }

            try {
                job.addTask(schedulerTask);
            } catch (UserException e) {
                e.printStackTrace();
            }

        }

        try {
            lastSubJobId = scheduler.submit(job);
            if (debugCurrentJob) {
                System.out.println("[AOMatlabEnvironment] Job " + lastSubJobId.value() + " submitted.");
            }
            currentJobIds.put(lastSubJobId, new MatlabJobInfo());

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        // The last call puts a method in the RequestQueue 
        // that won't be executed until all the results are received (see runactivity)
        return stubOnThis.waitAllResults();
    }

    /**
     * terminate
     */
    public void terminate() {
        this.terminated = true;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#schedulerStateUpdatedEvent(org.ow2.proactive.scheduler.common.SchedulerEvent)
     */
    public void schedulerStateUpdatedEvent(SchedulerEvent eventType) {
        switch (eventType) {
            case KILLED:
            case SHUTDOWN:
            case SHUTTING_DOWN:
            case STOPPED:
                if (debug) {
                    logger.debug("Received " + eventType.toString() + " event");
                }
                schedulerStopped = true;
                break;
        }

    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobStateUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void jobStateUpdatedEvent(NotificationData<JobInfo> notification) {
        switch (notification.getEventType()) {
            case JOB_RUNNING_TO_FINISHED:
                JobInfo info = notification.getData();
                if (info.getStatus() == JobStatus.KILLED) {
                    if (debugCurrentJob) {
                        System.out.println("Received job killed event...");
                    }

                    // Filtering the right job
                    if (!currentJobIds.containsKey(info.getJobId())) {
                        return;
                    }
                    currentJobIds.get(info.getJobId()).setStatus(info.getStatus());
                    currentJobIds.get(info.getJobId()).setJobFinished(true);
                } else {
                    if (debugCurrentJob) {
                        System.out.println("Received job finished event...");
                    }

                    if (info == null) {
                        return;
                    }

                    // Filtering the right job
                    if (!currentJobIds.containsKey(info.getJobId())) {
                        return;
                    }
                    // Getting the Job result from the Scheduler
                    JobResult jResult = null;

                    try {
                        jResult = scheduler.getJobResult(info.getJobId());
                    } catch (SchedulerException e) {
                        jobDidNotSucceed(info.getJobId(), e, true, null);
                        return;
                    }

                    if (debugCurrentJob) {
                        System.out.println("[AOMatlabEnvironment] Updating results of job: " +
                            jResult.getName() + "(" + info.getJobId() + ")");
                    }

                    // Geting the task results from the job result
                    Map<String, TaskResult> task_results = null;
                    if (jResult.hadException()) {
                        task_results = jResult.getExceptionResults();
                    } else {
                        // sorted results

                        task_results = jResult.getAllResults();
                    }
                    ArrayList<Integer> keys = new ArrayList<Integer>();
                    for (String key : task_results.keySet()) {
                        keys.add(Integer.parseInt(key));
                    }
                    Collections.sort(keys);
                    // Iterating over the task results
                    ArrayList<Token> results = new ArrayList<Token>();
                    for (Integer key : keys) {
                        TaskResult res = task_results.get("" + key);
                        if (debugCurrentJob) {
                            System.out.println("[AOMatlabEnvironment] Looking for result of task: " + key);
                        }

                        // No result received
                        if (res == null) {
                            jobDidNotSucceed(info.getJobId(), new RuntimeException("Task id = " + key +
                                " was not returned by the scheduler"), false, null);

                        } else {

                            String logs = res.getOutput().getAllLogs(false);
                            if (res.hadException()) {

                                //Exception took place inside the framework
                                if (res.getException() instanceof ptolemy.kernel.util.IllegalActionException) {
                                    // We filter this specific exception which means that the "out" variable was not set by the function 
                                    // due to an error inside the script or a missing licence 

                                    jobDidNotSucceed(info.getJobId(), new MatlabTaskException(logs), false,
                                            logs);
                                } else {
                                    // For other types of exception we forward it as it is.
                                    jobDidNotSucceed(info.getJobId(), res.getException(), true, logs);
                                }
                            } else {
                                // Normal success

                                Token computedResult = null;
                                try {
                                    computedResult = (Token) res.value();
                                    results.add(computedResult);
                                    // We print the logs of the job, if any
                                    if (logs.length() > 0) {
                                        System.out.println(logs);
                                    }
                                } catch (ptolemy.kernel.util.IllegalActionException e1) {
                                    jobDidNotSucceed(info.getJobId(), new MatlabTaskException(logs), false,
                                            logs);
                                } catch (Throwable e2) {
                                    jobDidNotSucceed(info.getJobId(), e2, true, logs);
                                }
                            }
                        }
                    }
                    MatlabJobInfo jinfo = currentJobIds.get(info.getJobId());
                    jinfo.setResults(results);
                    jinfo.setStatus(info.getStatus());
                    jinfo.setJobFinished(true);
                }
                break;
        }

    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#jobSubmittedEvent(org.ow2.proactive.scheduler.common.job.JobState)
     */
    public void jobSubmittedEvent(JobState job) {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#taskStateUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void taskStateUpdatedEvent(NotificationData<TaskInfo> notification) {
        // TODO Auto-generated method stub
    }

    /**
     * @see org.ow2.proactive.scheduler.common.SchedulerEventListener#usersUpdatedEvent(org.ow2.proactive.scheduler.common.NotificationData)
     */
    public void usersUpdatedEvent(NotificationData<UserIdentification> notification) {
        // TODO Auto-generated method stub
    }

    /**
     * Handles case of an unsuccessful job
     *
     * @param jobId      id of the job
     * @param ex         exception thrown
     * @param printStack do we print the stack trace ?
     * @param logs       logs of the task creating the problem
     */
    private void jobDidNotSucceed(JobId jobId, Throwable ex, boolean printStack, String logs) {
        System.err.println("Job did not succeed");
        if (logs.length() > 0) {
            System.out.println(logs);
        }
        if (printStack) {
            ex.printStackTrace();
        }
        MatlabJobInfo jinfo = currentJobIds.get(jobId);
        jinfo.setErrorToThrow(ex);
        jinfo.setJobFinished(true);
    }

    /**
     * {@inheritDoc}
     */
    public void runActivity(final Body body) {
        Service service = new Service(body);
        while (!terminated) {
            try {

                service.waitForRequest();
                if (debug) {
                    logger.debug("Request received");
                }
                // We detect a waitXXX request in the request queue
                Request waitRequest = service.getOldest("waitAllResults");
                if (waitRequest != null) {

                    // if there is one request we remove it and store it for later
                    // we look at the last submitted job id
                    currentJobIds.get(lastSubJobId).setPendingRequest(waitRequest);
                    if (debugCurrentJob) {
                        System.out.println("[AOMatlabEnvironment] Removed waitAllResults " + lastSubJobId +
                            " request from the queue");
                    }
                    service.blockingRemoveOldest("waitAllResults");
                    //Request submitRequest = buildRequest(body);
                    //service.serve(submitRequest);
                }

                // we serve everything else which is not a waitXXX method
                // Careful, the order is very important here, we need to serve the solve method before the waitXXX
                service.serveOldest(new FindNotWaitFilter());

                // we maybe serve the pending waitXXX method if there is one and if the necessary results are collected
                maybeServePending(service);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        // we clear the service to avoid dirty pending requests 
        service.flushAll();
        // we block the communications because a getTask request might still be coming from a worker created just before the master termination
        body.blockCommunication();
        // we finally terminate the master
        body.terminate();
    }

    /**
     * If there is a pending waitXXX method, we serve it if the necessary results are collected
     *
     * @param service
     */
    protected void maybeServePending(Service service) {
        if (!currentJobIds.isEmpty()) {
            HashMap<JobId, MatlabJobInfo> clonedJobIds = new HashMap<JobId, MatlabJobInfo>(currentJobIds);
            for (Map.Entry<JobId, MatlabJobInfo> entry : clonedJobIds.entrySet()) {
                if (entry.getValue().isJobFinished() || schedulerStopped) {
                    if (debugCurrentJob) {
                        System.out.println("[AOMatlabEnvironment] serving waitAllResults " + entry.getKey());
                    }
                    servePending(service, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Serve the pending waitXXX method
     *
     * @param service
     */
    protected void servePending(Service service, JobId jid, MatlabJobInfo jinfo) {
        Request req = jinfo.getPendingRequest();
        waitAllResultsJobID = jid;
        jinfo.setPendingRequest(null);
        service.serve(req);
    }

    /**
     * @author The ProActive Team
     *         Internal class for filtering requests in the queue
     */
    protected class FindNotWaitFilter implements RequestFilter {

        /**
         * Creates the filter
         */
        public FindNotWaitFilter() {
        }

        /**
         * {@inheritDoc}
         */
        public boolean acceptRequest(final Request request) {
            // We find all the requests which can't be served yet
            String name = request.getMethodName();
            if (name.equals("waitAllResults")) {
                return false;
            }
            return true;
        }
    }

}
