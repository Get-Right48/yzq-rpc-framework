package rpc.client.route.impl;

import rpc.client.handler.RpcClientHandler;
import rpc.client.route.RpcLoadBalance;
import rpc.protocol.RpcProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRURpcLoadBalance extends RpcLoadBalance {

    private ConcurrentHashMap<String, LinkedHashMap<RpcProtocol,RpcProtocol>> lruMap =
            new ConcurrentHashMap<>();

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNode) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNode);
        List<RpcProtocol> rpcProtocols = serviceMap.get(serviceKey);
        if (!rpcProtocols.isEmpty()){
            return doRoute(serviceKey,rpcProtocols);
        }else {
            throw new Exception("can not find connection for serviceKey="+serviceKey);
        }
    }

    private RpcProtocol doRoute(String serviceKey, List<RpcProtocol> rpcProtocols) {
        LinkedHashMap<RpcProtocol, RpcProtocol> linkedHashMap = lruMap.get(serviceKey);
        if (linkedHashMap == null){
            linkedHashMap = new LinkedHashMap<RpcProtocol, RpcProtocol>(16,0.75f,true){
                @Override
                protected boolean removeEldestEntry(Map.Entry<RpcProtocol, RpcProtocol> eldest) {
                    if (super.size() > 100){
                        return true;
                    }else {
                        return false;
                    }
                }
            };
            lruMap.putIfAbsent(serviceKey,linkedHashMap);
        }
        // put new and remove old
        linkedHashMap.clear();
        for (RpcProtocol rpcProtocol : rpcProtocols) {
            if (!linkedHashMap.containsKey(rpcProtocol)){
                linkedHashMap.put(rpcProtocol,rpcProtocol);
            }
        }
        return linkedHashMap.entrySet().iterator().next().getKey();
    }
}
