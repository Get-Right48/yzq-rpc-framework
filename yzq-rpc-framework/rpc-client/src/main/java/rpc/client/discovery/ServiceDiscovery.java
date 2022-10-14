package rpc.client.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.client.connect.ConnectionManager;
import rpc.config.Constant;
import rpc.protocol.RpcProtocol;
import rpc.zookeeper.CuratorClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private CuratorClient curatorClient;

    public ServiceDiscovery(String registryAddr){
        this.curatorClient = new CuratorClient(registryAddr);
        discoveryService();
    }

    private void discoveryService(){
        try {
            getServiceAndUpdateService();
            curatorClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PREFIX, (curatorFramework, pathChildrenCacheEvent) -> {
                PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                ChildData data = pathChildrenCacheEvent.getData();
                switch (type){
                    case CONNECTION_RECONNECTED:
                        logger.info("reconnected to zookeeper, try get last service");
                        getServiceAndUpdateService();
                        break;
                    case CHILD_ADDED:
                        getServiceAndUpdateService(data, PathChildrenCacheEvent.Type.CHILD_ADDED);
                        break;
                    case CHILD_REMOVED:
                        getServiceAndUpdateService(data, PathChildrenCacheEvent.Type.CHILD_REMOVED);
                    case CHILD_UPDATED:
                        getServiceAndUpdateService(data, PathChildrenCacheEvent.Type.CHILD_UPDATED);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getServiceAndUpdateService(ChildData data, PathChildrenCacheEvent.Type type) {
        String json = new String(data.getData(), StandardCharsets.UTF_8);
        RpcProtocol protocol = RpcProtocol.fromJson(json);
        updateConnectedServer(protocol,type);
    }

    private void updateConnectedServer(RpcProtocol protocol, PathChildrenCacheEvent.Type type) {
        ConnectionManager.getInstance().updateConnectedServer(protocol,type);
    }

    private void getServiceAndUpdateService() {
        try {
            List<String> childrenNode = curatorClient.getChildren(Constant.ZK_REGISTRY_PREFIX);
            ArrayList<RpcProtocol> dataList = new ArrayList<>();
            for (String node : childrenNode) {
                logger.info("service node:{}",node);
                byte[] bytes = curatorClient.getData(Constant.ZK_REGISTRY_PREFIX+"/"+node);
                String data = new String(bytes);
                RpcProtocol rpcProtocol = RpcProtocol.fromJson(data);
                dataList.add(rpcProtocol);
            }
            // 更新最新数据
            logger.info("service node data:{}",dataList);
            UpdateService(dataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void UpdateService(ArrayList<RpcProtocol> dataList) {
        ConnectionManager.getInstance().updateConnectedServer(dataList);
    }

    public void close(){
        this.curatorClient.close();
    }
}
