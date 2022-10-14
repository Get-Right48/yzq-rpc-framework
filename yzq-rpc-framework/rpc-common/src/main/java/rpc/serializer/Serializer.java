package rpc.serializer;

public abstract class Serializer {
    public abstract <T> byte[] serializer(T obj);

    public abstract <T> Object deserializer(byte[] bytes,Class<T> cls);
}
