package server.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import constant.ApkRpcMessage;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
/**
 * 功能1:生成唯一的会话ID
 * 功能2:管理所有连接的channel，方便使用同一channel通道回复
 * 功能3:管理会话ID对应channel，包括增加、删除、写数据
 * luoyg
 */

public class ApkRpcSessManager {
	public static AtomicLong sessionIDS = new AtomicLong(0);
	public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private static Map<String,Channel> sessionGroup = new ConcurrentHashMap<String,Channel>();

	private ApkRpcSessManager(){
	}

	public static void addChannel(Long sessID,Channel chnn){
		sessionGroup.put(sessID+"", chnn);
	}

	public static void removeChannel(Long sessID){
		Channel chnn = sessionGroup.remove(sessID+"");
		if(chnn != null && chnn.isActive()){
			chnn.close();
		}
	}

	public static void writeChannel(String sessID,int type,String from,String to,String params,String time){
		try{
			System.out.println("[rpc response]"+type+"  -  "+from+"  -  "+to+"  -  "+params+"  -  "+time);
			sessionGroup.get(sessID).writeAndFlush(createRpcMessage(type,from,to,params,time));
		}catch(NullPointerException e){}
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