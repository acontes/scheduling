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
package org.ow2.proactive.scheduler.job;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.MapKeyManyToMany;
import org.hibernate.annotations.MetaValue;
import org.hibernate.annotations.Proxy;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.dataspaces.core.naming.NamingService;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.db.DatabaseCallback;
import org.ow2.proactive.db.annotation.Alterable;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.exception.ExecutableCreationException;
import org.ow2.proactive.scheduler.common.exception.UnknownTaskException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.common.task.TaskStatus;
import org.ow2.proactive.scheduler.common.task.flow.FlowAction;
import org.ow2.proactive.scheduler.common.task.flow.FlowActionType;
import org.ow2.proactive.scheduler.common.task.flow.FlowBlock;
import org.ow2.proactive.scheduler.core.SchedulerFrontend;
import org.ow2.proactive.scheduler.core.annotation.TransientInSerialization;
import org.ow2.proactive.scheduler.core.db.DatabaseManager;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.descriptor.JobDescriptor;
import org.ow2.proactive.scheduler.descriptor.JobDescriptorImpl;
import org.ow2.proactive.scheduler.descriptor.TaskDescriptor;
import org.ow2.proactive.scheduler.job.JobInfoImpl.ReplicatedTask;
import org.ow2.proactive.scheduler.task.ClientTaskState;
import org.ow2.proactive.scheduler.task.TaskIdImpl;
import org.ow2.proactive.scheduler.task.TaskResultImpl;
import org.ow2.proactive.scheduler.task.internal.InternalForkedJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalNativeTask;
import org.ow2.proactive.scheduler.task.internal.InternalTask;
import org.ow2.proactive.scheduler.task.launcher.TaskLauncher;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;


/**
 * Internal and global description of a job.
 * This class contains all informations about the job to launch.
 * It also provides method to manage the content regarding the scheduling process.<br/>
 * Specific internal job may extend this abstract class.
 *
 * @author The ProActive Team
 * @since ProActive Scheduling 0.9
 */
@MappedSuperclass
@Table(name = "INTERNAL_JOB")
@AccessType("field")
@Proxy(lazy = false)
public abstract class InternalJob extends JobState {
    public static final Logger logger_dev = ProActiveLogger.getLogger(SchedulerDevLoggers.CORE);

    /** Owner of the job */
    @Column(name = "OWNER")
    private String owner = "";

    /** List of every tasks in this job. */
    @ManyToAny(metaColumn = @Column(name = "ITASK_TYPE", length = 5))
    @AnyMetaDef(idType = "long", metaType = "string", metaValues = {
            @MetaValue(targetEntity = InternalJavaTask.class, value = "IJT"),
            @MetaValue(targetEntity = InternalNativeTask.class, value = "INT"),
            @MetaValue(targetEntity = InternalForkedJavaTask.class, value = "IFJT") })
    @JoinTable(joinColumns = @JoinColumn(name = "ITASK_ID"), inverseJoinColumns = @JoinColumn(name = "DEPEND_ID"))
    @LazyCollection(value = LazyCollectionOption.FALSE)
    @Cascade(CascadeType.ALL)
    @MapKeyManyToMany(targetEntity = TaskIdImpl.class)
    protected Map<TaskId, InternalTask> tasks = new HashMap<TaskId, InternalTask>();

    /** Informations (that can be modified) about job execution */
    @Alterable
    @Cascade(CascadeType.ALL)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = JobInfoImpl.class)
    protected JobInfoImpl jobInfo = new JobInfoImpl();

    /** Job descriptor for dependences management */
    //Not DB managed, created once needed.
    @Transient
    @TransientInSerialization
    @XmlTransient
    private JobDescriptor jobDescriptor;

    /** DataSpace application manager for this job */
    //Not DB managed, created once needed.
    @Transient
    @TransientInSerialization
    @XmlTransient
    private JobDataSpaceApplication jobDataSpaceApplication;

    /** Job result */
    @Cascade(CascadeType.ALL)
    @OneToOne(fetch = FetchType.EAGER, targetEntity = JobResultImpl.class)
    @TransientInSerialization
    private JobResult jobResult;

    /** Initial waiting time for a task before restarting in millisecond */
    @Column(name = "RESTART_TIMER")
    @TransientInSerialization
    private long restartWaitingTimer = PASchedulerProperties.REEXECUTION_INITIAL_WAITING_TIME.getValueAsInt();

    /** used credentials to fork as user id. Can be null, or contain user/pwd[/key] */
    @Lob
    @Column(name = "CREDENTIALS", updatable = false, length = 16384/* 16Ko max */)
    @XmlTransient
    @TransientInSerialization
    private Credentials credentials = null;

    /** Hibernate default constructor */
    public InternalJob() {
    }

    /**
     * Create a new Job with the given parameters. It provides methods to add or
     * remove tasks.
     *
     * @param name the current job name.
     * @param priority the priority of this job between 1 and 5.
     * @param cancelJobOnError true if the job has to run until its end or an user intervention.
     * @param description a short description of the job and what it will do.
     */

    public InternalJob(String name, JobPriority priority, boolean cancelJobOnError, String description) {
        this.name = name;
        this.jobInfo.setPriority(priority);
        this.setCancelJobOnError(cancelJobOnError);
        this.description = description;
    }

    /**
     * This method will do two things :<br />
     * First, it will update the job with the informations contained in the given taskInfo<br />
     * Then, it will update the proper task using the same given taskInfo.
     *
     * @param info a taskInfo containing new information about the task.
     */
    @Override
    public synchronized void update(TaskInfo info) {
        //ensure that is a JobInfoImpl
        //if not, we are in client side and client brings its own JobInfo Implementation
        if (!getId().equals(info.getJobId())) {
            throw new IllegalArgumentException(
                "This job info is not applicable for this job. (expected id is '" + getId() + "' but was '" +
                    info.getJobId() + "'");
        }
        jobInfo = (JobInfoImpl) info.getJobInfo();
        try {
            tasks.get(info.getTaskId()).update(info);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("This task info is not applicable in this job. (task id '" +
                info.getTaskId() + "' not found)");
        }
    }

    /**
     * To update the content of this job with a jobInfo.
     *
     * @param info the JobInfo to set
     */
    @Override
    public synchronized void update(JobInfo info) {
        if (!getId().equals(info.getJobId())) {
            throw new IllegalArgumentException(
                "This job info is not applicable for this job. (expected id is '" + getId() + "' but was '" +
                    info.getJobId() + "'");
        }
        //update job info
        this.jobInfo = (JobInfoImpl) info;
        //update task status if needed
        if (this.jobInfo.getTaskStatusModify() != null) {
            for (TaskId id : tasks.keySet()) {
                tasks.get(id).setStatus(this.jobInfo.getTaskStatusModify().get(id));
            }
        }
        //update task finished time if needed
        if (this.jobInfo.getTaskFinishedTimeModify() != null) {
            for (TaskId id : tasks.keySet()) {
                if (this.jobInfo.getTaskFinishedTimeModify().containsKey(id)) {
                    //a null send to a long setter throws a NullPointerException so, here is the fix
                    tasks.get(id).setFinishedTime(this.jobInfo.getTaskFinishedTimeModify().get(id));
                }
            }
        }
        // update skipped tasks
        if (this.jobInfo.getTasksSkipped() != null) {
            for (TaskId id : tasks.keySet()) {
                if (this.jobInfo.getTasksSkipped().contains(id)) {
                    InternalTask it = tasks.get(id);
                    it.setStatus(TaskStatus.SKIPPED);
                }
            }
        }
        // replicated tasks have been added through FlowAction#REPLICATE
        if (this.jobInfo.getTasksReplicated() != null) {
            updateTasksReplicated();
        }
        // replicated tasks have been added through FlowAction#LOOP
        if (this.jobInfo.getTasksLooped() != null) {
            updateTasksLooped();
        }
    }

    /**
     * Updates this job when tasks were replicated due to a {@link FlowActionType#REPLICATE} action
     * <p>
     * The internal state of the job will change: new tasks will be added,
     * existing tasks will be modified.
     */
    private void updateTasksReplicated() {
        // key: replicated task / value : id of the original task
        // originalId not used as key because tasks can be replicated multiple times
        Map<InternalTask, TaskId> newTasks = new TreeMap<InternalTask, TaskId>();

        // create the new tasks
        for (ReplicatedTask it : this.jobInfo.getTasksReplicated()) {
            InternalTask original = this.tasks.get(it.originalId);
            InternalTask replicated = null;
            try {
                replicated = (InternalTask) original.replicate();
            } catch (Exception e) {
            }
            replicated.setId(it.replicatedId);
            // for some reason the indices are embedded in the Readable name and not where they belong
            int dupId = InternalTask.getReplicationIndexFromName(it.replicatedId.getReadableName());
            int itId = InternalTask.getIterationIndexFromName(it.replicatedId.getReadableName());
            replicated.setReplicationIndex(dupId);
            replicated.setIterationIndex(itId);

            this.tasks.put(it.replicatedId, replicated);
            newTasks.put(replicated, it.originalId);

            // when nesting REPLICATE, the original replicated task can have its Replication Index changed:
            // the new Replication Id of the original task is the lowest id among replicated tasks minus one
            int minDup = Integer.MAX_VALUE;
            for (ReplicatedTask it2 : this.jobInfo.getTasksReplicated()) {
                String iDup = InternalTask.getInitialName(it2.replicatedId.getReadableName());
                String iOr = InternalTask.getInitialName(it.originalId.getReadableName());
                if (iDup.equals(iOr)) {
                    int iDupId = InternalTask.getReplicationIndexFromName(it2.replicatedId.getReadableName());
                    minDup = Math.min(iDupId, minDup);
                }
            }
            original.setReplicationIndex(minDup - 1);
        }

        // recreate deps contained in the data struct
        for (ReplicatedTask it : this.jobInfo.getTasksReplicated()) {
            InternalTask newtask = this.tasks.get(it.replicatedId);
            for (TaskId depId : it.deps) {
                InternalTask dep = this.tasks.get(depId);
                newtask.addDependence(dep);
            }
        }

        // plug mergers
        List<InternalTask> toAdd = new ArrayList<InternalTask>();
        for (InternalTask old : this.tasks.values()) {
            // task is not a replicated one, has dependencies
            if (!newTasks.containsValue(old.getId()) && old.hasDependences()) {
                for (InternalTask oldDep : old.getIDependences()) {
                    // one of its dependencies is a replicated task
                    if (newTasks.containsValue(oldDep.getId())) {
                        // connect those replicated tasks to the merger
                        for (Entry<InternalTask, TaskId> newTask : newTasks.entrySet()) {
                            if (newTask.getValue().equals(oldDep.getId())) {
                                toAdd.add(newTask.getKey());
                            }
                        }
                    }
                }
                // avoids concurrent modification
                for (InternalTask newDep : toAdd) {
                    old.addDependence(newDep);
                }
                toAdd.clear();
            }
        }

    }

    /**
     * Updates this job when tasks were replicated due to a {@link FlowActionType#LOOP} action
     * <p>
     * The internal state of the job will change: new tasks will be added,
     * existing tasks will be modified.
     */
    private void updateTasksLooped() {
        Map<TaskId, InternalTask> newTasks = new TreeMap<TaskId, InternalTask>();

        // create the new tasks
        for (ReplicatedTask it : this.jobInfo.getTasksLooped()) {
            InternalTask original = this.tasks.get(it.originalId);
            InternalTask replicated = null;
            try {
                replicated = (InternalTask) original.replicate();
            } catch (Exception e) {
            }
            replicated.setId(it.replicatedId);
            // for some reason the indices are embedded in the Readable name and not where they belong
            int dupId = InternalTask.getReplicationIndexFromName(it.replicatedId.getReadableName());
            int itId = InternalTask.getIterationIndexFromName(it.replicatedId.getReadableName());
            replicated.setReplicationIndex(dupId);
            replicated.setIterationIndex(itId);

            this.tasks.put(it.replicatedId, replicated);
            newTasks.put(it.originalId, replicated);
        }

        InternalTask oldInit = null;
        // recreate deps contained in the data struct
        for (ReplicatedTask it : this.jobInfo.getTasksLooped()) {
            InternalTask newtask = this.tasks.get(it.replicatedId);
            for (TaskId depId : it.deps) {
                InternalTask dep = this.tasks.get(depId);
                if (!newTasks.containsValue(dep)) {
                    oldInit = dep;
                }
                newtask.addDependence(dep);
            }
        }

        // find mergers
        InternalTask newInit = null;
        InternalTask merger = null;
        for (InternalTask old : this.tasks.values()) {
            // the merger is not a replicated task, nor has been replicated
            if (!newTasks.containsKey(old.getId()) && !newTasks.containsValue(old) && old.hasDependences()) {
                for (InternalTask oldDep : old.getIDependences()) {
                    // merger's deps contains the initiator of the LOOP
                    if (oldDep.equals(oldInit)) {
                        merger = old;
                        break;
                    }
                }
                if (merger != null) {
                    break;
                }
            }
        }

        // merger can be null
        if (merger != null) {
            // find new initiator
            Map<TaskId, InternalTask> newTasks2 = new HashMap<TaskId, InternalTask>();
            for (InternalTask it : newTasks.values()) {
                newTasks2.put(it.getId(), it);
            }
            for (InternalTask it : newTasks.values()) {
                if (it.hasDependences()) {
                    for (InternalTask dep : it.getIDependences()) {
                        newTasks2.remove(dep.getId());
                    }
                }
            }
            for (InternalTask it : newTasks2.values()) {
                newInit = it;
                break;
            }
            merger.getIDependences().remove(oldInit);
            merger.addDependence(newInit);
        }
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.JobState#getJobInfo()
     */
    @Override
    public JobInfo getJobInfo() {
        return jobInfo;
    }

    /**
     * Append a task to this job.
     *
     * @param task the task to add.
     * @return true if the task has been correctly added to the job, false if
     *         not.
     */
    public boolean addTask(InternalTask task) {
        task.setJobId(getId());

        if (TaskIdImpl.getCurrentValue() < this.tasks.size()) {
            TaskIdImpl.initialize(tasks.size());
        }
        task.setId(TaskIdImpl.nextId(getId(), task.getName()));

        boolean result = (tasks.put(task.getId(), task) == null);

        if (result) {
            jobInfo.setTotalNumberOfTasks(jobInfo.getTotalNumberOfTasks() + 1);
        }

        return result;
    }

    /**
     * Start a new task will set some count and update dependencies if necessary.
     *
     * @param td the task which has just been started.
     */
    public void startTask(InternalTask td) {
        logger_dev.debug(" ");
        setNumberOfPendingTasks(getNumberOfPendingTasks() - 1);
        setNumberOfRunningTasks(getNumberOfRunningTasks() + 1);

        if (getStatus() == JobStatus.STALLED) {
            setStatus(JobStatus.RUNNING);
        }

        getJobDescriptor().start(td.getId());
        td.setStatus(TaskStatus.RUNNING);
        td.setStartTime(System.currentTimeMillis());
        td.setFinishedTime(-1);
        td.setExecutionHostName(td.getExecuterInformations().getHostName() + " (" +
            td.getExecuterInformations().getNodeName() + ")");
    }

    /**
     * Start dataspace configuration and application
     */
    public void startDataSpaceApplication(NamingService namingService, String namingServiceURL) {
        if (jobDataSpaceApplication == null) {
            long appId = getJobInfo().getJobId().hashCode();
            jobDataSpaceApplication = new JobDataSpaceApplication(appId, namingService, namingServiceURL);
        }
        jobDataSpaceApplication.startDataSpaceApplication(getInputSpace(), getOutputSpace(), getOwner(),
                getId());
    }

    /**
     * Updates count for running to pending event.
     */
    public void newWaitingTask() {
        logger_dev.debug(" ");
        setNumberOfPendingTasks(getNumberOfPendingTasks() + 1);
        setNumberOfRunningTasks(getNumberOfRunningTasks() - 1);
        if (getNumberOfRunningTasks() == 0 && getStatus() != JobStatus.PAUSED) {
            setStatus(JobStatus.STALLED);
        }
    }

    /**
     * Set this task in restart mode, it will set the task to pending status and change task count.
     *
     * @param task the task which has to be restarted.
     */
    public void reStartTask(InternalTask task) {
        logger_dev.debug(" ");
        getJobDescriptor().reStart(task.getId());
        task.setProgress(0);
        if (getStatus() == JobStatus.PAUSED) {
            task.setStatus(TaskStatus.PAUSED);
            HashMap<TaskId, TaskStatus> hts = new HashMap<TaskId, TaskStatus>();
            hts.put(task.getId(), task.getStatus());
            getJobDescriptor().update(hts);
        } else {
            task.setStatus(TaskStatus.PENDING);
        }
    }

    /**
     * Terminate a task, change status, managing dependences
     *
     * Also, apply a Control Flow Action if provided.
     * This may alter the number of tasks in the job,
     * events have to be sent accordingly.
     * 
     * @param errorOccurred has an error occurred for this termination
     * @param taskId the task to terminate.
     * @param frontend Used to notify all listeners of the replication of tasks, triggered by the FlowAction
     * @param action a Control Flow Action that will potentially create new tasks inside the job
     * @return the taskDescriptor that has just been terminated.
     */
    public InternalTask terminateTask(boolean errorOccurred, TaskId taskId, SchedulerFrontend frontend,
            FlowAction action) {
        logger_dev.debug(" ");
        final InternalTask descriptor = tasks.get(taskId);
        descriptor.setFinishedTime(System.currentTimeMillis());
        descriptor.setStatus(errorOccurred ? TaskStatus.FAULTY : TaskStatus.FINISHED);
        try {
            descriptor.setExecutionDuration(((TaskResultImpl) getJobResult().getResult(descriptor.getName()))
                    .getTaskDuration());
        } catch (UnknownTaskException ute) {
            //should never happen : taskName is unknown
            logger_dev.error("", ute);
        }
        setNumberOfRunningTasks(getNumberOfRunningTasks() - 1);
        setNumberOfFinishedTasks(getNumberOfFinishedTasks() + 1);

        if ((getStatus() == JobStatus.RUNNING) && (getNumberOfRunningTasks() == 0)) {
            setStatus(JobStatus.STALLED);
        }

        boolean didAction = false;
        if (action != null) {
            InternalTask initiator = tasks.get(taskId);

            List<InternalTask> modifiedTasks = new ArrayList<InternalTask>();

            switch (action.getType()) {
                /*
                 * LOOP action
                 * 
                 */
                case LOOP: {

                    {
                        // find the target of the loop
                        InternalTask target = null;
                        if (action.getTarget().equals(initiator.getName())) {
                            target = initiator;
                        } else {
                            target = findTaskUp(action.getTarget(), initiator);
                        }
                        TaskId targetId = target.getTaskInfo().getTaskId();

                        logger_dev.info("Control Flow Action LOOP (init:" + initiator.getId() + ";target:" +
                            target.getId() + ")");

                        // accumulates the tasks between the initiator and the target
                        Map<TaskId, InternalTask> dup = new HashMap<TaskId, InternalTask>();

                        // replicate the tasks between the initiator and the target
                        try {
                            initiator.replicateTree(dup, targetId, true, initiator.getReplicationIndex(),
                                    initiator.getIterationIndex());
                        } catch (ExecutableCreationException e) {
                            logger_dev.error("", e);
                            break;
                        }

                        ((JobInfoImpl) this.getJobInfo()).setNumberOfPendingTasks(this.getJobInfo()
                                .getNumberOfPendingTasks() +
                            dup.size());

                        // ensure naming unicity
                        // time-consuming but safe
                        for (InternalTask nt : dup.values()) {
                            boolean ok;
                            do {
                                ok = true;
                                for (InternalTask task : tasks.values()) {
                                    if (nt.getName().equals(task.getName())) {
                                        nt.setIterationIndex(nt.getIterationIndex() + 1);
                                        ok = false;
                                    }
                                }
                            } while (!ok);
                        }

                        // configure the new tasks
                        InternalTask newTarget = null;
                        InternalTask newInit = null;
                        for (Entry<TaskId, InternalTask> it : dup.entrySet()) {
                            InternalTask nt = it.getValue();
                            if (target.getId().equals(it.getKey())) {
                                newTarget = nt;
                            }
                            if (initiator.getId().equals(it.getKey())) {
                                newInit = nt;
                            }
                            nt.setJobInfo(getJobInfo());
                            this.addTask(nt);
                            //add entry to job result
                            ((JobResultImpl) this.getJobResult()).addToAllResults(nt.getName());
                        }
                        modifiedTasks.addAll(dup.values());

                        // connect replicated tree
                        newTarget.addDependence(initiator);
                        modifiedTasks.add(newTarget);

                        // connect mergers
                        List<InternalTask> mergers = new ArrayList<InternalTask>();
                        for (InternalTask t : this.tasks.values()) {
                            if (t.getIDependences() != null) {

                                for (InternalTask p : t.getIDependences()) {
                                    if (p.getId().equals(initiator.getId())) {
                                        if (!t.equals(newTarget)) {
                                            mergers.add(t);
                                        }
                                    }
                                }
                            }
                        }
                        for (InternalTask t : mergers) {
                            t.getIDependences().remove(initiator);
                            t.addDependence(newInit);
                        }
                        modifiedTasks.addAll(mergers);

                        // propagate the changes in the job descriptor
                        getJobDescriptor().doLoop(taskId, dup, newTarget, newInit);

                        this.jobInfo.setModifiedTasks(createClientTaskStates(modifiedTasks));
                        // notify frontend that tasks were added and modified
                        frontend.jobStateUpdated(this.getOwner(), new NotificationData<JobInfo>(
                            SchedulerEvent.TASK_REPLICATED, this.getJobInfo()));
                        this.jobInfo.setModifiedTasks(null);

                        didAction = true;
                        break;
                    }
                }

                    /*
                     * IF action
                     * 
                     */
                case IF:

                {
                    // the targetIf from action.getTarget() is the selected branch;
                    // the IF condition has already been evaluated prior to being put in a FlowAction
                    // the targetElse from action.getTargetElse() is the branch that was NOT selected
                    InternalTask targetIf = null;
                    InternalTask targetElse = null;
                    InternalTask targetJoin = null;

                    // search for the targets as perfect matches of the unique name
                    for (InternalTask it : tasks.values()) {

                        // target is finished : probably looped
                        if (it.getStatus().equals(TaskStatus.FINISHED) ||
                            it.getStatus().equals(TaskStatus.SKIPPED)) {
                            continue;
                        }
                        if (action.getTarget().equals(it.getName())) {
                            if (it.getIfBranch().equals(initiator)) {
                                targetIf = it;
                            }
                        } else if (action.getTargetElse().equals(it.getName())) {
                            if (it.getIfBranch().equals(initiator)) {
                                targetElse = it;
                            }
                        } else if (action.getTargetContinuation().equals(it.getName())) {
                            if (findTaskUp(initiator.getName(), it).equals(initiator)) {
                                targetJoin = it;
                            }
                        }

                    }

                    boolean searchIf = (targetIf == null);
                    boolean searchElse = (targetElse == null);
                    boolean searchJoin = (targetJoin == null);

                    // search of a runnable perfect match for the targets failed;
                    // the natural target was iterated, need to find the next iteration
                    // which is the the one with the same dup index and base name,
                    // but the highest iteration index
                    for (InternalTask it : tasks.values()) {

                        // does not share the same dup index : cannot be the same scope
                        if (it.getReplicationIndex() != initiator.getReplicationIndex()) {
                            continue;
                        }

                        if (it.getStatus().equals(TaskStatus.FINISHED) ||
                            it.getStatus().equals(TaskStatus.SKIPPED)) {
                            continue;
                        }

                        String name = InternalTask.getInitialName(it.getName());

                        if (searchIf && InternalTask.getInitialName(action.getTarget()).equals(name)) {
                            if (targetIf == null || targetIf.getIterationIndex() < it.getIterationIndex()) {
                                targetIf = it;
                            }
                        } else if (searchElse &&
                            InternalTask.getInitialName(action.getTargetElse()).equals(name)) {
                            if (targetElse == null || targetElse.getIterationIndex() < it.getIterationIndex()) {
                                targetElse = it;
                            }
                        } else if (searchJoin &&
                            InternalTask.getInitialName(action.getTargetContinuation()).equals(name)) {
                            if (targetJoin == null || targetJoin.getIterationIndex() < it.getIterationIndex()) {
                                targetJoin = it;
                            }
                        }
                    }

                    logger_dev.info("Control Flow Action IF: " + targetIf.getId() + " join: " +
                        ((targetJoin == null) ? "null" : targetJoin.getId()));

                    // these 2 tasks delimit the Task Block formed by the IF branch
                    InternalTask branchStart = targetIf;
                    InternalTask branchEnd = null;

                    String match = targetIf.getMatchingBlock();
                    if (match != null) {
                        for (InternalTask t : tasks.values()) {
                            if (match.equals(t.getName()) &&
                                !(t.getStatus().equals(TaskStatus.FINISHED) || t.getStatus().equals(
                                        TaskStatus.SKIPPED))) {
                                branchEnd = t;
                            }
                        }
                    }
                    // no matching block: there is no block, the branch is a single task
                    if (branchEnd == null) {
                        branchEnd = targetIf;
                    }

                    // plug the branch
                    branchStart.addDependence(initiator);
                    modifiedTasks.add(branchStart);
                    if (targetJoin != null) {
                        targetJoin.addDependence(branchEnd);
                        modifiedTasks.add(targetJoin);
                    }

                    // the other branch will not be executed
                    // first, find the concerned tasks
                    List<InternalTask> elseTasks = new ArrayList<InternalTask>();
                    //  elseTasks.add(targetElse);
                    for (InternalTask t : this.tasks.values()) {
                        if (t.dependsOn(targetElse)) {
                            elseTasks.add(t);
                        }
                    }

                    // even though the targetElse is not going to be executed, a
                    // dependency on initiator still makes sense and would help
                    // reconstruct the job graph on the client
                    targetElse.addDependence(initiator);
                    modifiedTasks.add(targetElse);

                    List<TaskId> tev = new ArrayList<TaskId>(elseTasks.size());
                    for (InternalTask it : elseTasks) {
                        it.setFinishedTime(System.currentTimeMillis());
                        it.setStatus(TaskStatus.SKIPPED);
                        it.setExecutionDuration(0);
                        setNumberOfPendingTasks(getNumberOfPendingTasks() - 1);
                        setNumberOfFinishedTasks(getNumberOfFinishedTasks() + 1);
                        tev.add(it.getId());
                        DatabaseManager.getInstance().unload(it);
                        logger_dev.info("Task " + it.getId() + " will not be executed");
                    }

                    // plug the branch in the descriptor
                    TaskId joinId = null;
                    if (targetJoin != null) {
                        joinId = targetJoin.getId();
                    }
                    getJobDescriptor().doIf(initiator.getId(), branchStart.getId(), branchEnd.getId(),
                            joinId, targetElse.getId(), elseTasks);

                    this.jobInfo.setTasksSkipped(tev);
                    this.jobInfo.setModifiedTasks(createClientTaskStates(modifiedTasks));
                    // notify frontend that tasks were modified
                    frontend.jobStateUpdated(this.getOwner(), new NotificationData<JobInfo>(
                        SchedulerEvent.TASK_SKIPPED, this.getJobInfo()));
                    this.jobInfo.setTasksSkipped(null);
                    this.jobInfo.setModifiedTasks(null);

                    // no jump is performed ; now that the tasks have been plugged
                    // the flow can continue its normal operation
                    getJobDescriptor().terminate(taskId);

                    didAction = true;

                    break;
                }

                    /*
                     * REPLICATE action
                     * 
                     */
                case REPLICATE:

                {
                    int runs = action.getDupNumber();
                    if (runs < 1) {
                        runs = 1;
                    }

                    logger_dev.info("Control Flow Action REPLICATE (runs:" + runs + ")");
                    List<InternalTask> toReplicate = new ArrayList<InternalTask>();

                    // find the tasks that need to be replicated
                    for (InternalTask ti : tasks.values()) {
                        List<InternalTask> tl = ti.getIDependences();
                        if (tl != null) {
                            for (InternalTask ts : tl) {
                                if (ts.getId().equals(initiator.getId()) && !toReplicate.contains(ti)) {
                                    // ti needs to be replicated
                                    toReplicate.add(ti);
                                }
                            }
                        }
                    }

                    // for each initial task to replicate
                    for (InternalTask todup : toReplicate) {

                        // determine the target of the replication whether it is a block or a single task
                        InternalTask target = null;

                        // target is a task block start : replication of the block
                        if (todup.getFlowBlock().equals(FlowBlock.START)) {
                            String tg = todup.getMatchingBlock();
                            for (InternalTask t : tasks.values()) {
                                if (tg.equals(t.getName()) &&
                                    !(t.getStatus().equals(TaskStatus.FINISHED) || t.getStatus().equals(
                                            TaskStatus.SKIPPED)) && t.dependsOn(todup)) {
                                    target = t;
                                    break;
                                }
                            }
                            if (target == null) {
                                logger_dev.error("REPLICATE: could not find matching block '" + tg + "'");
                                continue;
                            }
                        }
                        // target is not a block : replication of the task
                        else {
                            target = todup;
                        }

                        // for each number of parallel run
                        for (int i = 1; i < runs; i++) {

                            // accumulates the tasks between the initiator and the target
                            Map<TaskId, InternalTask> dup = new HashMap<TaskId, InternalTask>();
                            // replicate the tasks between the initiator and the target
                            try {
                                target.replicateTree(dup, todup.getId(), false, initiator
                                        .getReplicationIndex() *
                                    runs, 0);
                            } catch (Exception e) {
                                logger_dev.error("REPLICATE: could not replicate tree", e);
                                break;
                            }

                            ((JobInfoImpl) this.getJobInfo()).setNumberOfPendingTasks(this.getJobInfo()
                                    .getNumberOfPendingTasks() +
                                dup.size());

                            // pointers to the new replicated tasks corresponding the begin and 
                            // the end of the block ; can be the same
                            InternalTask newTarget = null;
                            InternalTask newEnd = null;

                            // configure the new tasks
                            for (Entry<TaskId, InternalTask> it : dup.entrySet()) {
                                InternalTask nt = it.getValue();
                                nt.setJobInfo(getJobInfo());
                                this.addTask(nt);
                                int dupIndex = initiator.getReplicationIndex() * runs + i;
                                nt.setReplicationIndex(dupIndex);
                                //add entry to job result
                                ((JobResultImpl) this.getJobResult()).addToAllResults(nt.getName());
                            }
                            modifiedTasks.addAll(dup.values());

                            // find the beginning and the ending of the replicated block
                            for (Entry<TaskId, InternalTask> it : dup.entrySet()) {
                                InternalTask nt = it.getValue();

                                // connect the first task of the replicated block to the initiator
                                if (todup.getId().equals(it.getKey())) {
                                    newTarget = nt;
                                    newTarget.addDependence(initiator);
                                    // no need to add newTarget to modifiedTasks
                                    // because newTarget is among dup.values(), and we
                                    // have added them all
                                }
                                // connect the last task of the block with the merge task(s)
                                if (target.getId().equals(it.getKey())) {
                                    newEnd = nt;

                                    List<InternalTask> toAdd = new ArrayList<InternalTask>();
                                    // find the merge tasks ; can be multiple
                                    for (InternalTask t : tasks.values()) {
                                        List<InternalTask> pdeps = t.getIDependences();
                                        if (pdeps != null) {
                                            for (InternalTask parent : pdeps) {
                                                if (parent.getId().equals(target.getId())) {
                                                    toAdd.add(t);
                                                }
                                            }
                                        }
                                    }
                                    // connect the merge tasks
                                    for (InternalTask t : toAdd) {
                                        t.addDependence(newEnd);
                                        modifiedTasks.add(t);
                                    }
                                }
                            }

                            // propagate the changes on the JobDescriptor
                            getJobDescriptor().doReplicate(taskId, dup, newTarget, target.getId(),
                                    newEnd.getId());

                        }
                    }

                    // notify frontend that tasks were added to the job
                    this.jobInfo.setModifiedTasks(createClientTaskStates(modifiedTasks));
                    frontend.jobStateUpdated(this.getOwner(), new NotificationData<JobInfo>(
                        SchedulerEvent.TASK_REPLICATED, this.getJobInfo()));
                    this.jobInfo.setModifiedTasks(null);

                    // no jump is performed ; now that the tasks have been replicated and
                    // configured, the flow can continue its normal operation
                    getJobDescriptor().terminate(taskId);
                    didAction = true;
                    break;

                }

                    /*
                     * CONTINUE action :
                     * - continue taskflow as if no action was provided
                     */
                case CONTINUE:

                    logger_dev.debug("Task flow Action CONTINUE on task " +
                        initiator.getId().getReadableName());
                    break;
            }

            /**
            System.out.println("******** task dump ** " + this.getJobInfo().getJobId() + " " +
                initiator.getName() + " does " + action.getType() + " " +
                ((action.getTarget() == null) ? "." : action.getTarget()) + " " +
                ((action.getTargetElse() == null) ? "." : action.getTargetElse()) + " " +
                ((action.getTargetJoin() == null) ? "." : action.getTargetJoin()));
            for (InternalTask it : this.tasks.values()) {
                System.out.print(it.getName() + " ");
                if (it.getIDependences() != null) {
                    System.out.print("deps ");
                    for (InternalTask parent : it.getIDependences()) {
                        System.out.print(parent.getName() + " ");
                    }
                }
                if (it.getIfBranch() != null) {
                    System.out.print("if " + it.getIfBranch().getName() + " ");
                }
                if (it.getJoinedBranches() != null && it.getJoinedBranches().size() == 2) {
                    System.out.print("join " + it.getJoinedBranches().get(0).getName() + " " +
                        it.getJoinedBranches().get(1).getName());
                }
                System.out.println();
            }
            System.out.println("******** task dump ** " + this.getJobInfo().getJobId());
            System.out.println();
            **/

        }

        //terminate this task
        if (!didAction) {
            getJobDescriptor().terminate(taskId);
        } else {
            DatabaseManager.getInstance().runAsSingleTransaction(new DatabaseCallback() {
                @Override
                public void workWithDatabase(org.ow2.proactive.db.DatabaseManager dbManager) {
                    dbManager.synchronize(getJobInfo());
                    dbManager.synchronize(descriptor.getTaskInfo());
                    dbManager.update(InternalJob.this);
                }
            });
        }

        //creating list of status for the jobDescriptor
        HashMap<TaskId, TaskStatus> hts = new HashMap<TaskId, TaskStatus>();

        for (InternalTask td : tasks.values()) {
            hts.put(td.getId(), td.getStatus());
        }

        //updating job descriptor for eligible task
        getJobDescriptor().update(hts);

        return descriptor;
    }

    private static List<ClientTaskState> createClientTaskStates(List<InternalTask> tasks) {
        List<ClientTaskState> newTasks = new ArrayList<ClientTaskState>();
        for (InternalTask task : tasks) {
            newTasks.add(new ClientTaskState(task));
        }
        return newTasks;
    }

    /**
     * Walk up <code>down</code>'s dependences until
     * a task <code>name</code> is met
     * 
     * also walks weak references created by {@link FlowActionType#IF}
     * 
     * @return the task names <code>name</code>, or null
     */
    private InternalTask findTaskUp(String name, InternalTask down) {
        InternalTask ret = null;
        List<InternalTask> ideps = new ArrayList<InternalTask>();
        if (down.getIDependences() != null) {
            ideps.addAll(down.getIDependences());
        }
        if (down.getJoinedBranches() != null) {
            ideps.addAll(down.getJoinedBranches());
        }
        if (down.getIfBranch() != null) {
            ideps.add(down.getIfBranch());
        }
        for (InternalTask up : ideps) {
            if (up.getName().equals(name)) {
                ret = up;
            } else {
                InternalTask r = findTaskUp(name, up);
                if (r != null) {
                    ret = r;
                }
            }
        }
        return ret;
    }

    /**
     * Simulate that a task have been started and terminated.
     * Used only by the recovery method in scheduler core.
     *
     * @param id the id of the task to start and terminate.
     */
    public void simulateStartAndTerminate(TaskId id) {
        logger_dev.debug(" ");
        getJobDescriptor().start(id);
        getJobDescriptor().terminate(id);
    }

    /**
     * Failed this job due to the given task failure or job has been killed
     *
     * @param taskId the task that has been the cause to failure. Can be null if the job has been killed
     * @param jobStatus type of the failure on this job. (failed/canceled/killed)
     */
    public void failed(TaskId taskId, JobStatus jobStatus) {
        logger_dev.debug(" ");
        if (jobStatus != JobStatus.KILLED) {
            InternalTask descriptor = tasks.get(taskId);
            if (descriptor.getStartTime() > 0) {
                descriptor.setFinishedTime(System.currentTimeMillis());
                setNumberOfFinishedTasks(getNumberOfFinishedTasks() + 1);
            }
            descriptor.setStatus((jobStatus == JobStatus.FAILED) ? TaskStatus.FAILED : TaskStatus.FAULTY);
            //terminate this job descriptor
            getJobDescriptor().failed();
        }
        //set the new status of the job
        setFinishedTime(System.currentTimeMillis());
        setNumberOfPendingTasks(0);
        setNumberOfRunningTasks(0);
        setStatus(jobStatus);

        //creating list of status
        HashMap<TaskId, TaskStatus> hts = new HashMap<TaskId, TaskStatus>();
        HashMap<TaskId, Long> htl = new HashMap<TaskId, Long>();

        for (InternalTask td : tasks.values()) {
            if (!td.getId().equals(taskId)) {
                if (td.getStatus() == TaskStatus.RUNNING) {
                    td.setStatus(TaskStatus.ABORTED);
                    td.setFinishedTime(System.currentTimeMillis());
                } else if (td.getStatus() == TaskStatus.WAITING_ON_ERROR ||
                    td.getStatus() == TaskStatus.WAITING_ON_FAILURE) {
                    td.setStatus(TaskStatus.NOT_RESTARTED);
                } else if (td.getStatus() != TaskStatus.FINISHED && td.getStatus() != TaskStatus.FAILED &&
                    td.getStatus() != TaskStatus.FAULTY && td.getStatus() != TaskStatus.SKIPPED) {
                    td.setStatus(TaskStatus.NOT_STARTED);
                }
            }

            htl.put(td.getId(), td.getFinishedTime());
            hts.put(td.getId(), td.getStatus());
        }

        setTaskStatusModify(hts);
        setTaskFinishedTimeModify(htl);

        if (jobDataSpaceApplication != null) {
            jobDataSpaceApplication.terminateDataSpaceApplication();
        }
    }

    /**
     * Get a task descriptor that is in the running task queue.
     * 
     * @param id the id of the task descriptor to retrieve.
     * @return the task descriptor associated to this id, or null if not running.
     */
    public TaskDescriptor getRunningTaskDescriptor(TaskId id) {
        return getJobDescriptor().GetRunningTaskDescriptor(id);
    }

    /**
     * Set all properties following a job submitting.
     */
    public void submitAction() {
        logger_dev.debug(" ");
        setSubmittedTime(System.currentTimeMillis());
        setStatus(JobStatus.PENDING);
    }

    /**
     * Prepare tasks in order to be ready to be scheduled.
     * The task may have a consistent id and job info.
     */
    public synchronized void prepareTasks() {
        logger_dev.debug(" ");
        //get tasks
        ArrayList<InternalTask> sorted = getITasks();
        //re-init taskId count
        TaskIdImpl.initialize();
        //sort task according to the ID
        Collections.sort(sorted);
        tasks.clear();
        for (InternalTask td : sorted) {
            TaskId newId = TaskIdImpl.nextId(getId(), td.getName());
            td.setId(newId);
            td.setJobInfo(getJobInfo());
            tasks.put(newId, td);
        }
    }

    /**
     * Set all properties in order to start the job.
     * After this method and for better performances you may have to
     * set the taskStatusModify to "null" : setTaskStatusModify(null);
     */
    public void start() {
        logger_dev.debug(" ");
        setStartTime(System.currentTimeMillis());
        setNumberOfPendingTasks(getTotalNumberOfTasks());
        setNumberOfRunningTasks(0);
        setStatus(JobStatus.RUNNING);

        HashMap<TaskId, TaskStatus> taskStatus = new HashMap<TaskId, TaskStatus>();

        for (InternalTask td : getITasks()) {
            td.setStatus(TaskStatus.PENDING);
            taskStatus.put(td.getId(), TaskStatus.PENDING);
        }

        setTaskStatusModify(taskStatus);
    }

    /**
     * Set all properties in order to terminate the job.
     */
    public void terminate() {
        logger_dev.debug(" ");
        setStatus(JobStatus.FINISHED);
        setFinishedTime(System.currentTimeMillis());
        if (jobDataSpaceApplication != null) {
            if (!TaskLauncher.logger_dev_dataspace.isDebugEnabled()) {
                jobDataSpaceApplication.terminateDataSpaceApplication();
            }
        }
    }

    /**
     * Paused every running and submitted tasks in this pending job.
     * After this method and for better performances you may have to
     * set the taskStatusModify to "null" : setTaskStatusModify(null);
     * 
     * @return true if the job has correctly been paused, false if not.
     */
    public boolean setPaused() {
        logger_dev.debug(" ");
        if (jobInfo.getStatus() == JobStatus.PAUSED) {
            return false;
        }

        jobInfo.setStatus(JobStatus.PAUSED);

        HashMap<TaskId, TaskStatus> hts = new HashMap<TaskId, TaskStatus>();

        for (InternalTask td : tasks.values()) {
            if ((td.getStatus() != TaskStatus.FINISHED) && (td.getStatus() != TaskStatus.RUNNING) &&
                (td.getStatus() != TaskStatus.SKIPPED) && (td.getStatus() != TaskStatus.FAULTY)) {
                td.setStatus(TaskStatus.PAUSED);
            }

            hts.put(td.getId(), td.getStatus());
        }

        getJobDescriptor().update(hts);
        setTaskStatusModify(hts);

        return true;
    }

    /**
     * Status of every paused tasks becomes pending or submitted in this pending job.
     * After this method and for better performances you may have to
     * set the taskStatusModify to "null" : setTaskStatusModify(null);
     * 
     * @return true if the job has correctly been unpaused, false if not.
     */
    public boolean setUnPause() {
        logger_dev.debug(" ");
        if (jobInfo.getStatus() != JobStatus.PAUSED) {
            return false;
        }

        if ((getNumberOfPendingTasks() + getNumberOfRunningTasks() + getNumberOfFinishedTasks()) == 0) {
            jobInfo.setStatus(JobStatus.PENDING);
        } else if (getNumberOfRunningTasks() == 0) {
            jobInfo.setStatus(JobStatus.STALLED);
        } else {
            jobInfo.setStatus(JobStatus.RUNNING);
        }

        HashMap<TaskId, TaskStatus> hts = new HashMap<TaskId, TaskStatus>();

        for (InternalTask td : tasks.values()) {
            if (jobInfo.getStatus() == JobStatus.PENDING) {
                td.setStatus(TaskStatus.SUBMITTED);
            } else if ((jobInfo.getStatus() == JobStatus.RUNNING) ||
                (jobInfo.getStatus() == JobStatus.STALLED)) {
                if ((td.getStatus() != TaskStatus.FINISHED) && (td.getStatus() != TaskStatus.RUNNING) &&
                    (td.getStatus() != TaskStatus.SKIPPED) && (td.getStatus() != TaskStatus.FAULTY)) {
                    td.setStatus(TaskStatus.PENDING);
                }
            }

            hts.put(td.getId(), td.getStatus());
        }

        getJobDescriptor().update(hts);
        setTaskStatusModify(hts);

        return true;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.Job#setPriority(org.ow2.proactive.scheduler.common.job.JobPriority)
     */
    @Override
    public void setPriority(JobPriority priority) {
        jobInfo.setPriority(priority);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.JobState#getTasks()
     */
    @Override
    public ArrayList<TaskState> getTasks() {
        return new ArrayList<TaskState>(tasks.values());
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.JobState#getHMTasks()
     */
    @Override
    public Map<TaskId, TaskState> getHMTasks() {
        Map<TaskId, TaskState> tmp = new HashMap<TaskId, TaskState>();
        for (Entry<TaskId, InternalTask> e : tasks.entrySet()) {
            tmp.put(e.getKey(), e.getValue());
        }
        return tmp;
    }

    /**
     * To get the tasks as an array list.
     *
     * @return the tasks
     */
    public ArrayList<InternalTask> getITasks() {
        return new ArrayList<InternalTask>(tasks.values());
    }

    /**
     * To get the tasks as a hash map.
     *
     * @return the tasks
     */
    public Map<TaskId, InternalTask> getIHMTasks() {
        return tasks;
    }

    /**
     * To set the taskStatusModify
     *
     * @param taskStatusModify the taskStatusModify to set
     */
    public void setTaskStatusModify(Map<TaskId, TaskStatus> taskStatusModify) {
        jobInfo.setTaskStatusModify(taskStatusModify);
    }

    /**
     * To set the taskFinishedTimeModify
     *
     * @param taskFinishedTimeModify the taskFinishedTimeModify to set
     */
    public void setTaskFinishedTimeModify(Map<TaskId, Long> taskFinishedTimeModify) {
        jobInfo.setTaskFinishedTimeModify(taskFinishedTimeModify);
    }

    /**
     * To set the tasksReplicated
     *
     * @param d tasksReplicated
     */
    public void setReplicatedTasksModify(List<ReplicatedTask> d) {
        jobInfo.setTasksReplicated(d);
    }

    /**
     * To set the tasksLooped
     *
     * @param d tasksLooped
     */
    public void setLoopedTasksModify(List<ReplicatedTask> d) {
        jobInfo.setTasksLooped(d);
    }

    /**
     * To set the tasksSkipped
     *
     * @param d tasksSkipped
     */
    public void setSkippedTasksModify(List<TaskId> d) {
        jobInfo.setTasksSkipped(d);
    }

    /**
     * To set the id
     *
     * @param id the id to set
     */
    public void setId(JobId id) {
        jobInfo.setJobId(id);
    }

    /**
     * To set the finishedTime
     *
     * @param finishedTime
     *            the finishedTime to set
     */
    public void setFinishedTime(long finishedTime) {
        jobInfo.setFinishedTime(finishedTime);
    }

    /**
     * To set the startTime
     *
     * @param startTime
     *            the startTime to set
     */
    public void setStartTime(long startTime) {
        jobInfo.setStartTime(startTime);
    }

    /**
     * To set the submittedTime
     *
     * @param submittedTime
     *            the submittedTime to set
     */
    public void setSubmittedTime(long submittedTime) {
        jobInfo.setSubmittedTime(submittedTime);
    }

    /**
     * To set the removedTime
     *
     * @param removedTime
     *            the removedTime to set
     */
    public void setRemovedTime(long removedTime) {
        jobInfo.setRemovedTime(removedTime);
    }

    /**
     * To set the numberOfFinishedTasks
     *
     * @param numberOfFinishedTasks the numberOfFinishedTasks to set
     */
    public void setNumberOfFinishedTasks(int numberOfFinishedTasks) {
        jobInfo.setNumberOfFinishedTasks(numberOfFinishedTasks);
    }

    /**
     * To set the numberOfPendingTasks
     *
     * @param numberOfPendingTasks the numberOfPendingTasks to set
     */
    public void setNumberOfPendingTasks(int numberOfPendingTasks) {
        jobInfo.setNumberOfPendingTasks(numberOfPendingTasks);
    }

    /**
     * To set the numberOfRunningTasks
     *
     * @param numberOfRunningTasks the numberOfRunningTasks to set
     */
    public void setNumberOfRunningTasks(int numberOfRunningTasks) {
        jobInfo.setNumberOfRunningTasks(numberOfRunningTasks);
    }

    /**
     * To get the jobDescriptor
     *
     * @return the jobDescriptor
     */
    @XmlTransient
    public JobDescriptorImpl getJobDescriptor() {
        if (jobDescriptor == null) {
            jobDescriptor = new JobDescriptorImpl(this);
        }
        return (JobDescriptorImpl) jobDescriptor;
    }

    /**
     * Set the job Descriptor
     *
     * @param jobD the JobDescriptor to set.
     */
    public void setJobDescriptor(JobDescriptor jobD) {
        this.jobDescriptor = jobD;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        jobInfo.setStatus(status);
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.JobState#getOwner()
     */
    @Override
    public String getOwner() {
        return owner;
    }

    /**
     * To set the owner of this job.
     *
     * @param owner the owner to set.
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @see org.ow2.proactive.scheduler.common.job.JobState#getJobResult()
     */
    public JobResult getJobResult() {
        return jobResult;
    }

    /**
     * Sets the jobResult to the given jobResult value.
     *
     * @param jobResult the jobResult to set.
     */
    public void setJobResult(JobResult jobResult) {
        this.jobResult = jobResult;
    }

    /**
     * Get the credentials for this job
     *
     * @return the credentials for this job
     */
    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Set the credentials value to the given credentials value
     *
     * @param credentials the credentials to set
     */
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    /**
     * Get the next restart waiting time in millis.
     * 
     * @return the next restart waiting time in millis.
     */
    public long getNextWaitingTime(int executionNumber) {
        if (executionNumber <= 0) {
            //execution number is 0 or less, restart with the minimal amount of time
            return restartWaitingTimer;
        } else if (executionNumber > 10) {
            //execution timer exceed 10, restart after 60 seconds
            return 60 * 1000;
        } else {
            //else restart according to this function
            return (getNextWaitingTime(executionNumber - 1) + executionNumber * 1000);
        }
    }

    /**
     * Set this job to the state toBeRemoved.
     */
    public void setToBeRemoved() {
        jobInfo.setToBeRemoved();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof InternalJob) {
            return getId().equals(((InternalJob) o).getId());
        }

        return false;
    }

    /**
     * Get the jobDataSpaceApplication
     *
     * @return the jobDataSpaceApplication
     */
    public JobDataSpaceApplication getJobDataSpaceApplication() {
        return jobDataSpaceApplication;
    }

    @Transient
    private transient Map<String, InternalTask> tasknameITaskMapping = null;

    /**
     * Return the internal task associated with the given task name for this job.
     *
     * @param taskName the task name to find
     * @return the internal task associated with the given name.
     * @throws UnknownTaskException if the given taskName does not exist.
     */
    public InternalTask getTask(String taskName) throws UnknownTaskException {
        if (tasknameITaskMapping == null) {
            tasknameITaskMapping = new HashMap<String, InternalTask>(tasks.size());
            for (InternalTask it : tasks.values()) {
                tasknameITaskMapping.put(it.getId().getReadableName(), it);
            }
        }
        if (tasknameITaskMapping.containsKey(taskName)) {
            return tasknameITaskMapping.get(taskName);
        } else {
            throw new UnknownTaskException("'" + taskName + "' does not exist in this job.");
        }
    }

    //********************************************************************
    //************************* SERIALIZATION ****************************
    //********************************************************************

    /**
     * <b>IMPORTANT : </b><br />
     * Using hibernate does not allow to have java transient fields that is inserted in database anyway.<br />
     * Hibernate defined @Transient annotation meaning the field won't be inserted in database.
     * If the java transient modifier is set for a field, so the hibernate @Transient annotation becomes
     * useless and the field won't be inserted in DB anyway.<br />
     * For performance reason, some field must be java transient but not hibernate transient.
     * These fields are annotated with @TransientInSerialization.
     * The @TransientInSerialization describe the fields that won't be serialized by java since the two following
     * methods describe the serialization process.
     */

    private void writeObject(ObjectOutputStream out) throws IOException {
        try {
            Map<String, Object> toSerialize = new HashMap<String, Object>();
            Field[] fields = InternalJob.class.getDeclaredFields();
            for (Field f : fields) {
                if (!f.isAnnotationPresent(TransientInSerialization.class) &&
                    !Modifier.isStatic(f.getModifiers())) {
                    toSerialize.put(f.getName(), f.get(this));
                }
            }
            out.writeObject(toSerialize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            Map<String, Object> map = (Map<String, Object>) in.readObject();
            for (Entry<String, Object> e : map.entrySet()) {
                InternalJob.class.getDeclaredField(e.getKey()).set(this, e.getValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
