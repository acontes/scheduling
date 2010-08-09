/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2010 INRIA/University of
 * 				Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
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
 * If needed, contact us to obtain a release under GPL Version 2
 * or a different license than the GPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $ACTIVEEON_INITIAL_DEV$
 */
package org.ow2.proactive.scheduler.common;

import org.objectweb.proactive.annotation.PublicAPI;
import org.ow2.proactive.scheduler.common.exception.JobAlreadyFinishedException;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.exception.UnknownTaskException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.util.logforwarder.AppenderProvider;
import org.ow2.proactive.scheduler.common.util.logforwarder.LogForwardingService;


/**
 * Scheduler interface for someone connected to the scheduler. This is suitable for every role in the Scheduler<br>
 * This interface provides methods to managed the user task and jobs on the scheduler.<br>
 *
 * Scheduler currently has 2 roles :
 * <ul>
 * <li>User : </li>will only be able to managed his jobs and tasks, and also see the entire scheduling process.
 * <li>Admin : </li>Should do what user can do + administration stuffs
 * </ul>
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 2.0
 */
@PublicAPI
public interface Scheduler extends SchedulerCoreMethods {
    /**
     * Submit a new job to the scheduler.
     * A user can only managed their jobs.
     * <p>
     * It will execute the tasks of the jobs as soon as resources are available.
     * The job will be considered as finished once every tasks have finished (error or success).
     * </p>
     * Thus, user could get the job result according to the precious result.
     * <br /><br />
     * It is possible to get a listener on the scheduler.
     * (see {@link Scheduler#addEventListener(SchedulerEventListener, boolean, SchedulerEvent...)} for more details)
     *
     * @param job the new job to submit.
     * @return the generated new job ID.
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular method.
     * @throws SubmissionClosedException if the submit action could not be performed.
     * @throws JobCreationException if Their was a problem while creation the job
     */
    public JobId submit(Job job) throws NotConnectedException, PermissionException,
            SubmissionClosedException, JobCreationException;

    /**
     * Get the result for the given jobId.<br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only get HIS result back except if he is admin.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.<br>
     * So, if you have the right to get the job result represented by the given jobId and if the job exists,
     * so you will receive the result. In any other cases a schedulerException will be thrown.
     *
     * @param jobId the job on which the result will be send
     * @return a job Result containing information about the result.
     * 		If the job result is not yet available (job not finished), null is returned.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public JobResult getJobResult(String jobId) throws NotConnectedException, PermissionException,
            UnknownJobException;

    /**
     * Get the result for the given task name in the given jobId. <br >
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only get HIS result back.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.<br>
     * So, if you have the right to get the task result that is in the job represented by the given jobId and if the job and task name exist,
     * so you will receive the result. In any other cases a schedulerException will be thrown.<br>
     *
     * @param jobId the job in which the task result is.
     * @param taskName the name of the task in which the result is.
     * @return a job Result containing information about the result.
     * 		If null is returned, this task is not yet terminated or not available.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws UnknownTaskException if this task is unknown in the Job.
     * @throws PermissionException if you can't access to this particular job.
     */
    public TaskResult getTaskResult(String jobId, String taskName) throws NotConnectedException,
            UnknownJobException, UnknownTaskException, PermissionException;

    /**
     * Remove the job from the scheduler. <br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only remove HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job to be removed.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public void removeJob(String jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Listen for the tasks user logs.<br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only listen to HIS jobs.
     *
     * @param jobId the id of the job to listen to.
     * @param appenderProvider a provider for an appender that must be connected on a log server on the caller side (see {@link LogForwardingService})
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public void listenJobLogs(String jobId, AppenderProvider appenderProvider) throws NotConnectedException,
            UnknownJobException, PermissionException;

    /**
     * Kill the job represented by jobId.<br>
     * This method will kill every running tasks of this job, and remove it from the scheduler.<br>
     * The job won't be terminated, it won't have result.<br><br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only kill HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job to kill.
     * @return true if success, false if not.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public boolean killJob(String jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Pause the job represented by jobId.<br>
     * This method will finish every running tasks of this job, and then pause the job.<br>
     * The job will have to be resumed in order to finish.<br><br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only pause HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job to pause.
     * @return true if success, false if not.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public boolean pauseJob(String jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Resume the job represented by jobId.<br>
     * This method will restart every non-finished tasks of this job.<br><br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only resume HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job to resume.
     * @return true if success, false if not.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public boolean resumeJob(String jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Change the priority of the job represented by jobId.<br>
     * Only administrator can change the priority to HIGH, HIGEST, IDLE.<br><br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only change HIS job priority.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job on which to change the priority.
     * @param priority The new priority to apply to the job.
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     * @throws JobAlreadyFinishedException if you want to change the priority on a finished job.
     */
    public void changeJobPriority(String jobId, JobPriority priority) throws NotConnectedException,
            UnknownJobException, PermissionException, JobAlreadyFinishedException;

    /**
     * Return the state of the given job.<br>
     * The state contains informations about the job, every tasks and informations about the tasks.<br><br>
     * The jobId is given as a string. It's in fact the string returned by the {@link JobId#value()} method.<br>
     * A user can only get the state of HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job on which to get the state.
     * @return the current state of the given job
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public JobState getJobState(String jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Get the current status of the Scheduler
     *
     * @return the current status of the Scheduler
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular method.
     */
    public SchedulerStatus getStatus() throws NotConnectedException, PermissionException;

    /**
     * Return the state of the given job.<br>
     * The state contains informations about the job, every tasks and informations about the tasks.<br><br>
     * A user can only get the state of HIS job.<br>
     * If the job does not exist, a schedulerException is sent with the proper message.
     *
     * @param jobId the job on which to get the state.
     * @return the current state of the given job
     * @throws NotConnectedException if you are not authenticated.
     * @throws UnknownJobException if the job does not exist.
     * @throws PermissionException if you can't access to this particular job.
     */
    public JobState getJobState(JobId jobId) throws NotConnectedException, UnknownJobException,
            PermissionException;

    /**
     * Get the list of job states that describe every jobs in the Scheduler.
     * The SchedulerState contains 3 list of jobs, pending, running, and finished
     *
     * @return the list of every jobs in the Scheduler
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular method.
     */
    public SchedulerState getState() throws NotConnectedException, PermissionException;

    /**
     * Add a scheduler event Listener. this listener provides method to notice of
     * new coming job, started task, finished task, running job, finished job, etc...<br>
     * <p>
     * This method behaves exactly the same as a call to addEventListener(sel, myEventsOnly, false, events); but return nothing
     * </p>
     *
     * @param sel a SchedulerEventListener on which the scheduler will talk.
     * @param myEventsOnly a boolean that indicates if you want to receive every event or just the one concerning your jobs.
     * 			This won't affect the scheduler state event that will be sent anyway.
     * @param events An array of events that you want to receive from the scheduler.
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular job.
     */
    public void addEventListener(SchedulerEventListener sel, boolean myEventsOnly, SchedulerEvent... events)
            throws NotConnectedException, PermissionException;

    /**
     * Add a scheduler event Listener. this listener provides method to notice of
     * new coming job, started task, finished task, running job, finished job, etc...<br>
     * <p>
     * You may use this method once by remote or active object.<br>
     * Every call to this method will remove your previous listening settings.<br>
     * If you want to get 2 type of events, add the 2 events type you want at the end of this method. If no type is specified, all of them
     * will be sent.
     * </p>
     * <p>
     * If you want to received the events concerning your job only, just set the 'myEventsOnly' parameter to true. otherwise, you will received
     * events coming from any user.
     * </p>
     *
     * @param sel a SchedulerEventListener on which the scheduler will talk.
     * @param myEventsOnly a boolean that indicates if you want to receive every events or just those concerning your jobs.
     * 			This won't affect the scheduler state event that will be sent anyway.
     * @param getCurrentState if false, this method returns null, if true, it returns the Scheduler current state.
     * @param events An array of events that you want to receive from the scheduler.
     * @return the scheduler current state containing the different lists of jobs if the getInitialState parameter is true, null if false.
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular job.
     */
    public SchedulerState addEventListener(SchedulerEventListener sel, boolean myEventsOnly,
            boolean getCurrentState, SchedulerEvent... events) throws NotConnectedException,
            PermissionException;

    /**
     * Remove the current event listener your listening on.<br>
     * If no listener is defined, this method has no effect.
     *
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular method.
     */
    public void removeEventListener() throws NotConnectedException, PermissionException;

    /**
     * Disconnect properly the user from the scheduler.
     *
     * @throws NotConnectedException if you are not authenticated.
     * @throws PermissionException if you can't access to this particular method.
     */
    public void disconnect() throws NotConnectedException, PermissionException;

    /**
     * Test whether or not the user is connected to the ProActive Scheduler.
     *
     * @return true if the user connected to a Scheduler, false otherwise.
     */
    public boolean isConnected();
}
