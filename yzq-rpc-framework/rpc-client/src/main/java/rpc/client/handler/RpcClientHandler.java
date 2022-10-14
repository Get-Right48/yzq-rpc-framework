package rpc.client.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.client.connect.ConnectionManager;
import rpc.codec.Beat;
import rpc.codec.RpcRequest;
import rpc.codec.RpcResponse;
import rpc.protocol.RpcProtocol;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    private ConcurrentHashMap<String,RpcFuture> pendingMap = new ConcurrentHashMap<>();
    private Channel channel;
    private RpcProtocol rpcProtocol;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        String responseId = msg.getResponseId();
        logger.info("Receive response:{}",responseId);
        RpcFuture future = pendingMap.get(responseId);
        if (future != null){
            pendingMap.remove(responseId);
            future.done(msg);
        }else {
            logger.error("can not get syncFuture for response id:{}",responseId);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("client caught exception:{}",cause.getMessage());
        ctx.close();
    }

    public void close(){
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    public RpcFuture sendRequest(RpcRequest request){
        RpcFuture future = new RpcFuture(request);
        pendingMap.put(request.getRequestId(),future);
        try {
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()){
                logger.info("Send request id:{} error",request.getRequestId());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return future;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                sendRequest(Beat.BEAT_PING);
                logger.info("client send ping");
            }else if (idleStateEvent.state() == IdleState.READER_IDLE){
                logger.info("connected timeout");
                ctx.channel().close();
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void setRpcProtocol(RpcProtocol rpcProtocol) {
        this.rpcProtocol = rpcProtocol;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcProtocol);
    }
}
