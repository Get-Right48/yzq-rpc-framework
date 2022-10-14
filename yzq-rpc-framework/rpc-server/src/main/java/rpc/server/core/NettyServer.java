package rpc.server.core;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.server.registry.ServiceRegistry;
import rpc.util.ServiceUtil;
import rpc.util.ThreadPoolUtil;

import javax.print.DocFlavor;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class NettyServer implements Server {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
    // 当前线程
    private Thread thread;
    // IP地址
    private String serverAddress;

    private ServiceRegistry serviceRegistry;

    private Map<String,Object> serviceMap = new HashMap<>();

    public NettyServer(String serverAddress,String registryAddr){
        this.serverAddress = serverAddress;
        this.serviceRegistry = new ServiceRegistry(registryAddr);
    }

    public void addService(String serviceName,String version, Object serviceBean){
        logger.info("add service,serviceName:{},version:{},serviceBean:{}",serviceName,version,serviceBean);
        String key = ServiceUtil.makeServiceKey(serviceName,version);
        serviceMap.put(key,serviceBean);
    }

    @Override
    public void start() throws Exception {
        thread = new Thread(new Runnable() {
            ThreadPoolExecutor threadPool = ThreadPoolUtil.makerServiceThreadPool(NettyServer.class.getSimpleName(),
                    16,16);
            @Override
            public void run() {
                NioEventLoopGroup bossGroup = new NioEventLoopGroup();
                NioEventLoopGroup workGroup = new NioEventLoopGroup();

                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new RpcInitializer(serviceMap, threadPool))
                            // tcp协议的服务端连接队列大小
                            .option(ChannelOption.SO_BACKLOG, 128)
                            // TCP的keep-alive，长时间没有数据传输时，测试连接状态
                            .childOption(ChannelOption.SO_KEEPALIVE, true);
                    String[] arr = serverAddress.split(":");
                    String host = arr[0];
                    int port = Integer.parseInt(arr[1]);
                    // 绑定端口和host，同步等待监听成功
                    Channel channel = bootstrap.bind(host, port).sync().channel();
                    if (serviceRegistry != null) {
                        serviceRegistry.registryService(host, port, serviceMap);
                    }
                    logger.info("Server started host:{} port:{}", host, port);
                    channel.closeFuture().sync();
                } catch (InterruptedException e) {
                    if (e instanceof InterruptedException) logger.info("RPC server closed...");
                    else logger.info("RPC server error", e);
                } finally {
                    workGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                    serviceRegistry.closeService();
                }
            }
        });
        thread.start();
    }

    @Override
    public void stop() throws Exception {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
