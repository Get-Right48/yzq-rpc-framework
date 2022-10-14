package rpc.protocol;

import rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class RpcProtocol implements Serializable {
    private String host;

    private Integer port;

    private List<RpcServiceInfo> serviceInfos;

    public RpcProtocol(){}

    public RpcProtocol(String host,Integer port,List<RpcServiceInfo> serviceInfos){
        this.host = host;
        this.port = port;
        this.serviceInfos = serviceInfos;
    }


    public static RpcProtocol fromJson(String json){
        return JsonUtil.jsonToObject(json,RpcProtocol.class);
    }

    private static boolean isListEquals(List<RpcServiceInfo> t1, List<RpcServiceInfo> t2){
        if(t1 == null && t2 == null) return true;
        if ((t1 == null && t2 != null)
                || (t1 != null && t2 == null)
                || (t1.size() != t2.size()))
            return false;
        return t1.containsAll(t2) && t2.contains(t1);
    }

    public String toJson(){
        return JsonUtil.objectToJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcProtocol that = (RpcProtocol) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                isListEquals(serviceInfos, that.getServiceInfoList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, serviceInfos);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getServiceInfoList() {
        return serviceInfos;
    }

    public void setServiceInfoList(List<RpcServiceInfo> serviceInfoList) {
        this.serviceInfos = serviceInfoList;
    }
}
