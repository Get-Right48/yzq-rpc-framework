package rpc.server.core;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.sf.cglib.reflect.FastClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.codec.Beat;
import rpc.codec.RpcRequest;
import rpc.codec.RpcResponse;
import rpc.util.ServiceUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private Map<String,Object> handlerMap;
    private ThreadPoolExecutor threadPoolExecutor;

    public RpcServerHandler(Map<String,Object> handlerMap, ThreadPoolExecutor threadPoolExecutor){
        this.handlerMap = handlerMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        threadPoolExecutor.execute(() -> {
            logger.info("service Request id:{}",msg.getRequestId());
            RpcResponse response = new RpcResponse();
            response.setResponseId(msg.getRequestId());
            try {
                Object res = handle(msg);
                response.setResult(res);
            } catch (InvocationTargetException e) {
                response.setError(e.toString());
                logger.error("RPC server handler request error:{}",e.getMessage());
            }
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                logger.info("send response "+ response.getResponseId());
            });
        });
    }

    private Object handle(RpcRequest request) throws InvocationTargetException {
        String className = request.getClassName();
        String version = request.getVersion();
        String key = ServiceUtil.makeServiceKey(className,version);
        Object target = handlerMap.get(key);
        logger.info("request service key:{}",key);
        if (target == null){
            logger.error("Can not find serviceBean with classname:{} and version:{}",className,version);
            return null;
        }
        Class<?> tarClass = target.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        // Cglib 反射调用目标方法
        FastClass tarFastClass = FastClass.create(tarClass);
        int methodIdx = tarFastClass.getIndex(methodName, parameterTypes);
        return tarFastClass.invoke(methodIdx,target,parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException){
            logger.error("client connection termination");
            ctx.channel().close();
        }else {
            logger.error("{}",cause);
        }
    }
}
