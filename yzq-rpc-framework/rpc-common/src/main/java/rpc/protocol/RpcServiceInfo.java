package rpc.protocol;

import rpc.util.JsonUtil;

import java.io.Serializable;
import java.util.Objects;

public class RpcServiceInfo implements Serializable {
    private String serviceName;

    private String version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RpcServiceInfo that = (RpcServiceInfo) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(version, that.version);
    }


    @Override
    public int hashCode() {
        return Objects.hash(serviceName, version);
    }

    public String toJson(){
        return JsonUtil.objectToJson(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
