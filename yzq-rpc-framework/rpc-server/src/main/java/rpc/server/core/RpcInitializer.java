package rpc.server.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import rpc.codec.RpcDecode;
import rpc.codec.RpcEncode;
import rpc.codec.RpcRequest;
import rpc.codec.RpcResponse;
import rpc.serializer.Serializer;
import rpc.serializer.kryo.KryoSerializer;
import rpc.util.ThreadPoolUtil;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RpcInitializer extends ChannelInitializer<SocketChannel> {

    private Map<String,Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public RpcInitializer(Map<String,Object> serviceMap, ThreadPoolExecutor threadPoolExecutor){
        this.handlerMap = serviceMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        Serializer serializer = new KryoSerializer();
        ChannelPipeline pipeline = ch.pipeline();
        // 心跳检测处理器
        pipeline.addLast(new IdleStateHandler(0,0,500, TimeUnit.SECONDS));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(66536,0,4,0,0));
        pipeline.addLast(new RpcEncode(RpcResponse.class,serializer));
        pipeline.addLast(new RpcDecode(RpcRequest.class,serializer));
        pipeline.addLast(new RpcServerHandler(handlerMap,threadPoolExecutor));
    }
}
