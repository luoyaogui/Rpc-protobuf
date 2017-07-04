package client;


import java.util.concurrent.TimeUnit;

import client.req.ApkReqThreadPool;
import server.listen.ApkRpcServer;

public class Main {

	public static void main(String[] args) {
		for(int index=0;index<100;index++)
			ApkReqThreadPool.callRpc("*",0, index, "from-me", "to-your", "no params", "20160607-08:42:24");
	}
}
