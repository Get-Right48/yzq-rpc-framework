package rpc.client.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpc.codec.RpcRequest;
import rpc.codec.RpcResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class RpcFuture implements Future<Object> {
    private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    private final long timeThreshold = 5000l;
    private ReentrantLock lock = new ReentrantLock();
    private List<AsyncCallback> callbacks = new ArrayList<>();

    public RpcFuture(RpcRequest request){
        this.request = request;
        this.sync = new Sync();
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        if (this.response != null){
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        if (success){
            if (this.response != null){
                return this.response.getResult();
            }else {
                return null;
            }
        }else{
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    public void done(RpcResponse response){
        this.response = response;
        // 唤醒等待响应的线程
        sync.release(1);

    }

    /**
     * acquire时不满足条件阻塞等待
     * 等待response响应唤醒阻塞线程
     */
    static class Sync extends AbstractQueuedSynchronizer {
        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }
}
