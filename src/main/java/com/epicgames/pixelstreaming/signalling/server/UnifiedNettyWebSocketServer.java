// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.handler.PathBasedWebSocketHandshakeHandler;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Unified Netty-based WebSocket server that supports path-based routing.
 * Handles all connection types (player, streamer, SFU, unreal) on a single port.
 */
public class UnifiedNettyWebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedNettyWebSocketServer.class);

    private final String name;
    private final String host;
    private final int port;
    private final SignallingConfig config;
    private final MessageHelper messageHelper;
    private final ConnectionManager connectionManager;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public UnifiedNettyWebSocketServer(String name, String host, int port, 
                                     SignallingConfig config,
                                     MessageHelper messageHelper,
                                     ConnectionManager connectionManager) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.config = config;
        this.messageHelper = messageHelper;
        this.connectionManager = connectionManager;
    }

    /**
     * Start the unified WebSocket server.
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                startServer();
            } catch (Exception e) {
                throw new RuntimeException("Failed to start " + name + " server", e);
            }
        });
    }

    /**
     * Stop the unified WebSocket server.
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            try {
                stopServer();
            } catch (Exception e) {
                throw new RuntimeException("Failed to stop " + name + " server", e);
            }
        });
    }

    private void startServer() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new UnifiedWebSocketServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            serverChannel = future.channel();
            
            logger.info("{} unified server started on {}:{} with path-based routing", name, host, port);
            
            // Wait for the server socket to close
            serverChannel.closeFuture().sync();
            
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        logger.info("Stopping {} unified server", name);
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(2, 5, TimeUnit.SECONDS);
        }
        
        logger.info("{} unified server stopped", name);
    }

    /**
     * Channel initializer for unified WebSocket connections.
     */
    private class UnifiedWebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
        
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            
            // HTTP codec
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(65536));
            
            // Path-based routing handler (must be before WebSocket protocol handler)
            pipeline.addLast(new PathBasedWebSocketHandshakeHandler(config, messageHelper, connectionManager));
            
            // WebSocket protocol handler - supports multiple paths
            pipeline.addLast(new WebSocketServerProtocolHandler("/", null, true, 
                config.getWebsocket().getMaxFrameSize()));
            
            // Idle state handling for connection timeout
            pipeline.addLast(new IdleStateHandler(
                config.getWebsocket().getConnectionTimeoutSeconds(),
                config.getWebsocket().getPingIntervalSeconds(),
                0,
                TimeUnit.SECONDS
            ));
            
            // Custom idle state handler
            pipeline.addLast(new IdleStateChannelHandler());
            
            // Note: Application handlers are added dynamically by PathBasedWebSocketHandshakeHandler
        }
    }

    /**
     * Handler for idle state events (ping/pong and connection timeout).
     */
    private static class IdleStateChannelHandler extends ChannelInboundHandlerAdapter {
        
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                
                switch (event.state()) {
                    case READER_IDLE:
                        logger.debug("Connection {} reader idle, closing", ctx.channel().remoteAddress());
                        ctx.close();
                        break;
                        
                    case WRITER_IDLE:
                        logger.debug("Sending ping to connection {}", ctx.channel().remoteAddress());
                        ctx.writeAndFlush(new PingWebSocketFrame());
                        break;
                        
                    case ALL_IDLE:
                        logger.debug("Connection {} all idle, closing", ctx.channel().remoteAddress());
                        ctx.close();
                        break;
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // Getters
    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isRunning() { 
        return serverChannel != null && serverChannel.isActive(); 
    }
}