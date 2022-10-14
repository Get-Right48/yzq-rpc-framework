package rpc.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {
    public static ThreadPoolExecutor makerServiceThreadPool(String serviceName,int coreSize,int maxSize){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(coreSize, maxSize,
                100L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000),
                r -> new Thread(r,"netty-rpc-"+serviceName+"-"+r.hashCode()),
                new ThreadPoolExecutor.AbortPolicy());
        return executor;
    }
}
