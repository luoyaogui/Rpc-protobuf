package client.req;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import client.internal.ApkConnPool;
import constant.ApkRpcMessage;



/**
 * 思想：通过netty传输对象，实现类似RPC操作。
 * 		1，具备IP和端口号，所以ApkRpcThreadPool中的Map的key为IP+port，而value为socket连接池.
 * 		2，一个RPC可能多任务而且公用一个channel连接，所以ApkConnPool中作为channel的池管理
 * 		3，为了保证每个RPC都相互独立，使用任务的具体调用交给ApkConnPool处理，保证每个RPC的独立性
 * @author luoyg
 * 2015-10-28
 */
public class ApkReqThreadPool {
	
	private static final int timeout;
	public static final Map<String,ApkConnPool> nRpcs;
	private static final ExecutorService pool;

	static {  
		timeout = 10000;//超时10秒
		nRpcs = new ConcurrentHashMap<String, ApkConnPool>();
		//pool = Executors.newFixedThreadPool(1);
		//CallerRunsPolicy：线程调用运行该任务的 execute 本身
		//AbortPolicy：处理程序遭到拒绝将抛出运行时RejectedExecutionException
		//DiscardPolicy：不能执行的任务将被删除
		//DiscardOldestPolicy：如果执行程序尚未关闭，则位于工作队列头部的任务将被删除，然后重试执行程序（如果再次失败，则重复此过程）
		pool = new ThreadPoolExecutor(8, 8, timeout,TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(10000),new ThreadPoolExecutor.DiscardOldestPolicy());
	} 
	private ApkReqThreadPool(){
	}
	//根据IP+端口号查询连接池，没有则创建连接池并初始化
	public static void callRpc(final String host,final int port,final int type,final String from,final String to,final String params,final String time){
		if(!nRpcs.containsKey(host+port))
			nRpcs.put(host+port, new ApkConnPool(host,port));
		pool.execute(new Runnable(){
			private long joinTime = System.currentTimeMillis();
			@Override
			public void run() {
				System.out.println("----------------------run-------------------+");
				Channel chnn = null;
				try{
					do{//循环调用
						if((System.currentTimeMillis() - joinTime) > timeout){//需要超时检测且已经超时
							System.out.println("---------------- rpc  timeout------------------");
							break;
						}
						if(nRpcs.get(host+port).getIsAvailable()){//判断是否可用
							chnn = nRpcs.get(host+port).getConnection();//从连接池中获取channel
							System.out.println("可写？"+chnn.isWritable()+","+chnn.isActive());
							if(chnn != null){//可写
								System.out.println("----------------------sending-------------------+");
								chnn.writeAndFlush(createRpcMessage(type,from,to,params,time));//发送消息
								nRpcs.get(host+port).releaseChannel(chnn);//回收channel到连接池中
								chnn = null;
								break;
							}
						}else{
							System.out.println("----------------getIsAvailable false------------------");
							break;
						}
					}while(true);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		});
	}
	//创建RPC消息
	private static ApkRpcMessage.RpcMessage createRpcMessage(int type,String from,String to,String params,String time) {
		ApkRpcMessage.RpcMessage.Builder builder = ApkRpcMessage.RpcMessage.newBuilder();
		builder.setType(type);
		builder.setFrom(from);
		builder.setTo(to);
		builder.setParms(params);
		builder.setTime(time);
		return builder.build();
	}
}