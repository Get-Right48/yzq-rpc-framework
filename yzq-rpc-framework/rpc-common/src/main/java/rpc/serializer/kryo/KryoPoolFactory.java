package rpc.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import org.objenesis.strategy.StdInstantiatorStrategy;
import rpc.codec.RpcRequest;
import rpc.codec.RpcResponse;

public class KryoPoolFactory {
    private static volatile KryoPoolFactory pollFactory = null;

    private KryoFactory factory = () -> {
        Kryo kryo = new Kryo();
        kryo.setReferences(false);
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        Kryo.DefaultInstantiatorStrategy strategy = (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
        strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    };

    private KryoPool pool = new KryoPool.Builder(factory).build();

    private KryoPoolFactory(){}

    public static KryoPool getKryoPoolInstance(){
        if (pollFactory == null){
            synchronized (KryoPoolFactory.class){
                if (pollFactory == null){
                    pollFactory = new KryoPoolFactory();
                }
            }
        }
        return pollFactory.getPool();
    }

    public KryoPool getPool() {
        return pool;
    }
}
