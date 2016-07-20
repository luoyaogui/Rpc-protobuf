package server.pool;

import constant.ApkRpcMessage;
import server.res.ApkRpcResp;

/**
 * DVR-RPC监听处理
 * luoyg
 * 2016-02-25
 */
public class ApkRpcTask implements Runnable {

	private ApkRpcMessage.RpcMessage Msg;
	private long sessID;
	
	public ApkRpcTask(ApkRpcMessage.RpcMessage msg,long sessid){
		Msg = msg;
		sessID = sessid;
	}
	
	@Override
	public void run() {
		System.out.println("服务端处理："+Msg.getType()+","+Msg.getFrom()+","+Msg.getTo()+","+Msg.getParms()+","+Msg.getTime());
		ApkRpcResp.call(Msg.getType()+1, ""+sessID, Msg.getTo(), Msg.getFrom(), Msg.getParms(), Msg.getTime());//回复
	}
}
