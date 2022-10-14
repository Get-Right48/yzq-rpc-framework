package rpc.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rpc.annotation.RpcAutowired;
import rpc.client.connect.ConnectionManager;
import rpc.client.discovery.ServiceDiscovery;
import rpc.client.proxy.ObjectProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class RpcClient implements ApplicationContextAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

    private ServiceDiscovery discovery;

    public RpcClient(String addr){
        this.discovery = new ServiceDiscovery(addr);
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null){
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean,createService(field.getType(),version));
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        discovery.close();
        ConnectionManager.getInstance().stop();
    }



    @SuppressWarnings("unchecked")
    public static <T, P> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T, P>(interfaceClass, version)
        );
    }
}
