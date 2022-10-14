package rpc.codec;

import java.io.Serializable;

public class RpcResponse implements Serializable {
    private String responseId;

    private Object result;

    private String error;

    public boolean isError() {
        return error != null;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String requestId) {
        this.responseId = requestId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
