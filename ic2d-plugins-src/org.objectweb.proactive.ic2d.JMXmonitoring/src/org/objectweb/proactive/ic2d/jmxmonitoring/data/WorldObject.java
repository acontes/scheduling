package org.objectweb.proactive.ic2d.jmxmonitoring.data;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MalformedObjectNameException;

import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.jmx.ProActiveConnection;
import org.objectweb.proactive.core.jmx.util.JMXNotificationManager;

/**
 * Holder class for all monitored hosts and virtual nodes
 * @author ProActive Team
 */
public class WorldObject extends AbstractData{

	// -------------------------------------------
	// --- Constants -----------------------------
	// -------------------------------------------

	public static boolean HIDE_P2PNODE_MONITORING = true;
	public static boolean DEFAULT_ENABLE_AUTO_RESET = false;

	public final static String ADD_VN_MESSAGE = "Add a virtual node";
	public final static String REMOVE_VN_MESSAGE = "Remove a virtual node";

	// 60 s
	public static int MAX_AUTO_RESET_TIME = 60;
	// 1 s
	public static int MIN_AUTO_RESET_TIME = 1;
	// 7 s
	public static int DEFAULT_AUTO_RESET_TIME = 7;
	
	private static int DEFAULT_MAX_DEPTH = 3;
	
	public enum methodName { ADD_CHILD, REMOVE_CHILD, RESET_COMMUNICATIONS };

	// -------------------------------------------
	// --- Variables -----------------------------
	// -------------------------------------------

	private int currentAutoResetTime = DEFAULT_AUTO_RESET_TIME;
	private boolean enableAutoReset = DEFAULT_ENABLE_AUTO_RESET;

	private String name;

	private ProActiveConnection connection;

	/** Contains all virtual nodes. */
	private Map<String, VNObject> vnChildren;

	/**
	 * A map of all known active objects.
	 * This map is used to have good performances.
	 */
	private Map<UniqueID, ActiveObject> activeObjects;
	
	private Map<UniqueID, ActiveObject> migrations;
	
	private int maxDepth = DEFAULT_MAX_DEPTH;
	
	/**
	 * Thread
	 */
	private MonitorThread monitorThread;
	
	private JMXNotificationManager notificationManager;
	
	// -------------------------------------------
	// --- Constructor ---------------------------
	// -------------------------------------------

	/**
	 * Create a new WorldObject
	 * @param connection A ProActiveConnection
	 */
	public WorldObject(){
		super(null);
		this.activeObjects = new ConcurrentHashMap<UniqueID, ActiveObject>();
		this.migrations = new ConcurrentHashMap<UniqueID, ActiveObject>();
		this.vnChildren = new ConcurrentHashMap<String, VNObject>();
		
		// Record the model
		this.name = ModelRecorder.getInstance().addModel(this);
		
		// Adds a MonitorTread refresher
		monitorThread = new MonitorThread(this);
		addObserver(monitorThread);
		
		// Creates a notification manager
		notificationManager = JMXNotificationManager.getInstance();
	}

	public WorldObject(ProActiveConnection connection){
		this();
		this.connection = connection;
	}

	// -------------------------------------------
	// --- Methods -------------------------------
	// -------------------------------------------

	/**
	 * Add a host to the WorldObject
	 * @param url The url of the host.
	 * @param rank The rank of the depth.(0 if the user want ot monitor this host,
	 * 1,2,3...if this host was discovered.)
	 */
	public void addHost(String url, int rank){
		try {
			addChild(new HostObject(this, url, rank));
			notifyObservers();
		} catch (MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setChanged();
		if(getMonitoredChildrenSize() == 1)
			notifyObservers(methodName.ADD_CHILD);
		notifyObservers();
	}
	
	/**
	 * Add a host to the WorldObject
	 * @param url The url of the host.
	 */
	public void addHost(String url){
		this.addHost(url, 0);
	}
	
	@Override
	public void removeChild(AbstractData child){
		super.removeChild(child);
		setChanged();
		if(getMonitoredChildrenSize() == 0)
			notifyObservers(methodName.REMOVE_CHILD);
		notifyObservers();
	}
	
	/**
	 * Find an active object in the map of the known active objects.
	 * @param id The Unique id of the active object.
	 * @return an active object.
	 */	
	public ActiveObject findActiveObject(UniqueID id){
		return this.activeObjects.get(id);
	}

	/**
	 * Records a new active object in the map of the known active objects.
	 * @param ao The active object to add.
	 */
	public void addActiveObject(ActiveObject ao){
		synchronized (activeObjects) {
			ActiveObject oldAO = this.activeObjects.get(ao.getUniqueID());
			if(oldAO!=null){
				// It's maybe a migration
				// If the url of the two active objects are different, this is a migration
				if(!ao.getParent().getUrl().equals(oldAO.getParent().getUrl()))
					migrations.put(ao.getUniqueID(), oldAO);
			}
			this.activeObjects.put(ao.getUniqueID(), ao);
		}
	}
	
	
	/**
	 * Removes an active object in the map of the known active objects.
	 * @param ao The active object to remove.
	 */
	public void removeActiveObject(UniqueID id){
		ActiveObject ao = null;
		synchronized (migrations) {
			ao = migrations.remove(id);
		}
		if(ao==null){
			synchronized (activeObjects) {
				ao = this.activeObjects.remove(id);
			}
			
		}
			
		if(ao==null){
			System.out.println("Suppression de "+ao);
		}
		else{
			System.out.println("Suppression de "+ao+", sur "+ao.getParent());	
		}
		
		ao.resetCommunications();
		ao.getParent().removeChild(ao);
		ao.unsubscribe(ao.getListener());
	}
		

	@Override
	public <T extends AbstractData> T getParent() {
		return null;
	}

	@Override
	public void explore() {
		super.exploreEachChild();
	}

	@Override
	public WorldObject getWorldObject(){
		return this;
	}

	@Override
	protected ProActiveConnection getConnection(){
		return this.connection;
	}

	@Override
	public String getKey() {
		return this.name;
	}

	@Override
	public String getType() {
		return "world object";
	}
	
	@Override
	public int getHostRank(){
		return 0;
	}
	
	@Override
	public int getDepth(){
		return this.maxDepth;
	}
	
	/**
	 * Changes the max depth.
	 * @param depth The new max depth.
	 */
	public void setDepth(int depth){
		this.maxDepth = depth;
	}
	
	public MonitorThread getMonitorThread(){
		return this.monitorThread;
	}
	
	public void addMigration(ActiveObject ao){
		this.migrations.put(ao.getUniqueID(), ao);
	}
	
	public JMXNotificationManager getNotificationManager(){
		return this.notificationManager;
	}
	
	////////// NEW DATA

	/**
	 * Returns the name of this world.
	 * @return The name of this world.
	 */
	public String getName(){
		return name;
	}

	/**
	 * Enables the auto reset action
	 * @param enable
	 */
	public void setEnableAutoResetTime(boolean enable){
		enableAutoReset = enable;
	}

	/**
	 * Returns true if the auto reset time is enabled, false otherwise
	 * @return true if the auto reset time is enabled, false otherwise
	 */
	public boolean enableAutoResetTime(){
		return enableAutoReset;
	}
	
	/**
	 * Change the current auto reset time
	 * @param time The new time
	 */
	public void setAutoResetTime(int time){
		currentAutoResetTime = time;
	}

	/**
	 * Returns the current auto reset time
	 * @return The current auto reset time
	 */
	public int getAutoResetTime(){
		return this.currentAutoResetTime;
	}
	
	/**
	 * Add a virtual node to this object
	 * @param vn
	 */
	protected void addVirtualNode(VNObject vn) {
		vnChildren.put(vn.getKey(), vn);
		setChanged();
		Hashtable<String, VNObject> data = new Hashtable<String, VNObject>();
		data.put(ADD_VN_MESSAGE, vn);
		notifyObservers(data);
	}
	
	/**
	 * Remove a virtual node to this object
	 * @param vn
	 */
	protected void removeVirtualNode(VNObject vn){
		vnChildren.remove(vn.getKey());
		setChanged();
		Hashtable<String, VNObject> data = new Hashtable<String, VNObject>();
		data.put(REMOVE_VN_MESSAGE, vn);
		notifyObservers(data);
	}

	public VNObject getVirtualNode(String virtualNodeName) {
		return vnChildren.get(virtualNodeName);
	}
	
	public List<VNObject>getVNChildren () {
		return new ArrayList<VNObject>(this.vnChildren.values ());
	}
}
