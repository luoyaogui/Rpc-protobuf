package client.back;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import constant.ApkRpcMessage;



/**
 * 设置一个线程池为 处理业务线程，根据实际硬件配置，设置参数
 * @author luoyg
 *
 */
public class ApkRpcThreadPool {

	private static ExecutorService nPool;

	static {  
		nPool = new ThreadPoolExecutor(
				4, 4, 10, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
	} 
	private ApkRpcThreadPool(){
	}

	public static void callHandle(ApkRpcMessage.RpcMessage msg){
		//if(msg.getType() != ApkRpcExternalType.heartbeat)
		nPool.execute(new ApkRpcTask(msg));
	}
}
