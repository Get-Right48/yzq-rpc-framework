package rpc.test.service;

public class HelloServiceImpl2 implements HelloService {
    @Override
    public String hello(String name) {
        return name;
    }
}
