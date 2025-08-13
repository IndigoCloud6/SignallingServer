// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * WebSocket handshake handler that routes connections based on URL path.
 * Supports unified WebSocket endpoint with path-based routing.
 */
public class PathBasedWebSocketHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PathBasedWebSocketHandshakeHandler.class);
    
    // Attribute key to store the connection type for this channel
    public static final AttributeKey<String> CONNECTION_TYPE_KEY = AttributeKey.valueOf("connectionType");

    private final SignallingConfig config;
    private final MessageHelper messageHelper;
    private final ConnectionManager connectionManager;

    public PathBasedWebSocketHandshakeHandler(SignallingConfig config, 
                                            MessageHelper messageHelper, 
                                            ConnectionManager connectionManager) {
        this.config = config;
        this.messageHelper = messageHelper;
        this.connectionManager = connectionManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            
            // Check if this is a WebSocket upgrade request
            if (isWebSocketUpgrade(request)) {
                handleWebSocketUpgrade(ctx, request);
                return;
            }
        }
        
        // Pass through non-WebSocket requests
        super.channelRead(ctx, msg);
    }

    private boolean isWebSocketUpgrade(HttpRequest request) {
        HttpHeaders headers = request.headers();
        return "websocket".equalsIgnoreCase(headers.get("Upgrade")) &&
               "upgrade".equalsIgnoreCase(headers.get("Connection"));
    }

    private void handleWebSocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = request.uri();
            logger.debug("WebSocket upgrade request for URI: {}", uri);
            
            // Parse the path from the URI
            String path = extractPath(uri);
            String connectionType = determineConnectionType(path);
            
            if (connectionType == null) {
                logger.warn("Unknown WebSocket path: {}", path);
                ctx.close();
                return;
            }
            
            // Store the connection type in channel attributes
            ctx.channel().attr(CONNECTION_TYPE_KEY).set(connectionType);
            
            logger.info("WebSocket connection type determined: {} for path: {}", connectionType, path);
            
            // Create and add the appropriate handler to the pipeline
            AbstractWebSocketHandler handler = createHandler(connectionType);
            if (handler != null) {
                ctx.pipeline().addLast("websocket-handler", handler);
                logger.debug("Added {} handler to pipeline", connectionType);
            } else {
                logger.error("Failed to create handler for connection type: {}", connectionType);
                ctx.close();
                return;
            }
            
        } catch (Exception e) {
            logger.error("Error handling WebSocket upgrade", e);
            ctx.close();
            return;
        }
        
        // Pass the request to the next handler in the pipeline
        ctx.fireChannelRead(request);
    }

    private String extractPath(String uri) {
        try {
            URI parsedUri = new URI(uri);
            String path = parsedUri.getPath();
            // Handle empty or null path by returning "/"
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (URISyntaxException e) {
            logger.warn("Failed to parse URI: {}", uri, e);
            return "/";
        }
    }

    private String determineConnectionType(String path) {
        // Match the configured paths to connection types
        if (config.getWebsocket().getPlayerPath().equals(path)) {
            return "PLAYER";
        } else if (config.getWebsocket().getStreamerPath().equals(path)) {
            return "STREAMER";
        } else if (config.getWebsocket().getSfuPath().equals(path)) {
            return "SFU";
        } else if (config.getWebsocket().getUnrealPath().equals(path)) {
            return "UNREAL";
        }
        
        // Default fallback - check for legacy root path behavior
        if ("/".equals(path)) {
            // For backward compatibility, default to streamer for root path
            return "STREAMER";
        }
        
        return null;
    }

    private AbstractWebSocketHandler createHandler(String connectionType) {
        switch (connectionType) {
            case "PLAYER":
                return new PlayerWebSocketHandler(messageHelper, connectionManager);
            case "STREAMER":
                return new StreamerWebSocketHandler(messageHelper, connectionManager, config);
            case "SFU":
                return new SFUWebSocketHandler(messageHelper, connectionManager);
            case "UNREAL":
                return new UnrealWebSocketHandler(messageHelper, connectionManager);
            default:
                logger.error("Unknown connection type: {}", connectionType);
                return null;
        }
    }
}