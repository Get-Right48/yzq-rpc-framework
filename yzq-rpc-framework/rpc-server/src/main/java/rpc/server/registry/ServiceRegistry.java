package rpc.server.registry;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.config.Constant;
import rpc.protocol.RpcProtocol;
import rpc.protocol.RpcServiceInfo;
import rpc.util.ServiceUtil;
import rpc.zookeeper.CuratorClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 服务注册 ---zookeeper
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CuratorClient client;

    private List<String> pathList = new ArrayList<>();

    public ServiceRegistry(String registryAddr){
        this.client = new CuratorClient(registryAddr);
    }

    public void registryService(String host, int port, Map<String,Object> serviceMap){
        List<RpcServiceInfo> infos = new ArrayList<>();
        for (String key : serviceMap.keySet()) {
            String[] arr = key.split(ServiceUtil.SERVICE_CONCAT_TOKEN);
            if (arr.length > 0){
                RpcServiceInfo info = new RpcServiceInfo();
                info.setServiceName(arr[0]);
                if (arr.length == 2){
                    info.setVersion(arr[1]);
                }else {
                    info.setVersion("");
                }
                logger.info("Registry new service : {}",key);
                infos.add(info);
            }else {
                logger.warn("can not get serviceName and version for {}",key);
            }
        }
        try {
            RpcProtocol rpcProtocol = new RpcProtocol(host,port,infos);
            byte[] bytes = rpcProtocol.toJson().getBytes();
            String path = Constant.ZK_DATA_PATH + "-" + rpcProtocol.hashCode();
            path = client.createPathData(path,bytes);
            pathList.add(path);
            logger.info("Registry {} new Service, host : {} , port : {}",infos.size(),host,port);
        } catch (Exception e) {
            logger.error("Registry service fail, exception : {}",e.getMessage());
        }
        client.addConnectionListener((framework, state) -> {
            if (state == ConnectionState.RECONNECTED){
                logger.info("Connection state:{}, registry service after reconnected",state);
                registryService(host, port, serviceMap);
            }
        });
    }

    public void closeService(){
        logger.info("destroy all service");
        for (String path : pathList) {
            try {
                client.deletePath(path);
            } catch (Exception e) {
                logger.error("delete service error path:{}",path);
            }
        }
        client.close();
    }
}
