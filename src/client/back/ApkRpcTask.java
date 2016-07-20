package client.back;

import constant.ApkRpcMessage;

/**
 * RCP应答处理
 * luoyg
 * 2016-02-25
 */
public class ApkRpcTask implements Runnable {

	private final ApkRpcMessage.RpcMessage Msg;
	
	public ApkRpcTask(ApkRpcMessage.RpcMessage msg){
		Msg = msg;
	}

	@Override
	public void run() {
		System.out.println("客户端处理："+Msg.getType()+","+Msg.getFrom()+","+Msg.getTo()+","+Msg.getParms()+","+Msg.getTime());
	}
}
