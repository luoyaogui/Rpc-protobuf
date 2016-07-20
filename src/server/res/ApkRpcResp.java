package server.res;

import server.manager.ApkRpcSessManager;

/**
 * dvr的RPC应答具体实现
 * 2016-02-25
 * luoyg
 */
public class ApkRpcResp {
	//回复
	public static void call(int type, String sessID, String dvrNum, String userName, String params, String time){
		ApkRpcSessManager.writeChannel(sessID, type, dvrNum, userName, params, time);//根据原来的socket会话通道进行回复
	}
}
