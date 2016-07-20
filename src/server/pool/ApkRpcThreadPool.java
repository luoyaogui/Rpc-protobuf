package server.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import constant.ApkRpcMessage;



/**
 * 设置一个线程池为 处理业务线程，根据实际硬件配置，设置参数
 * 2016-02-25
 * luoyg
 */
public class ApkRpcThreadPool {

	private static ExecutorService nPool;
	
	static {  
		nPool = new ThreadPoolExecutor(
				4, 4, 5, TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(10000),new ThreadPoolExecutor.DiscardOldestPolicy());
	} 
	private ApkRpcThreadPool(){
	}
	
	public static void callHandle(ApkRpcMessage.RpcMessage msg,long sessID){
		nPool.execute(new ApkRpcTask(msg,sessID));
	}
}
