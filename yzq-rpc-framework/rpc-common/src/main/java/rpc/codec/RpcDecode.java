package rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.serializer.Serializer;

import java.util.List;

public class RpcDecode extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcDecode.class);
    private Class<?> genericClass;
    private Serializer serializer;

    public RpcDecode(Class<?> genericClass,Serializer serializer){
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) return;
        in.markReaderIndex();
        int dataLen = in.readInt();
        // 半包
        if (in.readableBytes() < dataLen){
            in.resetReaderIndex();
            return;
        }
        byte[] bytes = new byte[dataLen];
        in.readBytes(bytes);
        Object obj = null;
        try {
            obj = serializer.deserializer(bytes, genericClass);
            out.add(obj);
        } catch (Exception e) {
            logger.error("decode error :{}",e.getMessage());
        }
    }
}
