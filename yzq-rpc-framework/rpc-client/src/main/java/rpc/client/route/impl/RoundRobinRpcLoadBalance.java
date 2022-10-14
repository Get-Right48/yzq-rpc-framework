package rpc.client.route.impl;

import rpc.client.handler.RpcClientHandler;
import rpc.client.route.RpcLoadBalance;
import rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRpcLoadBalance extends RpcLoadBalance {

    private AtomicInteger roundRobin = new AtomicInteger(0);

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNode) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNode);
        List<RpcProtocol> protocols = serviceMap.get(serviceKey);
        if (protocols != null && protocols.size() > 0){
            return doRoute(protocols);
        }else {
            throw new Exception("can not find connected for serviceKey:"+serviceKey);
        }
    }


    private RpcProtocol doRoute(List<RpcProtocol> protocols) {
        int index = (roundRobin.addAndGet(1)) % protocols.size();
        return protocols.get(index);
    }
}
