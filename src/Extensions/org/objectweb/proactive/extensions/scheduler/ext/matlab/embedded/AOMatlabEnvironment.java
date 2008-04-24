package org.objectweb.proactive.extensions.scheduler.ext.matlab.embedded;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.Body;
import org.objectweb.proactive.InitActive;
import org.objectweb.proactive.RunActive;
import org.objectweb.proactive.Service;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.body.request.Request;
import org.objectweb.proactive.core.body.request.RequestFilter;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.scheduler.common.exception.SchedulerException;
import org.objectweb.proactive.extensions.scheduler.common.exception.UserException;
import org.objectweb.proactive.extensions.scheduler.common.job.Job;
import org.objectweb.proactive.extensions.scheduler.common.job.JobEvent;
import org.objectweb.proactive.extensions.scheduler.common.job.JobId;
import org.objectweb.proactive.extensions.scheduler.common.job.JobPriority;
import org.objectweb.proactive.extensions.scheduler.common.job.JobResult;
import org.objectweb.proactive.extensions.scheduler.common.job.TaskFlowJob;
import org.objectweb.proactive.extensions.scheduler.common.job.UserIdentification;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.SchedulerAuthenticationInterface;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.SchedulerConnection;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.SchedulerEvent;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.SchedulerEventListener;
import org.objectweb.proactive.extensions.scheduler.common.scheduler.UserSchedulerInterface;
import org.objectweb.proactive.extensions.scheduler.common.scripting.InvalidScriptException;
import org.objectweb.proactive.extensions.scheduler.common.scripting.SelectionScript;
import org.objectweb.proactive.extensions.scheduler.common.task.JavaTask;
import org.objectweb.proactive.extensions.scheduler.common.task.TaskEvent;
import org.objectweb.proactive.extensions.scheduler.common.task.TaskResult;
import org.objectweb.proactive.extensions.scheduler.ext.matlab.SimpleMatlab;
import org.objectweb.proactive.extensions.scheduler.ext.matlab.exception.MatlabTaskException;

import ptolemy.data.Token;


/**
 * This active object handles the connection between Matlab and the Scheduler directly from the Matlab environment
 * @author The ProActive Team
 *
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
     * Id of the current job running
     */
    private JobId currentJobId = null;

    /**
     * Internal job id for job description only
     */
    private long lastJobId = 0;

    /**
     * Id of the last task created + 1 
     */
    private long lastTaskId = 0;

    /**
     * Results gathered
     */

    private TreeMap<String, Token> results;

    /**
     * WaitAllResults Request waiting to be served
     */
    protected Request pendingRequest;

    /**
     * log4j logger 
     */
    protected static Logger logger = ProActiveLogger.getLogger(Loggers.SCHEDULER_MATLAB_EXT);

    /**
     * Proactive stub on this AO
     */
    private AOMatlabEnvironment stubOnThis;

    /**
     * Is the AO terminated ?
     */
    private boolean terminated;

    /**
     * Is the current job finished ?
     */
    private boolean isJobFinished;

    private boolean jobKilled = false;
    private boolean schedulerStopped = false;

    /**
     * Exception to throw in case of error
     */
    private Throwable errorToThrow;
    private SchedulerAuthenticationInterface auth;

    private static final int number_of_task_to_finish = 100;
    private URL scriptUsedURL = null;

    /**
     * ProActive no arg constructor
     */
    @Deprecated
    public AOMatlabEnvironment() {

    }

    /**
     * Creates a connection to the scheduler
     * @param schedulerUrl url of the scheduler
     * @param user username
     * @param passwd password
     */
    public AOMatlabEnvironment(String schedulerUrl) {
        this.schedulerUrl = schedulerUrl;
    }

    /**
     * Trys to log into the scheduler, using the provided user and password
     * @param user username
     * @param passwd password
     * @throws LoginException if the login fails
     * @throws SchedulerException if an other error occurs
     */
    public void login(String user, String passwd) throws LoginException, SchedulerException {

        this.scheduler = auth.logAsUser(user, passwd);

        this.scheduler.addSchedulerEventListener((AOMatlabEnvironment) stubOnThis, SchedulerEvent.JOB_KILLED,
                SchedulerEvent.JOB_RUNNING_TO_FINISHED, SchedulerEvent.KILLED, SchedulerEvent.SHUTDOWN,
                SchedulerEvent.SHUTTING_DOWN);

    }

    /**
     * Tells if we are connected to the scheduler or not
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
        results = new TreeMap<String, Token>();

        try {
            auth = SchedulerConnection.join(schedulerUrl);
        } catch (SchedulerException e1) {
            e1.printStackTrace();
            terminated = true;
            return;
        }

    }

    /**
     * Returns all the results in an array or throw a RuntimeException in case of error
     * @return array of ptolemy tokens
     */
    public ArrayList<Token> waitAllResults() {
        ArrayList<Token> answer = null;
        if (logger.isDebugEnabled()) {
            System.out.println("Sending the results back...");
        }

        if (schedulerStopped) {
            System.err.println("The scheduler has been stopped");
            answer = new ArrayList<Token>();
        } else if (jobKilled) {
            // Job killed 
            System.err.println("The job has been killed");
            answer = new ArrayList<Token>();
        } else if (errorToThrow != null) {
            // Error inside job
            if (errorToThrow instanceof MatlabTaskException) {
                System.err.println(errorToThrow.getMessage());
                answer = new ArrayList<Token>();
            } else {
                results.clear();
                currentJobId = null;
                jobKilled = false;
                throw new RuntimeException(errorToThrow);
            }
        } else {
            // Normal termination
            answer = new ArrayList<Token>(results.values());
        }

        results.clear();
        currentJobId = null;
        jobKilled = false;
        return answer;
    }

    /**
     * Submit a new bunch of tasks to the scheduler, throws a runtime exception if a job is currently running
     * @param tasks tasks to solve
     * @param priority priority of the job
     */
    public ArrayList<Token> solve(SimpleMatlab[] tasks, URL scriptURL, JobPriority priority) {
        if (schedulerStopped) {
            System.err.println("The Scheduler is stopped");
            return new ArrayList<Token>();
        }
        // We store the script selecting the nodes to use it later at termination.

        if ((scriptUsedURL == null) && (scriptURL != null)) {
            scriptUsedURL = scriptURL;
        }
        if (currentJobId != null) {
            throw new RuntimeException("The Scheduler is already busy with one job");
        }

        if (logger.isDebugEnabled()) {
            System.out.println("Submitting job of " + tasks.length + " tasks...");
        }

        // Creating a task flow job
        TaskFlowJob job = new TaskFlowJob();
        job.setName("Matlab Environment Job " + lastJobId++);
        job.setPriority(priority);
        job.setCancelOnException(true);
        job.setDescription("Set of parallel matlab tasks");
        // We removed the external log files as the output is forwarded into Matlab directly,
        // uncomment to have them back.
        job.setLogFile("Matlab_job_log_" + lastJobId + ".txt");
        for (SimpleMatlab task : tasks) {

            JavaTask schedulerTask = new JavaTask();

            schedulerTask.setName("" + lastTaskId++);
            schedulerTask.setPreciousResult(true);
            schedulerTask.setTaskInstance(task);
            SelectionScript sscript = null;
            try {
                sscript = new SelectionScript(scriptURL, null, true);
            } catch (InvalidScriptException e1) {
                throw new RuntimeException(e1);
            }
            schedulerTask.setSelectionScript(sscript);

            try {
                job.addTask(schedulerTask);
            } catch (UserException e) {
                e.printStackTrace();
            }

        }

        try {
            currentJobId = scheduler.submit(job);
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        this.isJobFinished = false;
        this.jobKilled = false;
        this.errorToThrow = null;
        // The last call puts a method in the RequestQueue 
        // that won't be executed until all the results are received (see runactivity)
        return stubOnThis.waitAllResults();
    }

    public void jobChangePriorityEvent(JobEvent event) {
        // TODO Auto-generated method stub

    }

    public void jobKilledEvent(JobId jobId) {
        if (logger.isDebugEnabled()) {
            System.out.println("Received job killed event...");
        }

        // Filtering the right job
        if ((currentJobId == null) || !jobId.equals(currentJobId)) {
            return;
        }
        this.jobKilled = true;

    }

    public void jobPausedEvent(JobEvent event) {
        // TODO Auto-generated method stub

    }

    public void jobPendingToRunningEvent(JobEvent event) {
        // TODO Auto-generated method stub

    }

    public void jobRemoveFinishedEvent(JobEvent event) {
        // TODO Auto-generated method stub

    }

    public void jobResumedEvent(JobEvent event) {
        // TODO Auto-generated method stub

    }

    public void jobRunningToFinishedEvent(JobEvent event) {
        if (logger.isDebugEnabled()) {
            System.out.println("Received job finished event...");
        }

        if (event == null) {
            return;
        }

        // Filtering the right job
        if (!event.getJobId().equals(currentJobId)) {
            return;
        }
        // Getting the Job result from the Scheduler
        JobResult jResult = null;

        try {
            jResult = scheduler.getJobResult(event.getJobId());
        } catch (SchedulerException e) {
            jobDidNotSucceed(event.getJobId(), e, true, null);
            return;
        }

        if (logger.isDebugEnabled()) {
            System.out
                    .println("Updating results of job: " + jResult.getName() + "(" + event.getJobId() + ")");
        }

        // Geting the task results from the job result
        HashMap<String, TaskResult> task_results = null;
        if (jResult.hadException()) {
            task_results = jResult.getExceptionResults();
        } else {
            task_results = jResult.getAllResults();
        }

        // Iterating over the task results
        for (Map.Entry<String, TaskResult> res : task_results.entrySet()) {
            if (logger.isDebugEnabled()) {
                System.out.println("Looking for result of task: " + res.getKey());
            }

            // No result received
            if (res.getValue() == null) {
                jobDidNotSucceed(event.getJobId(), new RuntimeException("Task id = " + res.getKey() +
                    " was not returned by the scheduler"), false, null);

            } else if (res.getValue().hadException()) {
                //Exception took place inside the framework
                if (res.getValue().getException() instanceof ptolemy.kernel.util.IllegalActionException) {
                    // We filter this specific exception which means that the "out" variable was not set by the function 
                    // due to an error inside the script or a missing licence 
                    String logs = res.getValue().getOuput().getAllLogs(false);
                    jobDidNotSucceed(event.getJobId(), new MatlabTaskException(logs), false, logs);
                } else {
                    // For other types of exception we forward it as it is.
                    jobDidNotSucceed(event.getJobId(), res.getValue().getException(), true, res.getValue()
                            .getOuput().getAllLogs(false));
                }
            } else {
                // Normal success
                Token computedResult = null;
                String logs = null;
                try {
                    logs = res.getValue().getOuput().getAllLogs(false);
                    computedResult = (Token) res.getValue().value();
                    results.put(res.getKey(), computedResult);
                    // We print the logs of the job, if any
                    if (logs.length() > 0) {
                        System.out.println(logs);
                    }
                } catch (ptolemy.kernel.util.IllegalActionException e1) {
                    jobDidNotSucceed(event.getJobId(), new MatlabTaskException(logs), false, logs);
                } catch (Throwable e2) {
                    jobDidNotSucceed(event.getJobId(), e2, true, logs);
                }
            }
        }

        isJobFinished = true;

    }

    public void jobSubmittedEvent(Job job) {
        // TODO Auto-generated method stub

    }

    public void schedulerImmediatePausedEvent() {
        // TODO Auto-generated method stub

    }

    public void schedulerKilledEvent() {
        if (logger.isDebugEnabled()) {
            System.out.println("Received Scheduler killed event");
        }
        schedulerStopped = true;

    }

    public void schedulerPausedEvent() {
        // TODO Auto-generated method stub

    }

    public void schedulerResumedEvent() {
        // TODO Auto-generated method stub

    }

    public void schedulerShutDownEvent() {
        if (logger.isDebugEnabled()) {
            System.out.println("Received Scheduler ShutDown event");
        }
        schedulerStopped = true;

    }

    public void schedulerShuttingDownEvent() {
        if (logger.isDebugEnabled()) {
            System.out.println("Received Scheduler Shutting Down event");
        }
        schedulerStopped = true;
    }

    public void schedulerStartedEvent() {
        // TODO Auto-generated method stub

    }

    public void schedulerStoppedEvent() {
        if (logger.isDebugEnabled()) {
            System.out.println("Received Scheduler Stop event");
        }
        schedulerStopped = true;
    }

    public void taskPendingToRunningEvent(TaskEvent event) {
        // TODO Auto-generated method stub

    }

    public void taskRunningToFinishedEvent(TaskEvent event) {
        // TODO Auto-generated method stub

    }

    /**
     * Handles case of an unsucessful job
     * @param jobId id of the job
     * @param ex exception thrown
     * @param printStack do we print the stack trace ?
     * @param logs logs of the task creating the problem 
     */
    private void jobDidNotSucceed(JobId jobId, Throwable ex, boolean printStack, String logs) {
        System.err.println("Job did not succeed");
        if (printStack) {
            ex.printStackTrace();
        }
        if (errorToThrow == null) {
            errorToThrow = ex;
        }
        isJobFinished = true;
    }

    /**
     * {@inheritDoc}
     */
    public void runActivity(final Body body) {
        Service service = new Service(body);
        while (!terminated) {
            try {

                service.waitForRequest();
                if (logger.isDebugEnabled()) {
                    System.out.println("Request received");
                }
                // We detect a waitXXX request in the request queue
                Request waitRequest = service.getOldest("waitAllResults");
                if (waitRequest != null) {
                    if (pendingRequest == null) {
                        // if there is one and there was none previously found we remove it and store it for later
                        pendingRequest = waitRequest;
                        if (logger.isDebugEnabled()) {
                            System.out.println("Blocking removing waitAllResults");
                        }
                        service.blockingRemoveOldest("waitAllResults");
                        //Request submitRequest = buildRequest(body);
                        //service.serve(submitRequest);
                    } else {
                        // if there is one and there was another one pending, we serve it immediately (it's an error)
                        service.serveOldest("waitAllResults");
                    }
                }

                // we serve everything else which is not a waitXXX method
                // Careful, the order is very important here, we need to serve the solve method before the waitXXX
                service.serveAll(new FindNotWaitFilter());

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
     * @param body 
     */
    protected void maybeServePending(Service service) {
        if (pendingRequest != null) {
            if (isJobFinished() || jobKilled || schedulerStopped) {
                servePending(service);
            }
        }
    }

    /**
     * Serve the pending waitXXX method
     * @param body 
     */
    protected void servePending(Service service) {
        Request req = pendingRequest;
        pendingRequest = null;
        service.serve(req);
    }

    protected boolean isJobFinished() {
        return this.isJobFinished;
    }

    /**
     * @author The ProActive Team
     * Internal class for filtering requests in the queue
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

    public void schedulerRMDownEvent() {
        // TODO Auto-generated method stub

    }

    public void schedulerRMUpEvent() {
        // TODO Auto-generated method stub

    }

    public void usersUpdate(UserIdentification userIdentification) {
        // TODO Auto-generated method stub

    }

}
