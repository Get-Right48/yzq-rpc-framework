package rpc.test.client;

import rpc.client.RpcClient;
import rpc.test.service.HelloService;

public class RpcClientTest {
    public static void main(String[] args) {
        RpcClient client = new RpcClient("192.168.144.128:2181");
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                HelloService service = client.createService(HelloService.class, "2.0");
                String res = service.hello(String.valueOf(i));
                if (!res.equals(String.valueOf(i))) {
                    System.out.println("error = " + res);
                } else {
                    System.out.println("res = " + res);
                }
            }
        });
        thread.start();
        try {
            thread.join();
            System.out.println("end ...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
