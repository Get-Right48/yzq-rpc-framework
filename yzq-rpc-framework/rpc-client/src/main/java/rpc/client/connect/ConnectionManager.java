package rpc.client.connect;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.client.handler.RpcClientHandler;
import rpc.client.handler.RpcClientInitializer;
import rpc.client.route.RpcLoadBalance;
import rpc.client.route.impl.RoundRobinRpcLoadBalance;
import rpc.protocol.RpcProtocol;
import rpc.protocol.RpcServiceInfo;
import rpc.util.ThreadPoolUtil;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private volatile static  ConnectionManager instance;
    // 多个客户端连接，要保证线程安全
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2,
            2,600L,
            TimeUnit.SECONDS,new LinkedBlockingQueue<>(100));

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static final long waitTimeout = 30l;

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private volatile  boolean isRunning = true;

    private RpcLoadBalance loadBalance = new RoundRobinRpcLoadBalance();

    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private ConnectionManager(){}

    public static ConnectionManager getInstance(){
        if (instance == null){
            synchronized (ConnectionManager.class){
                if (instance == null){
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList){
        if (serviceList != null && serviceList.size() > 0){
            HashSet<RpcProtocol> serviceSet = new HashSet<>();
            for (RpcProtocol rpcProtocol : serviceList) {
                serviceSet.add(rpcProtocol);
            }
            for (RpcProtocol rpcProtocol : serviceSet) {
                if(!rpcProtocolSet.contains(rpcProtocol)){
                    connectServerNode(rpcProtocol);
                }
            }
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)){
                    logger.info("delete service:{}",rpcProtocol.toJson());
                    removeAndCloseHandler(rpcProtocol);
                }
            }
        }else {
            logger.error("no usable service");
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                removeAndCloseHandler(rpcProtocol);
            }
        }
    }

    private void connectServerNode(RpcProtocol protocol){
        if (protocol.getServiceInfoList() == null || protocol.getServiceInfoList().isEmpty()){
            // 并没有服务注入zookeeper
            logger.info("no server on node,host:{},port:{}",protocol.getHost(),protocol.getPort());
            return;
        }
        rpcProtocolSet.add(protocol);
        logger.info("new service info,host:{},port:{}",protocol.getHost(),protocol.getPort());
        for (RpcServiceInfo info : protocol.getServiceInfoList()) {
            logger.info("new service info,name:{},version:{}",info.getServiceName(),info.getVersion());
        }
        final InetSocketAddress addr = new InetSocketAddress(protocol.getHost(),protocol.getPort());
        threadPoolExecutor.execute(() -> {
            Bootstrap boot = new Bootstrap();
            boot.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new RpcClientInitializer());
            ChannelFuture future = boot.connect(addr);
            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()){
                    logger.info("connect is success addr:{}",addr);
                    RpcClientHandler clientHandler = future1.channel().pipeline().get(RpcClientHandler.class);
                    connectedServerNodes.put(protocol,clientHandler);
                    clientHandler.setRpcProtocol(protocol);
                    signalAllHandler();
                }else{
                    logger.error("can not connect to addr:{}",addr);
                }
            });
        });
    }

    private void signalAllHandler(){
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }


    private void awaitForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.info("waiting for service");
            condition.await(waitTimeout,TimeUnit.SECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        while (isRunning && size == 0){
            try {
                awaitForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("waiting service is interrupted");
            }
        }
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        RpcClientHandler clientHandler = connectedServerNodes.get(rpcProtocol);
        return clientHandler;
    }

    public void removeAndCloseHandler(RpcProtocol rpcProtocol){
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) handler.close();
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
    }

    public void removeHandler(RpcProtocol rpcProtocol){
        rpcProtocolSet.remove(rpcProtocol);
        connectedServerNodes.remove(rpcProtocol);
    }

    public void stop(){
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            removeAndCloseHandler(rpcProtocol);
        }
        signalAllHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    public void updateConnectedServer(RpcProtocol protocol, PathChildrenCacheEvent.Type type) {
        if (protocol == null) return;
        if (type == PathChildrenCacheEvent.Type.CHILD_ADDED && !rpcProtocolSet.contains(protocol)){
            connectServerNode(protocol);
        }else if (type == PathChildrenCacheEvent.Type.CHILD_REMOVED){
            removeAndCloseHandler(protocol);
        }else if (type == PathChildrenCacheEvent.Type.CHILD_UPDATED){
            removeAndCloseHandler(protocol);
            connectServerNode(protocol);
        }
    }
}
