package rpc.client.route;

import rpc.client.handler.RpcClientHandler;
import rpc.protocol.RpcProtocol;
import rpc.protocol.RpcServiceInfo;
import rpc.util.ServiceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RpcLoadBalance {
    // group by service name
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNode){
        Map<String,List<RpcProtocol>> serviceMap = new HashMap<>();
        if (!connectedServerNode.isEmpty()){
            for (RpcProtocol rpcProtocol : connectedServerNode.keySet()) {
                for (RpcServiceInfo rpcServiceInfo : rpcProtocol.getServiceInfoList()) {
                    String key = ServiceUtil.makeServiceKey(rpcServiceInfo.getServiceName(), rpcServiceInfo.getVersion());
                    List<RpcProtocol> list = serviceMap.get(key);
                    if (list == null){
                        list = new ArrayList<>();
                    }
                    list.add(rpcProtocol);
                    serviceMap.putIfAbsent(key,list);
                }
            }
        }
        return serviceMap;
    }

    public abstract RpcProtocol route(String serviceKey,Map<RpcProtocol, RpcClientHandler> connectedServerNode) throws Exception;
}
