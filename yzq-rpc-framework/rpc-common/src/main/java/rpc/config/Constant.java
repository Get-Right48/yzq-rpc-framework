package rpc.config;

public class Constant {
    public static final int ZK_SESSION_TIMEOUT = 5000;
    public static final int ZK_CONNECTION_TIMEOUT = 5000;
    public static final String ZK_REGISTRY_PREFIX = "/registry";
    public static final String ZK_DATA_PATH = ZK_REGISTRY_PREFIX+"/data";
    public static final String ZK_NAMESPACE = "netty-rpc";
}
