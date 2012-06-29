package org.ow2.proactive.scheduler.core.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.junit.Test;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.ow2.proactive.db.DatabaseManager.FilteredExceptionCallback;
import org.ow2.proactive.db.DatabaseManagerExceptionHandler.DBMEHandler;
import org.ow2.proactive.db.DatabaseManagerException;
import org.ow2.proactive.db.DatabaseManagerExceptionHandler;
import org.ow2.proactive.scheduler.common.job.JobEnvironment;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobPriority;
import org.ow2.proactive.scheduler.common.job.JobResult;
import org.ow2.proactive.scheduler.common.job.JobStatus;
import org.ow2.proactive.scheduler.common.task.Task;
import org.ow2.proactive.scheduler.common.task.TaskId;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.TaskState;
import org.ow2.proactive.scheduler.common.task.TaskStatus;
import org.ow2.proactive.scheduler.common.task.dataspaces.InputSelector;
import org.ow2.proactive.scheduler.common.task.dataspaces.OutputSelector;
import org.ow2.proactive.scheduler.common.util.SchedulerLoggers;
import org.ow2.proactive.scheduler.core.account.SchedulerAccount;
import org.ow2.proactive.scheduler.core.db.TaskData.DBTaskId;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.job.InternalJob;
import org.ow2.proactive.scheduler.job.JobIdImpl;
import org.ow2.proactive.scheduler.job.JobResultImpl;
import org.ow2.proactive.scheduler.task.ExecutableContainer;
import org.ow2.proactive.scheduler.task.ForkedJavaExecutableContainer;
import org.ow2.proactive.scheduler.task.JavaExecutableContainer;
import org.ow2.proactive.scheduler.task.NativeExecutableContainer;
import org.ow2.proactive.scheduler.task.TaskIdImpl;
import org.ow2.proactive.scheduler.task.TaskResultImpl;
import org.ow2.proactive.scheduler.task.internal.InternalForkedJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalJavaTask;
import org.ow2.proactive.scheduler.task.internal.InternalNativeTask;
import org.ow2.proactive.scheduler.task.internal.InternalTask;
import org.ow2.proactive.scheduler.util.SchedulerDevLoggers;
import org.ow2.proactive.scripting.InvalidScriptException;


public class SchedulerDBManager implements FilteredExceptionCallback {

    private static final String JAVA_PROPERTYNAME_NODB = "scheduler.database.nodb";

    private static final Logger logger = ProActiveLogger.getLogger(SchedulerLoggers.DATABASE);

    private static final Logger debugLogger = ProActiveLogger.getLogger(SchedulerDevLoggers.DATABASE);

    private static final JobStatus[] finishedJobStatuses = { JobStatus.CANCELED, JobStatus.FAILED,
            JobStatus.KILLED, JobStatus.FINISHED };

    private static final JobStatus[] notFinishedJobStatuses = { JobStatus.PAUSED, JobStatus.PENDING,
            JobStatus.STALLED, JobStatus.RUNNING };

    private final SessionFactory sessionFactory;

    private final DatabaseManagerExceptionHandler exceptionHandler;

    private static abstract class SessionWork<T> {

        abstract T executeWork(Session session);

    }

    public synchronized static SchedulerDBManager createUsingProperties() {
        if (System.getProperty(JAVA_PROPERTYNAME_NODB) != null) {
            return createInMemorySchedulerDBManager();
        } else {
            File configFile = new File(PASchedulerProperties
                    .getAbsolutePath(PASchedulerProperties.SCHEDULER_DB_HIBERNATE_CONFIG.getValueAsString()));

            boolean drop = PASchedulerProperties.SCHEDULER_DB_HIBERNATE_DROPDB.getValueAsBoolean();

            logger.info("Initializing Scheduler DB using Hibernate config " + configFile.getAbsolutePath());

            return new SchedulerDBManager(new Configuration().configure(configFile), drop);
        }
    }

    public static SchedulerDBManager createInMemorySchedulerDBManager() {
        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        config.setProperty("hibernate.connection.url", "jdbc:h2:mem:scheduler");
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        return new SchedulerDBManager(config, true);
    }

    public SchedulerDBManager(Configuration configuration, boolean drop) {
        logger.info("Starting Hibernate...");
        logger.info("Drop DB : " + drop);
        try {
            configuration.addAnnotatedClass(JobData.class);
            configuration.addAnnotatedClass(TaskData.class);
            configuration.addAnnotatedClass(TaskResultData.class);
            configuration.addAnnotatedClass(JobClasspathContent.class);
            configuration.addAnnotatedClass(JavaTaskData.class);
            configuration.addAnnotatedClass(ForkedJavaTaskData.class);
            configuration.addAnnotatedClass(NativeTaskData.class);
            configuration.addAnnotatedClass(ScriptData.class);
            configuration.addAnnotatedClass(EnvironmentModifierData.class);
            configuration.addAnnotatedClass(SelectorData.class);
            if (drop) {
                configuration.setProperty("hibernate.hbm2ddl.auto", "create");
            }

            configuration.setProperty("hibernate.id.new_generator_mappings", "true");
            configuration.setProperty("hibernate.jdbc.use_streams_for_binary", "true");

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) {
            debugLogger.error("Initial SessionFactory creation failed", ex);
            throw new DatabaseManagerException("Initial SessionFactory creation failed", ex);
        }

        this.exceptionHandler = new DatabaseManagerExceptionHandler(
            new Class[] { org.hibernate.exception.JDBCConnectionException.class }, DBMEHandler.FILTER_ALL,
            this);
    }

    @Override
    public void notify(DatabaseManagerException dme) {
        if (this.callback != null) {
            this.callback.notify(dme);
        }
        throw dme;
    }

    private FilteredExceptionCallback callback;

    public void setCallback(FilteredExceptionCallback callback) {
        this.callback = callback;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void close() {
        try {
            if (sessionFactory != null) {
                debugLogger.info("Closing session factory");
                sessionFactory.close();
            }
        } catch (Exception e) {
            debugLogger.error("Error while closing database", e);
        }
    }

    public SchedulerAccount readAccount(final String username) {
        return runWithoutTransaction(new SessionWork<SchedulerAccount>() {

            @Override
            SchedulerAccount executeWork(Session session) {
                Query tasksQuery = session.createQuery(
                        "select count(*), sum(task.finishedTime) - sum(task.startTime) from TaskData task "
                            + "where task.finishedTime > 0 and task.jobData.owner = :username").setParameter(
                        "username", username);

                int taskCount;
                long taskDuration;

                Object[] taskResult = (Object[]) tasksQuery.uniqueResult();
                taskCount = ((Number) taskResult[0]).intValue();
                if (taskResult[1] != null) {
                    taskDuration = ((Number) taskResult[1]).longValue();
                } else {
                    taskDuration = 0;
                }

                int jobCount;
                long jobDuration;

                Query jobQuery = session.createQuery(
                        "select count(*), sum(finishedTime) - sum(startTime) from JobData"
                            + " where owner = :username and finishedTime > 0").setParameter("username",
                        username);

                Object[] jobResult = (Object[]) jobQuery.uniqueResult();
                jobCount = ((Number) jobResult[0]).intValue();
                if (jobResult[1] != null) {
                    jobDuration = ((Number) jobResult[1]).longValue();
                } else {
                    jobDuration = 0;
                }

                return new SchedulerAccount(username, taskCount, taskDuration, jobCount, jobDuration);
            }

        });
    }

    public void removeJob(final JobId jobId, final long removedTime, final boolean removeData) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                long id = jobId(jobId);

                if (removeData) {
                    session.createSQLQuery("delete from TASK_DATA_DEPENDENCIES where JOB_ID = :jobId")
                            .setParameter("jobId", id).executeUpdate();
                    session.createSQLQuery("delete from TASK_DATA_JOINED_BRANCHES where JOB_ID = :jobId")
                            .setParameter("jobId", id).executeUpdate();

                    session
                            .createQuery(
                                    "delete from ScriptData where id in (select td.envScript from ForkedJavaTaskData td where td.taskData.id.jobId = :jobId)")
                            .setParameter("jobId", id).executeUpdate();
                    session
                            .createQuery(
                                    "delete from ScriptData where id in (select td.generationScript from NativeTaskData td where td.taskData.id.jobId = :jobId)")
                            .setParameter("jobId", id).executeUpdate();

                    session
                            .createQuery(
                                    "delete from ScriptData where id in (select preScript from TaskData where id.jobId = :jobId)"
                                        + "or id in (select postScript from TaskData where id.jobId = :jobId) or id in (select cleanScript from TaskData where id.jobId = :jobId) or id in (select flowScript from TaskData where id.jobId = :jobId)")
                            .setParameter("jobId", id).executeUpdate();

                    session.createQuery("delete from JobData where id = :jobId").setParameter("jobId", id)
                            .executeUpdate();
                } else {
                    String jobUpdate = "update JobData set removedTime = :removedTime where id = :jobId";
                    session.createQuery(jobUpdate).setParameter("removedTime", removedTime).setParameter(
                            "jobId", id).executeUpdate();
                }

                return null;
            }

        });
    }

    public List<InternalJob> loadNotFinishedJobs(boolean fullState) {
        return loadJobs(fullState, notFinishedJobStatuses);
    }

    public List<InternalJob> loadFinishedJobs(boolean fullState) {
        return loadJobs(fullState, finishedJobStatuses);
    }

    private List<InternalJob> loadJobs(final boolean fullState, final JobStatus... status) {
        return runWithoutTransaction(new SessionWork<List<InternalJob>>() {
            @Override
            @SuppressWarnings("unchecked")
            List<InternalJob> executeWork(Session session) {
                List<Long> ids = session.createQuery(
                        "select id from JobData where status in (:status) and removedTime = -1")
                        .setParameterList("status", status).list();

                return loadInternalJobs(fullState, session, ids);
            }

        });
    }

    public InternalJob loadJobWithoutTasks(final JobId id) {
        return runWithoutTransaction(new SessionWork<InternalJob>() {
            @Override
            InternalJob executeWork(Session session) {
                JobData jobData = (JobData) session.get(JobData.class, jobId(id));
                if (jobData == null) {
                    return null;
                } else {
                    return jobData.toInternalJob();
                }
            }

        });
    }

    public List<InternalJob> loadJobs(final boolean fullState, final JobId... jobIds) {
        return runWithoutTransaction(new SessionWork<List<InternalJob>>() {
            @Override
            List<InternalJob> executeWork(Session session) {
                List<Long> ids = new ArrayList<Long>(jobIds.length);
                for (JobId jobId : jobIds) {
                    ids.add(jobId(jobId));
                }
                return loadInternalJobs(fullState, session, ids);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<TaskData>> loadJobsTasks(Session session, List<Long> jobIds) {
        Query tasksQuery = session
                .createQuery(
                        "from TaskData as task left outer join fetch task.dependentTasks where task.id.jobId in (:ids)")
                .setParameterList("ids", jobIds).setResultTransformer(
                        DistinctRootEntityResultTransformer.INSTANCE);

        Map<Long, List<TaskData>> tasksMap = new HashMap<Long, List<TaskData>>(jobIds.size());
        for (Long id : jobIds) {
            tasksMap.put(id, new ArrayList<TaskData>());
        }

        List<TaskData> tasks = tasksQuery.list();
        for (TaskData task : tasks) {
            tasksMap.get(task.getJobData().getId()).add(task);
        }

        return tasksMap;
    }

    private List<InternalJob> loadInternalJobs(boolean fullState, Session session, List<Long> ids) {
        Query jobQuery = session.createQuery("from JobData as job where job.id in (:ids)");

        List<InternalJob> result = new ArrayList<InternalJob>(ids.size());

        final int BATCH_SIZE = 100;

        List<Long> batchLoadIds = new ArrayList<Long>(BATCH_SIZE);

        for (Long id : ids) {
            batchLoadIds.add(id);
            if (batchLoadIds.size() == BATCH_SIZE) {
                batchLoadJobs(session, fullState, jobQuery, batchLoadIds, result);
                batchLoadIds.clear();
                session.clear();
            }
        }
        if (!batchLoadIds.isEmpty()) {
            batchLoadJobs(session, fullState, jobQuery, batchLoadIds, result);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void batchLoadJobs(Session session, boolean fullState, Query jobQuery, List<Long> ids,
            Collection<InternalJob> jobs) {
        Map<Long, List<TaskData>> tasksMap = loadJobsTasks(session, ids);

        jobQuery.setParameterList("ids", ids);
        List<JobData> jobsList = (List<JobData>) jobQuery.list();

        for (JobData jobData : jobsList) {
            InternalJob internalJob = jobData.toInternalJob();
            if (fullState) {
                String[] classpath = jobData.getClasspath();
                JobEnvironment env = new JobEnvironment(classpath, null, false, jobData.getClasspathCrc());
                internalJob.setEnvironment(env);
            }
            internalJob.setTasks(toInternalTasks(fullState, internalJob, tasksMap.get(jobData.getId())));

            jobs.add(internalJob);
        }
    }

    private Collection<InternalTask> toInternalTasks(boolean loadFullState, InternalJob internalJob,
            List<TaskData> taskRuntimeDataList) {
        Map<DBTaskId, InternalTask> tasks = new HashMap<DBTaskId, InternalTask>(taskRuntimeDataList.size());

        try {
            for (TaskData taskData : taskRuntimeDataList) {
                InternalTask internalTask = taskData.toInternalTask(internalJob);
                if (loadFullState) {
                    internalTask.setParallelEnvironment(taskData.getParallelEnvironment());
                    internalTask.setGenericInformations(taskData.getGenericInformation());
                    for (ScriptData scriptData : taskData.getSelectionScripts()) {
                        internalTask.addSelectionScript(scriptData.createSelectionScript());
                    }
                    if (taskData.getCleanScript() != null) {
                        internalTask.setCleaningScript(taskData.getCleanScript().createSimpleScript());
                    }
                    if (taskData.getPreScript() != null) {
                        internalTask.setPreScript(taskData.getPreScript().createSimpleScript());
                    }
                    if (taskData.getPostScript() != null) {
                        internalTask.setPostScript(taskData.getPostScript().createSimpleScript());
                    }
                    if (taskData.getFlowScript() != null) {
                        internalTask.setFlowScript(taskData.getFlowScript().createFlowScript());
                    }
                    for (SelectorData selectorData : taskData.getDataspaceSelectors()) {
                        if (selectorData.isInput()) {
                            InputSelector selector = selectorData.createInputSelector();
                            internalTask.addInputFiles(selector.getInputFiles(), selector.getMode());
                        } else {
                            OutputSelector selector = selectorData.createOutputSelector();
                            internalTask.addOutputFiles(selector.getOutputFiles(), selector.getMode());
                        }
                    }
                }
                tasks.put(taskData.getId(), internalTask);
            }
        } catch (InvalidScriptException e) {
            throw new DatabaseManagerException("Failed to initialize loaded script", e);
        }

        for (TaskData taskData : taskRuntimeDataList) {
            InternalTask internalTask = tasks.get(taskData.getId());
            if (!taskData.getDependentTasks().isEmpty()) {
                for (DBTaskId dependent : taskData.getDependentTasks()) {
                    internalTask.addDependence(tasks.get(dependent));
                }
            }
            if (loadFullState) {
                if (taskData.getIfBranch() != null) {
                    internalTask.setIfBranch(tasks.get(taskData.getIfBranch().getId()));
                }
                if (!taskData.getJoinedBranches().isEmpty()) {
                    List<InternalTask> branches = new ArrayList<InternalTask>(taskData.getJoinedBranches()
                            .size());
                    for (DBTaskId joinedBranch : taskData.getJoinedBranches()) {
                        branches.add(tasks.get(joinedBranch));
                    }
                    internalTask.setJoinedBranches(branches);
                }
                internalTask.setName(internalTask.getName());
            }
        }

        return tasks.values();
    }

    public void changeJobPriority(final JobId jobId, final JobPriority priority) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                long id = jobId(jobId);
                String jobUpdate = "update JobData set priority = :priority where id = :jobId";
                session.createQuery(jobUpdate).setParameter("priority", priority).setParameter("jobId", id)
                        .executeUpdate();
                return null;
            }
        });
    }

    public void jobTaskStarted(final InternalJob job, final InternalTask task,
            final boolean taskStatusToPending) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                long jobId = jobId(job);

                String jobUpdate = "update JobData set status = :status, "
                    + "startTime = :startTime, numberOfPendingTasks = :numberOfPendingTasks, "
                    + "numberOfRunningTasks = :numberOfRunningTasks where id = :jobId";

                JobInfo jobInfo = job.getJobInfo();

                session.createQuery(jobUpdate).setParameter("status", jobInfo.getStatus()).setParameter(
                        "startTime", jobInfo.getStartTime()).setParameter("numberOfPendingTasks",
                        jobInfo.getNumberOfPendingTasks()).setParameter("numberOfRunningTasks",
                        jobInfo.getNumberOfRunningTasks()).setParameter("jobId", jobId).executeUpdate();

                if (taskStatusToPending) {
                    JobData job = (JobData) session.load(JobData.class, jobId);
                    String taskStatusUpdate = "update TaskData task set task.taskStatus = :taskStatus "
                        + "where task.jobData = :job";
                    session.createQuery(taskStatusUpdate).setParameter("taskStatus", TaskStatus.PENDING)
                            .setParameter("job", job).executeUpdate();
                }

                TaskData.DBTaskId taskId = taskId(task);

                String taskUpdate = "update TaskData task set task.taskStatus = :taskStatus, "
                    + "task.startTime = :startTime, task.finishedTime = :finishedTime, "
                    + "task.executionHostName = :executionHostName where task.id = :taskId";

                TaskInfo taskInfo = task.getTaskInfo();

                session.createQuery(taskUpdate).setParameter("taskStatus", taskInfo.getStatus())
                        .setParameter("startTime", taskInfo.getStartTime()).setParameter("finishedTime",
                                taskInfo.getFinishedTime()).setParameter("executionHostName",
                                taskInfo.getExecutionHostName()).setParameter("taskId", taskId)
                        .executeUpdate();

                return null;
            }

        });
    }

    @Test
    public void taskRestarted(final InternalJob job, final InternalTask task, final TaskResultImpl result) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                long jobId = jobId(job);

                String jobUpdate = "update JobData set status = :status, "
                    + "numberOfPendingTasks = :numberOfPendingTasks, "
                    + "numberOfRunningTasks = :numberOfRunningTasks where id = :jobId";

                JobInfo jobInfo = job.getJobInfo();

                session.createQuery(jobUpdate).setParameter("status", jobInfo.getStatus()).setParameter(
                        "numberOfPendingTasks", jobInfo.getNumberOfPendingTasks()).setParameter(
                        "numberOfRunningTasks", jobInfo.getNumberOfRunningTasks()).setParameter("jobId",
                        jobId).executeUpdate();

                TaskData.DBTaskId taskId = taskId(task);

                String taskUpdate = "update TaskData set taskStatus = :taskStatus, "
                    + "numberOfExecutionLeft = :numberOfExecutionLeft,"
                    + "numberOfExecutionOnFailureLeft = :numberOfExecutionOnFailureLeft"
                    + " where id = :taskId";

                TaskInfo taskInfo = task.getTaskInfo();

                session.createQuery(taskUpdate).setParameter("taskStatus", taskInfo.getStatus())
                        .setParameter("numberOfExecutionLeft", taskInfo.getNumberOfExecutionLeft())
                        .setParameter("numberOfExecutionOnFailureLeft",
                                taskInfo.getNumberOfExecutionOnFailureLeft()).setParameter("taskId", taskId)
                        .executeUpdate();

                if (result != null) {
                    saveTaskResult(taskId, result, session);
                }

                return null;
            }

        });
    }

    public void updateAfterWorkflowTaskFinished(final InternalJob job, final InternalTask finishedTask,
            final TaskResultImpl result) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                String jobUpdate = "update JobData set status = :status, "
                    + "finishedTime = :finishedTime, numberOfPendingTasks = :numberOfPendingTasks, "
                    + "numberOfFinishedTasks = :numberOfFinishedTasks, "
                    + "numberOfRunningTasks = :numberOfRunningTasks, "
                    + "totalNumberOfTasks =:totalNumberOfTasks where id = :jobId";

                JobInfo jobInfo = job.getJobInfo();

                session.createQuery(jobUpdate).setParameter("status", jobInfo.getStatus()).setParameter(
                        "finishedTime", jobInfo.getFinishedTime()).setParameter("numberOfPendingTasks",
                        jobInfo.getNumberOfPendingTasks()).setParameter("numberOfFinishedTasks",
                        jobInfo.getNumberOfFinishedTasks()).setParameter("numberOfRunningTasks",
                        jobInfo.getNumberOfRunningTasks()).setParameter("totalNumberOfTasks",
                        jobInfo.getTotalNumberOfTasks()).setParameter("jobId", jobId(job)).executeUpdate();

                JobData jobRuntimeData = (JobData) session.load(JobData.class, jobId(job));

                List<TaskData> taskRuntimeDataList = new ArrayList<TaskData>();

                List<InternalTask> tasks = new ArrayList<InternalTask>();
                List<InternalTask> newTasks = new ArrayList<InternalTask>();

                for (InternalTask task : job.getITasks()) {
                    TaskData taskData = (TaskData) session.get(TaskData.class, taskId(task));
                    if (taskData == null) {
                        newTasks.add(task);
                    } else {
                        taskData.updateMutableAttributes(task);
                        session.update(taskData);
                        taskRuntimeDataList.add(taskData);
                        tasks.add(task);
                    }
                }

                for (InternalTask task : newTasks) {
                    if (task.getExecutableContainer() == null) {
                        InternalTask from = task.getReplicatedFrom();
                        ExecutableContainer container = from.getExecutableContainer();
                        if (container == null) {
                            container = loadExecutableContainer(session, from);
                        }
                        task.setExecutableContainer(container);
                    }
                    TaskData taskData = saveNewTask(session, jobRuntimeData, task);
                    taskRuntimeDataList.add(taskData);
                    tasks.add(task);
                }

                saveTaskDependencies(session, tasks, taskRuntimeDataList);

                TaskData.DBTaskId taskId = taskId(finishedTask.getId());
                saveTaskResult(taskId, result, session);

                return null;
            }

        }, false);
    }

    public void updateAfterJobKilled(final InternalJob job) {
        updateAfterTaskFinished(job, null, null);
    }

    public void updateJobAndTasksState(final InternalJob job) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                String taskUpdate = "update TaskData task set task.taskStatus = :taskStatus where task.id = :taskId";

                Query taskUpdateQuery = session.createQuery(taskUpdate);
                for (TaskState task : job.getTasks()) {
                    TaskInfo taskInfo = task.getTaskInfo();
                    taskUpdateQuery.setParameter("taskStatus", taskInfo.getStatus()).setParameter("taskId",
                            taskId(task.getId())).executeUpdate();
                }

                String jobUpdate = "update JobData set status = :status where id = :jobId";

                JobInfo jobInfo = job.getJobInfo();

                session.createQuery(jobUpdate).setParameter("status", jobInfo.getStatus()).setParameter(
                        "jobId", jobId(job)).executeUpdate();

                return null;
            }

        });
    }

    public void updateAfterTaskFinished(final InternalJob job, final InternalTask finishedTask,
            final TaskResultImpl result) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                String taskUpdate = "update TaskData task set task.taskStatus = :taskStatus, "
                    + "task.finishedTime = :finishedTime, " + "task.executionDuration = :executionDuration "
                    + "where task.id = :taskId and task.finishedTime < 0";

                Query taskUpdateQuery = session.createQuery(taskUpdate);

                for (TaskState task : job.getTasks()) {
                    TaskData.DBTaskId taskId = taskId(task.getId());

                    TaskInfo taskInfo = task.getTaskInfo();

                    taskUpdateQuery.setParameter("taskStatus", taskInfo.getStatus()).setParameter(
                            "finishedTime", taskInfo.getFinishedTime()).setParameter("executionDuration",
                            taskInfo.getExecutionDuration()).setParameter("taskId", taskId).executeUpdate();
                }

                long jobId = jobId(job);

                String jobUpdate = "update JobData set status = :status, "
                    + "finishedTime = :finishedTime, numberOfPendingTasks = :numberOfPendingTasks, "
                    + "numberOfFinishedTasks = :numberOfFinishedTasks, "
                    + "numberOfRunningTasks = :numberOfRunningTasks where id = :jobId";

                JobInfo jobInfo = job.getJobInfo();

                session.createQuery(jobUpdate).setParameter("status", jobInfo.getStatus()).setParameter(
                        "finishedTime", jobInfo.getFinishedTime()).setParameter("numberOfPendingTasks",
                        jobInfo.getNumberOfPendingTasks()).setParameter("numberOfFinishedTasks",
                        jobInfo.getNumberOfFinishedTasks()).setParameter("numberOfRunningTasks",
                        jobInfo.getNumberOfRunningTasks()).setParameter("jobId", jobId).executeUpdate();

                if (result != null) {
                    TaskData.DBTaskId taskId = taskId(finishedTask.getId());
                    saveTaskResult(taskId, result, session);
                }

                return null;
            }

        });
    }

    private TaskResultData saveTaskResult(TaskData.DBTaskId taskId, TaskResultImpl result, Session session) {
        TaskData taskRuntimeData = (TaskData) session.load(TaskData.class, taskId);

        TaskResultData resultData = TaskResultData.createTaskResultData(taskRuntimeData, result);
        session.save(resultData);

        return resultData;
    }

    public void jobSetToBeRemoved(final JobId jobId) {
        runWithTransaction(new SessionWork<Void>() {
            @Override
            Void executeWork(Session session) {
                long id = jobId(jobId);

                String jobUpdate = "update JobData set toBeRemoved = :toBeRemoved where id = :jobId";

                session.createQuery(jobUpdate).setParameter("toBeRemoved", true).setParameter("jobId", id)
                        .executeUpdate();

                return null;
            }
        });
    }

    public Map<TaskId, TaskResult> loadTasksResults(final JobId jobId, final List<TaskId> taskIds) {
        if (taskIds.isEmpty()) {
            throw new IllegalArgumentException("TaskIds list is empty");
        }

        return runWithoutTransaction(new SessionWork<Map<TaskId, TaskResult>>() {

            @Override
            Map<TaskId, TaskResult> executeWork(Session session) {
                JobData job = (JobData) session.get(JobData.class, jobId(jobId));

                if (job == null) {
                    throw new DatabaseManagerException("Invalid job id: " + jobId);
                }

                List<TaskData.DBTaskId> dbTaskIds = new ArrayList<TaskData.DBTaskId>(taskIds.size());
                for (TaskId taskId : taskIds) {
                    dbTaskIds.add(taskId(taskId));
                }

                Query query = session
                        .createQuery(
                                "select taskResult, "
                                    + "task.id, "
                                    + "task.taskName, "
                                    + "task.preciousResult from TaskResultData as taskResult join taskResult.taskRuntimeData as task "
                                    + "where task.id in (:tasksIds) order by task.id, taskResult.resultTime desc")
                        .setParameterList("tasksIds", dbTaskIds);

                JobResultImpl jobResult = loadJobResult(session, query, job, jobId);
                if (jobResult == null) {
                    throw new DatabaseManagerException("Failed to load result for tasks " + taskIds +
                        " (job: " + jobId + ")");
                }

                Map<TaskId, TaskResult> resultsMap = new HashMap<TaskId, TaskResult>(taskIds.size());
                for (TaskId taskId : taskIds) {
                    TaskResult taskResult = null;
                    for (TaskResult result : jobResult.getAllResults().values()) {
                        if (result.getTaskId().equals(taskId)) {
                            taskResult = result;
                            break;
                        }
                    }
                    if (taskResult == null) {
                        throw new DatabaseManagerException("Failed to load result for task " + taskId +
                            " (job: " + jobId + ")");
                    } else {
                        resultsMap.put(taskId, taskResult);
                    }
                }

                if (jobResult.getAllResults().size() != taskIds.size()) {
                    throw new DatabaseManagerException("Results: " + jobResult.getAllResults().size() + " " +
                        taskIds.size());
                }

                return resultsMap;
            }

        });

    }

    public JobResult loadJobResult(final JobId jobId) {
        return runWithoutTransaction(new SessionWork<JobResult>() {

            @Override
            JobResult executeWork(Session session) {
                long id = jobId(jobId);

                JobData job = (JobData) session.get(JobData.class, id);

                if (job == null) {
                    return null;
                }

                Query query = session
                        .createQuery(
                                "select taskResult, "
                                    + "task.id, "
                                    + "task.taskName, "
                                    + "task.preciousResult from TaskResultData as taskResult left outer join taskResult.taskRuntimeData as task "
                                    + "where task.jobData = :job order by task.id, taskResult.resultTime desc")
                        .setParameter("job", job);

                return loadJobResult(session, query, job, jobId);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private JobResultImpl loadJobResult(Session session, Query query, JobData job, JobId jobId) {
        JobResultImpl jobResult = new JobResultImpl();
        jobResult.setJobInfo(job.createJobInfo(jobId));

        DBTaskId currentTaskId = null;

        List<Object[]> resultList = (List<Object[]>) query.list();
        if (resultList.isEmpty()) {
            return jobResult;
        }

        String[] jobClasspath = job.getClasspath();
        int counter = 0;

        for (Object[] result : resultList) {
            TaskResultData resultData = (TaskResultData) result[0];
            DBTaskId dbTaskId = (DBTaskId) result[1];
            String taskName = (String) result[2];
            Boolean preciousResult = (Boolean) result[3];

            boolean nextTask = !dbTaskId.equals(currentTaskId);
            if (nextTask) {
                TaskId taskId = TaskIdImpl.createTaskId(jobId, taskName, dbTaskId.getTaskId(), false);
                jobResult.addTaskResult(taskName, resultData.toTaskResult(taskId, jobClasspath),
                        preciousResult);
                currentTaskId = dbTaskId;
            }

            if (++counter % 100 == 0) {
                session.clear();
            }
        }

        return jobResult;
    }

    public TaskResult loadLastTaskResult(final TaskId taskId) {
        return loadTaskResult(taskId, 0);
    }

    public TaskResult loadTaskResult(final JobId jobId, final String taskName, final int index) {
        return runWithoutTransaction(new SessionWork<TaskResult>() {

            @Override
            TaskResult executeWork(Session session) {
                long id = jobId(jobId);

                Object[] taskSearchResult = (Object[]) session.createQuery(
                        "select id, taskName from TaskData where "
                            + "taskName = :taskName and jobData = :job").setParameter("taskName", taskName)
                        .setParameter("job", session.load(JobData.class, id)).uniqueResult();

                if (taskSearchResult == null) {
                    throw new DatabaseManagerException("Failed to load result for task '" + taskName +
                        ", job: " + jobId);
                }

                DBTaskId dbTaskId = (DBTaskId) taskSearchResult[0];
                String taskName = (String) taskSearchResult[1];
                TaskId taskId = TaskIdImpl.createTaskId(jobId, taskName, dbTaskId.getTaskId(), false);

                return loadTaskResult(session, taskId, index);
            }

        });
    }

    public TaskResult loadTaskResult(final TaskId taskId, final int index) {
        return runWithoutTransaction(new SessionWork<TaskResult>() {
            @Override
            TaskResult executeWork(Session session) {
                return loadTaskResult(session, taskId, index);
            }

        });
    }

    @SuppressWarnings("unchecked")
    private TaskResult loadTaskResult(Session session, TaskId taskId, int resultIndex) {
        DBTaskId dbTaskId = taskId(taskId);

        TaskData task = (TaskData) session.load(TaskData.class, dbTaskId);
        Query query = session
                .createQuery(
                        "from TaskResultData result where result.taskRuntimeData = :task order by result.resultTime desc")
                .setParameter("task", task);

        query.setMaxResults(1);
        query.setFirstResult(resultIndex);
        List<TaskResultData> results = (List<TaskResultData>) query.list();
        if (results.isEmpty()) {
            return null;
        } else {
            String[] classpath = (String[]) session.createQuery(
                    "select job.classpath from JobData job where job.id =:jobId").setParameter("jobId",
                    jobId(taskId.getJobId())).uniqueResult();

            return results.get(0).toTaskResult(taskId, classpath);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveClasspathContentIfNeeded(Session session, JobEnvironment jobEnv) {
        if (jobEnv != null && jobEnv.getJobClasspath() != null) {
            List<Long> existing = session.createQuery(
                    "select crc from JobClasspathContent as ce where ce.crc = :crc").setLong("crc",
                    jobEnv.getJobClasspathCRC()).list();
            if (existing.isEmpty()) {
                JobClasspathContent classpathEntry = new JobClasspathContent();
                classpathEntry.setClasspathContent(jobEnv.getJobClasspathContent());
                classpathEntry.setCrc(jobEnv.getJobClasspathCRC());
                classpathEntry.setContainsJarFiles(jobEnv.containsJarFile());
                try {
                    session.save(classpathEntry);
                } catch (ConstraintViolationException e) {
                    debugLogger.warn("Failed to save classpath entry", e);
                }
            }
        }
    }

    public void newJobSubmitted(final InternalJob job) {
        runWithTransaction(new SessionWork<JobData>() {

            @Override
            JobData executeWork(Session session) {
                JobEnvironment jobEnv = job.getEnvironment();
                saveClasspathContentIfNeeded(session, jobEnv);

                JobData jobRuntimeData = JobData.createJobData(job);
                session.save(jobRuntimeData);

                job.setId(new JobIdImpl(jobRuntimeData.getId(), job.getName()));

                List<InternalTask> tasksWithNewIds = new ArrayList<InternalTask>();
                for (int i = 0; i < job.getITasks().size(); i++) {
                    InternalTask task = job.getITasks().get(i);
                    task.setId(TaskIdImpl.createTaskId(job.getId(), task.getTaskInfo().getTaskId()
                            .getReadableName(), i, true));
                    tasksWithNewIds.add(task);
                }
                job.getIHMTasks().clear();
                for (InternalTask task : tasksWithNewIds) {
                    job.getIHMTasks().put(task.getId(), task);
                }

                List<InternalTask> tasks = job.getITasks();
                List<TaskData> taskRuntimeDataList = new ArrayList<TaskData>(tasks.size());
                for (InternalTask task : tasks) {
                    taskRuntimeDataList.add(saveNewTask(session, jobRuntimeData, task));
                }
                saveTaskDependencies(session, tasks, taskRuntimeDataList);

                return jobRuntimeData;
            }

        });
    }

    private TaskData getTaskReference(Session session, InternalTask task) {
        return (TaskData) session.get(TaskData.class, taskId(task));
    }

    private void saveTaskDependencies(Session session, List<InternalTask> tasks,
            List<TaskData> taskRuntimeDataList) {
        for (int i = 0; i < tasks.size(); i++) {
            InternalTask task = tasks.get(i);
            TaskData taskRuntimeData = taskRuntimeDataList.get(i);
            if (task.hasDependences()) {
                List<DBTaskId> dependencies = new ArrayList<DBTaskId>(task.getDependences().size());
                for (Task dependency : task.getDependences()) {
                    dependencies.add(taskId((InternalTask) dependency));
                }
                taskRuntimeData.setDependentTasks(dependencies);
            } else {
                taskRuntimeData.setDependentTasks(Collections.<DBTaskId> emptyList());
            }
            if (task.getIfBranch() != null) {
                InternalTask ifBranch = task.getIfBranch();
                taskRuntimeData.setIfBranch(getTaskReference(session, ifBranch));
            } else {
                taskRuntimeData.setIfBranch(null);
            }
            if (task.getJoinedBranches() != null && task.getJoinedBranches().isEmpty()) {
                List<DBTaskId> joinedBranches = new ArrayList<DBTaskId>(task.getJoinedBranches().size());
                for (InternalTask joinedBrach : task.getJoinedBranches()) {
                    joinedBranches.add(taskId(joinedBrach));
                }
                taskRuntimeData.setJoinedBranches(joinedBranches);
            } else {
                taskRuntimeData.setJoinedBranches(Collections.<DBTaskId> emptyList());
            }
        }
    }

    private TaskData saveNewTask(Session session, JobData jobRuntimeData, InternalTask task) {
        TaskData taskRuntimeData = TaskData.createTaskData(jobRuntimeData, task);
        session.save(taskRuntimeData);

        if (task.getClass().equals(InternalJavaTask.class)) {
            JavaExecutableContainer container = (JavaExecutableContainer) task.getExecutableContainer();
            JavaTaskData javaTaskData = JavaTaskData.createJavaTaskData(taskRuntimeData, container);
            session.save(javaTaskData);
        } else if (task.getClass().equals(InternalForkedJavaTask.class)) {
            ForkedJavaExecutableContainer container = (ForkedJavaExecutableContainer) task
                    .getExecutableContainer();
            ForkedJavaTaskData forkedJavaTaskData = ForkedJavaTaskData.createForkedJavaTaskData(
                    taskRuntimeData, container);
            session.save(forkedJavaTaskData);
        } else if (task.getClass().equals(InternalNativeTask.class)) {
            NativeExecutableContainer container = (NativeExecutableContainer) task.getExecutableContainer();
            NativeTaskData nativeTaskData = NativeTaskData.createNativeTaskData(taskRuntimeData, container);
            session.save(nativeTaskData);
        } else {
            throw new IllegalArgumentException("Unexpected task class: " + task.getClass());
        }

        return taskRuntimeData;
    }

    public JobClasspathContent loadJobClasspathContent(final long crc) {
        return runWithoutTransaction(new SessionWork<JobClasspathContent>() {
            @Override
            JobClasspathContent executeWork(Session session) {
                return (JobClasspathContent) session.get(JobClasspathContent.class, crc);
            }

        });
    }

    private ExecutableContainer loadExecutableContainer(Session session, InternalTask task) {
        try {
            ExecutableContainer container = null;

            if (task.getClass().equals(InternalJavaTask.class)) {
                JavaTaskData taskData = (JavaTaskData) session.createQuery(
                        "from JavaTaskData td where td.taskData.id= :taskId").setParameter("taskId",
                        taskId(task)).uniqueResult();

                if (taskData != null) {
                    container = taskData.createExecutableContainer();
                }
            } else if (task.getClass().equals(InternalForkedJavaTask.class)) {
                ForkedJavaTaskData taskData = (ForkedJavaTaskData) session.createQuery(
                        "from ForkedJavaTaskData td where td.taskData.id = :taskId").setParameter("taskId",
                        taskId(task)).uniqueResult();

                if (taskData != null) {
                    container = taskData.createExecutableContainer();
                }
            } else if (task.getClass().equals(InternalNativeTask.class)) {
                NativeTaskData taskData = (NativeTaskData) session.createQuery(
                        "from NativeTaskData td where td.taskData.id = :taskId").setParameter("taskId",
                        taskId(task)).uniqueResult();

                if (taskData != null) {
                    container = taskData.createExecutableContainer();
                }
            } else {
                throw new IllegalArgumentException("Unexpected task class: " + task.getClass());
            }

            if (container == null) {
                throw new DatabaseManagerException("Failed to load data for task " + task.getId());
            }

            return container;
        } catch (Exception e) {
            throw new DatabaseManagerException(e);
        }
    }

    public ExecutableContainer loadExecutableContainer(final InternalTask task) {
        return runWithoutTransaction(new SessionWork<ExecutableContainer>() {
            @Override
            ExecutableContainer executeWork(Session session) {
                return loadExecutableContainer(session, task);
            }

        });
    }

    private <T> T runWithTransaction(SessionWork<T> sessionWork) {
        return runWithTransaction(sessionWork, true);
    }

    private <T> T runWithTransaction(SessionWork<T> sessionWork, boolean readonly) {
        Session session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            session.setDefaultReadOnly(readonly);
            tx = session.beginTransaction();
            T result = sessionWork.executeWork(session);
            tx.commit();
            return result;
        } catch (Throwable e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable rollbackError) {
                    debugLogger.warn("Failed to rollback transaction", rollbackError);
                }
            }
            debugLogger.warn("DB operation failed", e);
            exceptionHandler.handle("DB operation failed", e);
            return null;
        } finally {
            try {
                session.close();
            } catch (Throwable e) {
                debugLogger.warn("Failed to close session", e);
            }
        }
    }

    private <T> T runWithoutTransaction(SessionWork<T> sessionWork) {
        Session session = sessionFactory.openSession();
        try {
            session.setDefaultReadOnly(true);
            T result = sessionWork.executeWork(session);
            return result;
        } catch (Throwable e) {
            debugLogger.warn("DB operation failed", e);
            exceptionHandler.handle("DB operation failed", e);
            return null;
        } finally {
            try {
                session.close();
            } catch (Throwable e) {
                debugLogger.warn("Failed to close session", e);
            }
        }
    }

    private static TaskData.DBTaskId taskId(InternalTask task) {
        return taskId(task.getId());
    }

    private static TaskData.DBTaskId taskId(TaskId taskId) {
        TaskData.DBTaskId id = new TaskData.DBTaskId();
        id.setJobId(jobId(taskId.getJobId()));
        id.setTaskId(Long.valueOf(taskId.value()));
        return id;
    }

    private static long jobId(InternalJob job) {
        return jobId(job.getId());
    }

    private static long jobId(JobId jobId) {
        return Long.valueOf(jobId.value());
    }

}
