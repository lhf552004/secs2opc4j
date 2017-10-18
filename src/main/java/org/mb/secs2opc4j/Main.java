package org.mb.secs2opc4j;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.apache.camel.Consumer;
import org.apache.camel.EndpointConfiguration;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultEndpoint;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.DuplicateGroupException;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.Server;
import org.openscada.opc.lib.da.browser.Branch;
import org.openscada.opc.lib.da.browser.Leaf;
import org.openscada.opc.lib.da.browser.TreeBrowser;



public class Main extends DefaultEndpoint{
	private String domain = "localhost";
	private String host = "localhost";
	private String clsId;
	private String progId;
	private String username;
	private String password;
	private int poolSize = 2;
	private int delay = 500;
	private Server opcServer;
	private Group opcGroup;
	private boolean diffOnly = false;
	private boolean valuesOnly = true;
	private boolean failIfTagAbsent = true;

	public static final String ERROR_CODE = "errorCode";
	public static final String QUALITY = "quality";
	public static final String TIMESTAMP = "timestamp";
	public static final String VALUE = "value";

	private final Map<String, Item> opcItems = new TreeMap<String, Item>();
	private boolean forceHardwareRead = false;
	public static final String NO_CLSID_MSG = "clsId OR progId MUST BE SET!";
	public static final String NO_SUBGROUP_MSG = "Unable to find sub-group: %s %nPossible Matches:%s";
	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @param domain the domain to set
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	protected Server getOpcServer() {
		return opcServer;
	}
	public int getPoolSize() {
		return poolSize;
	}

	/**
	 * @param poolSize the poolSize to set
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	/**
	 * @return the clsId
	 */
	public String getClsId() {
		return clsId;
	}

	/**
	 * @param clsId the clsId to set
	 */
	public void setClsId(String clsId) {
		this.clsId = clsId;
	}

	/**
	 * @return the progId
	 */
	public String getProgId() {
		return progId;
	}

	/**
	 * @param progId the progId to set
	 */
	public void setProgId(String progId) {
		this.progId = progId;
	}

	/**
	 * @return the forceHardwareRead
	 */
	public boolean isForceHardwareRead() {
		return forceHardwareRead;
	}

	/**
	 * @param forceHardwareRead the forceHardwareRead to set
	 */
	public void setForceHardwareRead(boolean forceHardwareRead) {
		this.forceHardwareRead = forceHardwareRead;
	}

	/**
	 * @return the delay
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * @param delay the delay to set
	 */
	public void setDelay(int delay) {
		this.delay = delay;
	}

	/**
	 * @return the diffOnly
	 */
	public boolean isDiffOnly() {
		return diffOnly;
	}

	/**
	 * @param diffOnly the diffOnly to set
	 */
	public void setDiffOnly(boolean diffOnly) {
		this.diffOnly = diffOnly;
	}

	/**
	 * @return the valuesOnly
	 */
	public boolean isValuesOnly() {
		return valuesOnly;
	}

	/**
	 * @param valuesOnly the valuesOnly to set
	 */
	public void setValuesOnly(boolean valuesOnly) {
		this.valuesOnly = valuesOnly;
	}

	/**
	 * @return the opcItems
	 */
	public Map<String, Item> getOpcItems() {
		return opcItems;
	}

	/**
	 * @return the failIfTagAbsent
	 */
	public boolean isFailIfTagAbsent() {
		return failIfTagAbsent;
	}

	/**
	 * @param failIfTagAbsent the failIfTagAbsent to set
	 */
	public void setFailIfTagAbsent(boolean failIfTagAbsent) {
		this.failIfTagAbsent = failIfTagAbsent;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Main main = new Main();
		try {
			main.initializeServerConnection();
		} catch (OPCConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 *
	 * @return A connection to this endpoints opc server.
	 * @throws IllegalArgumentException
	 * @throws UnknownHostException
	 * @throws JIException
	 * @throws AlreadyConnectedException
	 */
	private void initializeServerConnection() throws OPCConnectionException {
		if (getOpcServer() == null) {

			if (getClsId() == null && getProgId() == null) {
				throw new OPCConnectionException(NO_CLSID_MSG);
			}

			ConnectionInformation connInfo = new ConnectionInformation();

			connInfo.setClsid(getClsId());
			connInfo.setProgId(getProgId());
			connInfo.setHost(getHost());
			connInfo.setDomain(getDomain());
			connInfo.setUser(getUsername());
			connInfo.setPassword(getPassword());

			ScheduledExecutorService execService
					= Executors.newScheduledThreadPool(
							getPoolSize(),
							new Opcda2EndpointThreadFactory());

			opcServer = new Server(connInfo, execService);

			try {
				opcServer.connect();

				opcGroup = opcServer.addGroup(getId());
			} catch (IllegalArgumentException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			} catch (UnknownHostException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			} catch (JIException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			} catch (AlreadyConnectedException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			} catch (NotConnectedException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			} catch (DuplicateGroupException ex) {
				throw new OPCConnectionException(ex.getMessage(), ex);
			}
			registerTags();
		}
	}
	private void registerTags() throws RuntimeCamelException, OPCConnectionException {
		EndpointConfiguration cfg = getEndpointConfiguration();
		String opcTreePath = cfg.getParameter("path");
		String[] pathArray = opcTreePath.split("/");
		try {
			TreeBrowser treeBrowser = opcServer.getTreeBrowser();
			Branch root = treeBrowser.browse();
			Branch parent = root;

			Leaf leaf = null;

			for (int i = 0; i < pathArray.length; i++) {
				boolean found = false;
				//This should handle "//" and the first /
				if (pathArray[i].isEmpty()) {
					continue;
				}
				for (Branch candidate : parent.getBranches()) {
					if (candidate.getName().equals(pathArray[i])) {
						parent = candidate;
						found = true;
						break;
					}
				}
				//If we are on the last item, and its still not found,
				//check to see if it is a tag (leaf)
				if (!found && i == pathArray.length - 1) {
					for (Leaf l : parent.getLeaves()) {
						if (l.getName().equals(pathArray[i])) {
							leaf = l;
							found = true;
							break;
						}
					}
				}

				if (!found) {
					ArrayList<String> possibleBranches = new ArrayList<String>();
					StringBuilder possibleMatches = new StringBuilder();
					for (Branch b : parent.getBranches()) {
						possibleBranches.add(String.format("%n[B] %s", b.getName()));
					}
					Collections.sort(possibleBranches, String.CASE_INSENSITIVE_ORDER);
					ArrayList<String> possibleLeaves = new ArrayList<String>();
					for (Leaf l : parent.getLeaves()) {
						possibleLeaves.add(String.format("%n[T] %s", l.getName()));
					}
					Collections.sort(possibleLeaves, String.CASE_INSENSITIVE_ORDER);

					for (String s : possibleBranches) {
						possibleMatches.append(s);
					}
					for (String s : possibleLeaves) {
						possibleMatches.append(s);
					}

					throw new OPCConnectionException(String.format(NO_SUBGROUP_MSG, pathArray[i], possibleMatches.toString()));
				}
			}
//			if (leaf != null) {
//				registerLeaf(leaf);
//			} else {
//				populateItemsMapRecursive(parent);
//			}
		} catch (Exception ex) {
			throw new OPCConnectionException(ex.getMessage(), ex);
		}
	}
	private final class Opcda2EndpointThreadFactory implements ThreadFactory {

		int count = 0;

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, Main.this.getId() + "_" + count);
			return t;
		}
	}
	private String id;
	public String getId() {
		// TODO Auto-generated method stub
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public Consumer createConsumer(Processor arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Producer createProducer() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}

}


