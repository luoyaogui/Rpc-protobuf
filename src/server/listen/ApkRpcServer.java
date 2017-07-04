package server.listen;

import java.util.concurrent.TimeUnit;


import constant.ApkRpcMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
/**
 * netty服务器端监听线程
 * 		具备自动重连功能
 * @author lyg
 *	2015-11-10
 */
public class ApkRpcServer extends Thread{

	private final String Host;
	private final int Port;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;
	private final ServerBootstrap boot;
	private volatile boolean closed = false;

	public ApkRpcServer(String host,int port){
		Host = host;
		Port = port;
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		boot = new ServerBootstrap();
	}

	@Override
	public void run() {
		//配置服务端的NIO线程组
		boot.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 1024)
			.handler(new LoggingHandler(LogLevel.INFO))
			.childHandler(new ChannelInitializer<SocketChannel>(){
	
				@Override
				protected void initChannel(SocketChannel arg0){
					arg0.pipeline().addLast(new ProtobufVarint32FrameDecoder());
					arg0.pipeline().addLast(new ProtobufDecoder(ApkRpcMessage.RpcMessage.getDefaultInstance()));
					arg0.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
					arg0.pipeline().addLast(new ProtobufEncoder());
					arg0.pipeline().addLast(new ApkHandler());
				}
	
			});
		doBind();
	}
	protected void doBind() {
		if (closed) {
			return;
		}
		boot.bind(Host, Port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				if (f.isSuccess()) {
					System.out.println("Started Tcp Server " + Host+":"+Port);
				} else {
					System.out.println("Started Tcp Server Failed " + Host+":"+Port);
					//f.channel().eventLoop().schedule(() -> doBind(), 10, TimeUnit.SECONDS);
					f.channel().eventLoop().schedule(new Runnable() {
						@Override
						public void run() {
							doBind();
						}
					}, 10, TimeUnit.SECONDS);
				}
			}
		});
	}
	
	public void close(){
		System.out.println("close Tcp Server " + Host+":"+Port);
		closed = true;
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}
	
	public static void main(String[] args) throws Exception {
		ApkRpcServer server = new ApkRpcServer("*", 0);
		server.start();
		//System.out.println((Long.parseLong("1446018761536")-Long.parseLong("1446018736639")));
	}
}
