package rpc.serializer.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import rpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianSerializer extends Serializer {


    @Override
    public <T> byte[] serializer(T obj) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Hessian2Output ho = new Hessian2Output(os);
        try {
            ho.writeObject(obj);
            ho.flush();
            byte[] result = os.toByteArray();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                ho.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                os.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> Object deserializer(byte[] bytes, Class<T> cls) {
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Hessian2Input hi = new Hessian2Input(is);
        try {
            Object result = hi.readObject();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                hi.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}