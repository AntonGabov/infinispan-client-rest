package org.infinispan.client.rest.impl.transport.netty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.infinispan.client.rest.configuration.Server;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class NettyTransport implements Transport {

   private static final Log log = LogFactory.getLog(NettyTransport.class);

   public static final String URI_BASIS = "/rest";
   private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

   private Bootstrap bootstrap;
   private EventLoopGroup workerGroup;

   private List<Server> serverList;

   public NettyTransport(List<Server> serverList) {
      this.serverList = serverList;
   }

   @Override
   public void start() {
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
      serverList.forEach(server -> write(server, cacheName, key, value));
   }

   private void write(Server server, Object cacheName, Object key, Object value) {
      ByteBuf content = Unpooled.wrappedBuffer(obj2byteArray(value));

      DefaultFullHttpRequest put = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, getUri(cacheName, key),
            content);
      put.headers().add("Content-Length", content.readableBytes());

      Channel ch = bootstrap.connect(server.getHost(), Integer.valueOf(server.getPort())).awaitUninterruptibly().channel().pipeline()
            .addLast(new HttpResponseHandler()).channel();
      try {
         ch.writeAndFlush(put).sync().channel().closeFuture().sync();
      } catch (InterruptedException e) {
         log.warn("Cannot perform a PUT request");
      }
   }
   
   @Override
   public Object read(Object cacheName, Object key) {
      Object data = null;
      for (int i = 0; i < serverList.size(); i++) {
         byte[] readInfo = read(serverList.get(i), cacheName, key);
         if (readInfo != null) {
            data = byteArray2Object(readInfo);
            break;
         }
      } 
      return data;
   }

   private byte[] read(Server server, Object cacheName, Object key) {
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
               log.warn("Something wrong with response:\n" + response.status());
            }
         } finally {
            response.release();
         }
      } catch (InterruptedException e1) {
         log.warn("Cannot perform a GET request");
      }
      return data;
   }
   
   private byte[] obj2byteArray(Object value) {
      byte[] data = null;
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos);) {
         oos.writeObject(value);
         oos.flush();
         oos.close();
         bos.close();
         data = bos.toByteArray();
      } catch (IOException e) {
         log.warn("Cannot convert to byte array");
      }
      return data;
   }
   
   private Object byteArray2Object(byte[] value) {
      Object data = null;
      try (ByteArrayInputStream bais = new ByteArrayInputStream(value);
         ObjectInputStream ois = new ObjectInputStream(bais);) {
         data = ois.readObject();
         ois.close();
         bais.close();
      } catch (IOException | ClassNotFoundException e) {
         log.warn("Cannot convert to Object");
      }
      return data;
   }

   private String getUri(Object... values) {
      StringBuilder bld = new StringBuilder(URI_BASIS);
      for (Object value : values) {
         if (bld.length() > 0) {
            bld.append("/");
         }
         bld.append(value);
      }
      return bld.toString();
   }

   private class HttpResponseHandler extends SimpleChannelInboundHandler<HttpResponse> {

      private FullHttpResponse response;

      private boolean retainResponse;

      public HttpResponseHandler() {
         this(false);
      }

      public HttpResponseHandler(boolean retainResponse) {
         this.retainResponse = retainResponse;
      }

      protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {
         if (retainResponse) {
            this.response = ((FullHttpResponse) msg).retain();
         }
         ctx.close();
      };

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         throw new Exception(cause);
      }

      public FullHttpResponse getResponse() {
         return response;
      }

   }

}
