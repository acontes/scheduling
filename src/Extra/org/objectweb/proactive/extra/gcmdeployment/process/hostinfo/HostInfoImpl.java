package org.objectweb.proactive.extra.gcmdeployment.process.hostinfo;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.proactive.core.util.OperatingSystem;
import static org.objectweb.proactive.extra.gcmdeployment.GCMDeploymentLoggers.GCMD_LOGGER;
import org.objectweb.proactive.extra.gcmdeployment.process.HostInfo;


public class HostInfoImpl implements HostInfo {
    private String username;
    private String homeDirectory;
    private String id;
    private int hostCapacity;
    private int vmCapacity;
    private OperatingSystem os;
    private Set<Tool> tools;

    public HostInfoImpl() {
        username = null;
        homeDirectory = null;
        id = null;
        hostCapacity = 0;
        vmCapacity = 0;
        os = null;
        tools = new HashSet<Tool>();
    }

    public HostInfoImpl(String id) {
        this();
        this.id = id;
    }

    /**
     * Checks that all required fields have been set.
     *
     * @throws IllegalStateException If a required field has not been set
     */
    public void check() throws IllegalStateException {
        if (id == null) {
            throw new IllegalStateException(
                "id field is not set in this HostInfo\n" + toString());
        }

        if (homeDirectory == null) {
            throw new IllegalStateException("homeDirectory is not set for id=" +
                id + "\n" + toString());
        }

        if (os == null) {
            throw new IllegalStateException("os is not set for id=" + id +
                "\n" + toString());
        }

        if ((hostCapacity % vmCapacity) != 0) {
            throw new IllegalStateException(
                "hostCapacity is not a multiple of vmCapacity for HostInfo=" +
                id + "\n" + toString());
        }

        // Theses fields are not mandatory
        if (username == null) {
            GCMD_LOGGER.debug("HostInfo is ready but username has not been set");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HostInfo<" + super.toString() + ">, ");
        sb.append(" id=" + id);
        sb.append(" os=" + os);
        sb.append(" username=" + username);
        sb.append(" homeDir=" + homeDirectory);
        sb.append("\n");
        for (Tool tool : tools) {
            sb.append("\t" + tool + "\n");
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HostInfoImpl other = (HostInfoImpl) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOs(OperatingSystem os) {
        this.os = os;
    }

    public void setHostCapacity(int hostCapacity) {
        this.hostCapacity = hostCapacity;
    }

    public void setVmCapacity(int vmCapacity) {
        this.vmCapacity = vmCapacity;
    }

    public void addTool(Tool tool) {
        this.tools.add(tool);
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public String getId() {
        return id;
    }

    public OperatingSystem getOS() {
        return os;
    }

    public Tool getTool(String id) {
        for (Tool tool : tools) {
            if (tool.getId().equals(id)) {
                return tool;
            }
        }

        return null;
    }

    public Set<Tool> getTools() {
        return tools;
    }

    public String getUsername() {
        return username;
    }

    public int getHostCapacity() {
        return hostCapacity;
    }

    public int getVmCapacity() {
        return vmCapacity;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        HostInfoImpl res = new HostInfoImpl();
        res.username = this.username;
        res.homeDirectory = this.homeDirectory;
        res.hostCapacity = this.hostCapacity;
        res.vmCapacity = this.vmCapacity;
        res.id = this.id;
        res.os = this.os;
        for (Tool t : tools) {
            res.tools.add((Tool) t.clone());
        }
        return res;
    }

    @SuppressWarnings("unused")
    static public class UnitTestHostInfoImpl {
        HostInfoImpl notInitialized;
        HostInfoImpl halfInitialized;
        HostInfoImpl fullyInitialized;

        @Before
        public void before() {
            notInitialized = new HostInfoImpl();

            halfInitialized = new HostInfoImpl();
            halfInitialized.setId("toto");
            halfInitialized.addTool(new Tool("tool", "//path"));

            fullyInitialized = new HostInfoImpl();
            fullyInitialized.setId("id");
            fullyInitialized.setOs(OperatingSystem.unix);
            fullyInitialized.setHomeDirectory("//homeidr");
            fullyInitialized.setUsername("usermane");
            fullyInitialized.addTool(new Tool("tool", "//path"));
        }

        @Test
        public void getTool1() {
            Assert.assertNotNull(fullyInitialized.getTool("tool"));
            Assert.assertNull(fullyInitialized.getTool("tool2"));
        }

        @Test
        public void equality() {
            HostInfoImpl tmp = new HostInfoImpl();
            tmp.setId("id");
            Assert.assertTrue(tmp.equals(fullyInitialized));

            tmp = new HostInfoImpl();
            tmp.setId("xxxxxxx");
            Assert.assertFalse(tmp.equals(fullyInitialized));
        }

        @Test(expected = IllegalStateException.class)
        public void checkReadygetHalfInitialized() {
            halfInitialized.check();
        }

        @Test(expected = IllegalStateException.class)
        public void checkReadygetHomeDirectory() {
            notInitialized.check();
        }
    }
}
