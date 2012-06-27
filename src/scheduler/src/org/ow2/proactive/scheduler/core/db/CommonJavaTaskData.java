package org.ow2.proactive.scheduler.core.db;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.type.SerializableToBlobType;
import org.ow2.proactive.scheduler.common.task.util.ByteArrayWrapper;
import org.ow2.proactive.scheduler.task.JavaExecutableContainer;


@MappedSuperclass
public class CommonJavaTaskData implements Serializable {

    private long id;

    private TaskData taskData;

    protected Map<String, byte[]> searializedArguments;

    private String userExecutableClassName;

    protected void initProperties(TaskData taskData, JavaExecutableContainer container) {
        setTaskData(taskData);

        setUserExecutableClassName(container.getUserExecutableClassName());

        Map<String, ByteArrayWrapper> args = container.getSerializedArguments();

        if (args != null) {
            Map<String, byte[]> convertedArgs = new HashMap<String, byte[]>(args.size());
            for (Map.Entry<String, ByteArrayWrapper> argEntry : args.entrySet()) {
                convertedArgs.put(argEntry.getKey(), argEntry.getValue().getByteArray());
            }
            setSearializedArguments(convertedArgs);
        }
    }

    @Id
    @GeneratedValue
    @Column(name = "ID")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns(value = { @JoinColumn(name = "JOB_ID", referencedColumnName = "TASK_ID_JOB"),
            @JoinColumn(name = "TASK_ID", referencedColumnName = "TASK_ID_TASK") })
    @OnDelete(action = OnDeleteAction.CASCADE)
    public TaskData getTaskData() {
        return taskData;
    }

    public void setTaskData(TaskData taskData) {
        this.taskData = taskData;
    }

    @Column(name = "CLASS_NAME", nullable = false)
    @Lob
    public String getUserExecutableClassName() {
        return userExecutableClassName;
    }

    public void setUserExecutableClassName(String userExecutableClassName) {
        this.userExecutableClassName = userExecutableClassName;
    }

    @Column(name = "ARGUMENTS")
    @Type(type = "org.hibernate.type.SerializableToBlobType", parameters = @Parameter(name = SerializableToBlobType.CLASS_NAME, value = "java.lang.Object"))
    public Map<String, byte[]> getSearializedArguments() {
        return searializedArguments;
    }

    public void setSearializedArguments(Map<String, byte[]> searializedArguments) {
        this.searializedArguments = searializedArguments;
    }

}
