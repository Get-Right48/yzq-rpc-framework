package rpc.test.service;

import rpc.annotation.NettyRpcService;

@NettyRpcService(value = HelloService.class,version = "1")
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl(){}

    @Override
    public String hello(String name) {
        return name;
    }
}
