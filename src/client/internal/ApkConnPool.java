package client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import constant.ApkRpcMessage;

/**
 * netty客户端的channel连接池管理
 * 2015-12-2
 * luoyg
 */
public class ApkConnPool {
	private EventLoopGroup group;//非阻塞IO线程组
	private Bootstrap boot;//主
	private Map<Channel, Boolean> map;//存放channel及其是否可用的状态
	private volatile boolean isAvailable = false;//是否连接的服务器可用
	private volatile boolean wait = true;//等待isAvailable验证

	private String Host;//要连接的主机IP
	private int Port;//要连接主机的监听端口号
	private int maxPoolSize = 50;//channel数量最大值
	private int waitTime = 100;//等待超时时间

	public ApkConnPool(String host,int port) {
		Host = host;
		Port = port;
		init();
	}
	//创建netty客户端，配置参数，启动连接
	//创建channel并放入map中，配置属性为可用
	private void init() {
		map = new HashMap<Channel, Boolean>();
		group = new NioEventLoopGroup();
		boot = new Bootstrap();
		boot.group(group)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>(){
				@Override
				protected void initChannel(SocketChannel arg0) {
					arg0.pipeline().addLast(new ProtobufVarint32FrameDecoder());
					arg0.pipeline().addLast(new ProtobufDecoder(ApkRpcMessage.RpcMessage.getDefaultInstance()));
					arg0.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
					arg0.pipeline().addLast(new ProtobufEncoder());
					arg0.pipeline().addLast(new ApkHandler(Host,Port));
				}
		});
	}
	//创建新的channel
	private Channel getNewChannel(){
		if(!getIsAvailable())//不可用时，直接返回NULL
			return null;
		try {
			return boot.connect(Host, Port).sync().channel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	//循环查询map中是否有可用的channel
	private Channel loopCheck(){
		Channel conn = null;
		for (Entry<Channel, Boolean> entry : map.entrySet()) {
			if (entry.getValue()) {
				conn = entry.getKey();
				map.put(conn, false);//设置状态的不可用
				break;
			}
		}
		return conn;
	}
	//判断连接是否可用
	public boolean getIsAvailable(){
		if(!isAvailable){
			wait = true;
			//添加监听器确定连接是否可用
			boot.connect(Host, Port).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if (f.isSuccess()) {
						isAvailable = true;
					}else{
						isAvailable = false;
					}
					wait = false;//已经处理完成
				}
			});
			while(wait){//等待上述监听处理完成
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return isAvailable;
	}
	//获得map中可用的channel，没有则判断是否达到配置最大值（没有则创建新的channel，否则等待5秒后再次查询map）
	public synchronized Channel getConnection() {
		Channel conn = null;
		try {
			conn = loopCheck();
			if (conn == null) {
				if (map.size() < maxPoolSize) {//可创建新的chanel
					conn = getNewChannel();
					map.put(conn, false);
				} else {
					wait(waitTime);//等待
					conn = loopCheck();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	//回收channel到map中
	public void releaseChannel(Channel conn) {
		if (conn == null) {
			return;
		}
		if(map.containsKey(conn)) {
			if (!conn.isWritable()) {//如果已经不可写则关闭移除
				map.remove(conn);
				if(map.isEmpty()){//如果已经清空，则把可用状态置为flase，保证建立新的连接时检测是否可用
					isAvailable = false;
				}
			} else {
				map.put(conn, true);//修改状态为可用
			}
		} else {
			if(conn.isWritable()){//如果是可写的则关闭
				conn.close();
			}
		}
	}
}