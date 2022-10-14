package rpc.client.handler;

public interface AsyncCallback {
    void success(Object res);
    void fail(Exception error);
}
