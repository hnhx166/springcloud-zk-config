package com.ghx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 
 * 集群测试
 * 
 * 导入配置文件
 * 
 * 项目名称：springcloud-zk-config 类名称：ZkClusterPropertiesImport 类描述： 创建人：guohaixiang
 * 创建时间：2018年10月22日 下午1:40:57 修改人：Administrator 修改时间：2018年10月22日 下午1:40:57 修改备注：
 * 
 * @version 1.0
 *
 */
public class ZkClusterPropertiesImport {

	private Logger logger = LoggerFactory.getLogger(ZkClusterPropertiesImport.class);

	public final static Integer MAX_CONNECT_ATTEMPT = 5;
    public final static String ZK_ROOT_NODE = "/";
    public final static String ZK_SYSTEM_NODE = "zookeeper"; // ZK internal folder (quota info, etc) - have to stay away from it
    public final static String ZK_HOSTS = "/appconfig/hosts";
    public final static String ROLE_USER = "USER";
    public final static String ROLE_ADMIN = "ADMIN";
    public final static String SOPA_PIPA = "SOPA/PIPA BLACKLISTED VALUE";

	private ArrayList<ACL> defaultAcl = ZooDefs.Ids.OPEN_ACL_UNSAFE;

	private ArrayList<ACL> defaultAcl() {
		return defaultAcl;
	}

	public void setDefaultAcl(String jsonAcl) {
		if (jsonAcl == null || jsonAcl.trim().length() == 0) {
			logger.trace("Using UNSAFE ACL. Anyone on your LAN can change your Zookeeper data");
			defaultAcl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
			return;
		}
		// Don't let things happen in a half-baked state, build the new ACL and then set
		// it into
		// defaultAcl
		ArrayList<ACL> newDefault = new ArrayList<>();
		// try {
		JSONArray acls = (JSONArray) ((JSONObject) JSONObject.parse(jsonAcl)).get("acls");
		for (Iterator it = acls.iterator(); it.hasNext();) {
			JSONObject acl = (JSONObject) it.next();
			String scheme = ((String) acl.get("scheme")).trim();
			String id = ((String) acl.get("id")).trim();
			int perms = 0;
			String permStr = ((String) acl.get("perms")).toLowerCase().trim();
			for (char c : permStr.toCharArray()) {
				switch (c) {
				case 'a':
					perms += ZooDefs.Perms.ADMIN;
					break;
				case 'c':
					perms += ZooDefs.Perms.CREATE;
					break;
				case 'd':
					perms += ZooDefs.Perms.DELETE;
					break;
				case 'r':
					perms += ZooDefs.Perms.READ;
					break;
				case 'w':
					perms += ZooDefs.Perms.WRITE;
					break;
				case '*':
					perms += ZooDefs.Perms.ALL;
					break;
				default:
					throw new RuntimeException("Illegal permission character in ACL " + c);
				}
			}
			newDefault.add(new ACL(perms, new Id(scheme, id)));
		}
		// } catch (ParseException e) {
		// // Throw it all the way up to the error handlers
		// throw new RuntimeException("Unable to parse default ACL " + jsonAcl, e);
		// }
		defaultAcl = newDefault;
	}

	/**
	 * 创建Zookeeper实例
	 * 
	 * @param connectUrl
	 *            连接地址，ip:port，多个用逗号分隔
	 * @param sessionTimeout
	 *            超时时间(单位：毫秒)
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public ZooKeeper createZKConnection(String connectUrl, Integer sessionTimeout)
			throws IOException, InterruptedException {
		Integer connectAttempt = 0;
		ZooKeeper zk = new ZooKeeper(connectUrl, sessionTimeout, new Watcher() {

			@Override
			public void process(WatchedEvent event) {
				logger.trace("Connecting to ZK.");
			}

		});

		while (zk.getState() != ZooKeeper.States.CONNECTED) {
			Thread.sleep(30);
			connectAttempt++;
			if (connectAttempt == MAX_CONNECT_ATTEMPT) {
				break;
			}
		}
		return zk;
	}
	
	public void closeZooKeeper(ZooKeeper zk) throws InterruptedException {
        logger.trace("Closing ZooKeeper");
        if (zk != null) {
            zk.close();
            logger.trace("Closed ZooKeeper");

        }
    }

	public void importData(List<String> importFile, boolean overwrite, ZooKeeper zk) throws InterruptedException, KeeperException {
		for (String line : importFile) {
			logger.debug("Importing line " + line);
			// Delete Operation
			if (line.startsWith("-")) {
				String nodeToDelete = line.substring(1);
				deleteNodeIfExists(nodeToDelete, zk);
			} else {
				int firstEq = line.indexOf('=');
				int secEq = line.indexOf('=', firstEq + 1);

				String path = line.substring(0, firstEq);
				if ("/".equals(path)) {
					path = "";
				}
				String name = line.substring(firstEq + 1, secEq);
				String value = readExternalizedNodeValue(line.substring(secEq + 1));
				String fullNodePath = path + "/" + name;

				// Skip import of system node
				if (fullNodePath.startsWith(ZK_SYSTEM_NODE)) {
					logger.debug("Skipping System Node Import: " + fullNodePath);
					continue;
				}
				boolean nodeExists = nodeExists(fullNodePath, zk);

				if (!nodeExists) {
					// If node doesnt exist then create it.
					createPathAndNode(path, name, value.getBytes(), true, zk);
				} else {
					// If node exists then update only if overwrite flag is set.
					if (overwrite) {
						setPropertyValue(path + "/", name, value, zk);
					} else {
						logger.info("Skipping update for existing property " + path + "/" + name
								+ " as overwrite is not enabled!");
					}
				}

			}
		}
	}

	private String readExternalizedNodeValue(String raw) {
		return raw.replaceAll("\\\\n", "\n");
	}

	private void createPathAndNode(String path, String name, byte[] data, boolean force, ZooKeeper zk)
			throws InterruptedException, KeeperException {
		// 1. Create path nodes if necessary
		StringBuilder currPath = new StringBuilder();
		for (String folder : path.split("/")) {
			if (folder.length() == 0) {
				continue;
			}
			currPath.append('/');
			currPath.append(folder);

			if (!nodeExists(currPath.toString(), zk)) {
				createIfDoesntExist(currPath.toString(), new byte[0], true, zk);
			}
		}

		// 2. Create leaf node
		createIfDoesntExist(path + '/' + name, data, force, zk);
	}

	private void createIfDoesntExist(String path, byte[] data, boolean force, ZooKeeper zooKeeper)
			throws InterruptedException, KeeperException {
		try {
			zooKeeper.create(path, data, defaultAcl(), CreateMode.PERSISTENT);
		} catch (KeeperException ke) {
			// Explicit Overwrite
			if (KeeperException.Code.NODEEXISTS.equals(ke.code())) {
				if (force) {
					zooKeeper.delete(path, -1);
					zooKeeper.create(path, data, defaultAcl(), CreateMode.PERSISTENT);
				}
			} else {
				throw ke;
			}
		}
	}
	
	public void setPropertyValue(String path, String name, String value, ZooKeeper zk) throws KeeperException, InterruptedException {
        String nodePath = path + name;
        logger.debug("Setting property " + nodePath + " to " + value);
        zk.setData(nodePath, value.getBytes(), -1);

    }
	
	public boolean nodeExists(String nodeFullPath, ZooKeeper zk) throws KeeperException, InterruptedException {
        logger.trace("Checking if exists: " + nodeFullPath);
        return zk.exists(nodeFullPath, false) != null;
    }

	private void deleteNodeIfExists(String path, ZooKeeper zk) throws InterruptedException, KeeperException {
		zk.delete(path, -1);
	}
	
	//导入文件
	public List<String> importFile() throws IOException {
		
		//获取classpath下边的文件
		ClassPathResource classPathResource = new ClassPathResource("config.txt");
		classPathResource.getFile();
		
		InputStream fis = classPathResource.getInputStream();
		
		// open the stream and put it into BufferedReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
        String inputLine;
        List<String> importFile = new ArrayList<>();
        Integer lineCnt = 0;
        while ((inputLine = br.readLine()) != null) {
            lineCnt++;
            // Empty or comment?
            if (inputLine.trim().equals("") || inputLine.trim().startsWith("#")) {
                continue;
            }
            if (inputLine.startsWith("-")) {
                //DO nothing.
            } else if (!inputLine.matches("/.+=.+=.*")) {
                throw new IOException("Invalid format at line " + lineCnt + ": " + inputLine);
            }

            importFile.add(inputLine);
        }
        br.close();
        
        return importFile;
	}

	public static void main(String[] args) throws InterruptedException, KeeperException, IOException {
		boolean overwrite = true;
		String connectUrl = "192.168.0.86:2181,192.168.0.86:2182,192.168.0.86:2183";
		int sessionTimeout = 30 * 1000;
		
		ZkClusterPropertiesImport zkpi = new  ZkClusterPropertiesImport();
		zkpi.importData(zkpi.importFile(), overwrite, zkpi.createZKConnection(connectUrl, sessionTimeout));
	}

}
