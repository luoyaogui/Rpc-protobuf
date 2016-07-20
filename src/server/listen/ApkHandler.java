package server.listen;

import constant.ApkRpcMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import server.manager.ApkRpcSessManager;
import server.pool.ApkRpcThreadPool;
/**
 * netty服务器端数据处理handler：
 * 			1，管理channel
 * 			2，生产唯一标识ID，和channel作为key-value保存
 * 			3，接受数据并放入线程池中进行解析处理
 * @author luoyg
 *	2015-11-10
 */
public class ApkHandler extends ChannelHandlerAdapter {
	private long ID = 0;
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("----------------------------channelActive=--------");
		ApkRpcSessManager.channelGroup.add(ctx.channel());
		ID = ApkRpcSessManager.sessionIDS.incrementAndGet();
		ApkRpcSessManager.addChannel(ID, ctx.channel());
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("----------------------------channelInactive=--------");
		ApkRpcSessManager.channelGroup.remove(ctx.channel());
		ApkRpcSessManager.removeChannel(ID);
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		System.out.println("----------------------------get=--------"+msg);
		ApkRpcThreadPool.callHandle((ApkRpcMessage.RpcMessage)msg,ID);//处理
		ReferenceCountUtil.release(msg);//添加防止内存泄漏的
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}