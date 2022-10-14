package rpc.test.server;

import rpc.server.RpcServer;
import rpc.test.service.HelloService;
import rpc.test.service.HelloServiceImpl;
import rpc.test.service.HelloServiceImpl2;

public class ServerBootstrap {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:6666";
        String registryAddr = "192.168.144.128:2181";
        RpcServer rpcServer = new RpcServer(serverAddress, registryAddr);
        HelloServiceImpl helloService = new HelloServiceImpl();
        rpcServer.addService(HelloService.class.getName(),"1.0",helloService);
        HelloService hello2 = new HelloServiceImpl2();
        rpcServer.addService(HelloService.class.getName(),"2.0",hello2);
        try {
            rpcServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
