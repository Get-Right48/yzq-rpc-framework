package rpc.server;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rpc.annotation.NettyRpcService;
import rpc.server.core.NettyServer;

import java.util.Map;

public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    public RpcServer(String serverAddress, String registryAddr) {
        super(serverAddress, registryAddr);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> map = applicationContext.getBeansWithAnnotation(NettyRpcService.class);
        if (MapUtils.isNotEmpty(map)){
            for (Object bean : map.values()) {
                NettyRpcService annotation = bean.getClass().getAnnotation(NettyRpcService.class);
                String serviceName = annotation.value().getName();
                String version = annotation.version();
                super.addService(serviceName,version,bean);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }
}
