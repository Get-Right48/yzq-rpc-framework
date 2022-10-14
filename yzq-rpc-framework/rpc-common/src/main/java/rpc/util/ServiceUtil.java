package rpc.util;

public class ServiceUtil {
    public final static String SERVICE_CONCAT_TOKEN = "#";
    public static String makeServiceKey(String serviceName,String version){
        if (version != null && version.trim().length() > 0){
            serviceName += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceName;
    }
}
