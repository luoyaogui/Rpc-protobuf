package client.internal;

import client.back.ApkRpcThreadPool;
import client.req.ApkReqThreadPool;
import constant.ApkRpcMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
/**
 * netty客户端的处理handler
 * 2015-12-2
 * luoyg
 */
public class ApkHandler extends ChannelHandlerAdapter {
	private String Host;
	private int Port;
	
	public ApkHandler(String host,int port){
		Host = host;
		Port = port;
	}
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ApkReqThreadPool.nRpcs.get(Host+Port).releaseChannel(ctx.channel());
		Host = null;
	}
	
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		ApkRpcThreadPool.callHandle((ApkRpcMessage.RpcMessage)msg);//放入解析线程池中
		ReferenceCountUtil.release(msg);//添加防止内存泄漏的
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		ctx.close();
	}
}
