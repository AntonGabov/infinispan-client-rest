package org.infinispan.client.rest.impl.transport.http;

import org.infinispan.client.rest.configuration.Configuration;
import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.marshall.MarshallUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpTransport implements Transport {

   //private static final Log log = LogFactory.getLog(HttpTransport.class);

   private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

   private Bootstrap bootstrap;
   private EventLoopGroup workerGroup;
   private Configuration configuration;

   @Override
   public void start(Configuration configuration) {
      this.configuration = configuration;
      workerGroup = new NioEventLoopGroup();
      bootstrap = new Bootstrap().group(workerGroup).channel(NioSocketChannel.class);
      bootstrap.handler(new ChannelInitializer<SocketChannel>() {
         @Override
         protected void initChannel(SocketChannel ch) {
            ch.pipeline().addLast(new HttpClientCodec());
         }
      });
      bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
   }

   @Override
   public void stop() {
      workerGroup.shutdownGracefully();
   }

   @Override
   public void write(Object cacheName, Object key, Object value) {
      configuration.servers().forEach(server -> write(server, cacheName, key, value));
   }

   private void write(ServerConfiguration server, Object cacheName, Object key, Object value) {
      ByteBuf content = Unpooled.wrappedBuffer(MarshallUtil.obj2byteArray(value));

      DefaultFullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
            content);
      put.headers().add("Content-Length", content.readableBytes());

      Channel ch = bootstrap.connect(server.getHost(), Integer.valueOf(server.getPort())).awaitUninterruptibly().channel().pipeline()
            .addLast(new HttpResponseHandler()).channel();
      try {
         ch.writeAndFlush(put).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
         //log.warn("Cannot perform a PUT request");
      }
   }
   
   @Override
   public Object read(Object cacheName, Object key) {
      Object data = null;
      for (int i = 0; i < configuration.servers().size(); i++) {
         byte[] readInfo = read(configuration.servers().get(i), cacheName, key);
         if (readInfo != null) {
            data = MarshallUtil.byteArray2Object(readInfo);
            break;
         }
      } 
      return data;
   }

   private byte[] read(ServerConfiguration server, Object cacheName, Object key) {
      byte[] data = null;
      try {
         DefaultHttpRequest get = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, getUri(cacheName, key));
         HttpResponseHandler handler = new HttpResponseHandler(true);
         Channel ch = bootstrap.connect(server.getHost(), Integer.valueOf(server.getPort())).awaitUninterruptibly().channel().pipeline()
               .addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH), handler).channel();
         ch.writeAndFlush(get).sync().channel().closeFuture().sync();

         FullHttpResponse response = handler.getResponse();
         try {
            if (HttpResponseStatus.OK.equals(response.status())) {
               ByteBuf content = response.content();
               data = new byte[content.readableBytes()];
               content.readBytes(data);
            } else {
               //log.warn("Something wrong with response:\n" + response.status());
            }
         } finally {
            response.release();
         }
      } catch (InterruptedException e1) {
         //log.warn("Cannot perform a GET request");
      }
      return data;
   }

}
