package com.ghx;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class ZkTest {

	// 会话超时时间，设置为与系统默认时间一致
	private static final int SESSION_TIMEOUT = 30 * 1000;

	// 创建 ZooKeeper 实例
	private ZooKeeper zk;
	// 创建 Watcher 实例
	private Watcher wh = new Watcher() {
		/**
		 * Watched事件
		 */
		public void process(WatchedEvent event) {
			System.out.println("WatchedEvent >>> " + event.toString());
		}
	};

	// 初始化 ZooKeeper 实例
	private void createZKInstance() throws IOException { // 连接到ZK服务，多个可以用逗号分割写
		zk = new ZooKeeper("192.168.0.86:2181,192.168.0.86:2182,192.168.0.86:2183", ZkTest.SESSION_TIMEOUT, this.wh);
		System.out.println(zk);
	}

	private void ZKOperations() throws IOException, InterruptedException, KeeperException {
		// 创建节点
		zk.create("/zoo", "myData".getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

		// 获取节点数据
		byte[] data = zk.getData("/zoo", this.wh, null);// 添加Watch
		System.out.println(new String(data));
		
		// 修改节点
		zk.setData("/zoo", "myDataUpdte".getBytes(), -1); // 这里再次进行修改，则不会触发Watch事件，这就是我们验证ZK的一个特性“一次性触发”，也就是说设置一次监视，只会对下次操作起一次作用。

		System.out.println(new String(zk.getData("/zoo", false, null)));
		
		// 删除节点
		zk.delete("/zoo", -1);
		System.out.println("查看节点是否被删除： ");
		System.out.println("节点状态： [" + zk.exists("/zoo2", false) + "]");
	}

	private void ZKClose() throws InterruptedException {
		zk.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
		ZkTest dm = new ZkTest();
		dm.createZKInstance();
		dm.ZKOperations();
		dm.ZKClose();

	}

}
