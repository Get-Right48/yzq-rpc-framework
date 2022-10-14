package rpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.serializer.Serializer;

public class RpcEncode extends MessageToByteEncoder {
    private static final Logger logger = LoggerFactory.getLogger(RpcEncode.class);
    private Class<?> genericClass;
    private Serializer serializer;

    public RpcEncode(Class<?> genericClass,Serializer serializer){
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (genericClass.isInstance(msg)){
            byte[] bytes = this.serializer.serializer(msg);
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }
}
