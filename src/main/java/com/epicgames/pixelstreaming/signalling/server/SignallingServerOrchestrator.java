// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.server;

import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import com.epicgames.pixelstreaming.signalling.handler.PlayerWebSocketHandler;
import com.epicgames.pixelstreaming.signalling.handler.StreamerWebSocketHandler;
import com.epicgames.pixelstreaming.signalling.handler.SFUWebSocketHandler;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for orchestrating all signalling servers.
 * Manages the lifecycle of player, streamer, and SFU WebSocket servers.
 */
@Service
public class SignallingServerOrchestrator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SignallingServerOrchestrator.class);

    private final SignallingConfig config;
    private final ConnectionManager connectionManager;
    private final MessageHelper messageHelper;
    
    private final List<NettyWebSocketServer> servers = new ArrayList<>();
    private UnifiedNettyWebSocketServer unifiedServer;

    public SignallingServerOrchestrator(SignallingConfig config, 
                                      ConnectionManager connectionManager,
                                      MessageHelper messageHelper) {
        this.config = config;
        this.connectionManager = connectionManager;
        this.messageHelper = messageHelper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Starting Pixel Streaming Signalling Servers");
        startAllServers();
    }

    /**
     * Start all configured servers.
     */
    private void startAllServers() {
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            if (config.getServer().isEnableUnifiedPort()) {
                // Start unified server on unified port
                logger.info("Starting unified WebSocket server on port {}", config.getServer().getUnifiedPort());
                unifiedServer = new UnifiedNettyWebSocketServer(
                    "Unified",
                    config.getServer().getHost(),
                    config.getServer().getUnifiedPort(),
                    config,
                    messageHelper,
                    connectionManager
                );
                futures.add(unifiedServer.start());
            } else {
                // Start separate servers for backward compatibility
                logger.info("Starting separate WebSocket servers (legacy mode)");
                startLegacyServers(futures);
            }

            // Wait for all servers to start
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("All signalling servers started successfully"))
                .exceptionally(throwable -> {
                    logger.error("Failed to start signalling servers", throwable);
                    System.exit(1);
                    return null;
                });

        } catch (Exception e) {
            logger.error("Error starting signalling servers", e);
            throw new RuntimeException("Failed to start signalling servers", e);
        }
    }

    /**
     * Start legacy separate servers for backward compatibility.
     */
    private void startLegacyServers(List<CompletableFuture<Void>> futures) {
        // Start player server
        NettyWebSocketServer playerServer = new NettyWebSocketServer(
            "Player",
            config.getServer().getHost(),
            config.getServer().getPlayerPort(),
            () -> new PlayerWebSocketHandler(messageHelper, connectionManager),
            config
        );
        servers.add(playerServer);

        // Start streamer server
        NettyWebSocketServer streamerServer = new NettyWebSocketServer(
            "Streamer",
            config.getServer().getHost(),
            config.getServer().getStreamerPort(),
            () -> new StreamerWebSocketHandler(messageHelper, connectionManager, config),
            config
        );
        servers.add(streamerServer);

        // Start SFU server if enabled
        if (config.getServer().isEnableSfu()) {
            NettyWebSocketServer sfuServer = new NettyWebSocketServer(
                "SFU",
                config.getServer().getHost(),
                config.getServer().getSfuPort(),
                () -> new SFUWebSocketHandler(messageHelper, connectionManager),
                config
            );
            servers.add(sfuServer);
        }

        // Add futures for all legacy servers
        futures.addAll(servers.stream()
            .map(NettyWebSocketServer::start)
            .toList());
    }

    /**
     * Stop all servers gracefully.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down signalling servers");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Add legacy servers
        futures.addAll(servers.stream()
            .map(NettyWebSocketServer::stop)
            .toList());
        
        // Add unified server if it exists
        if (unifiedServer != null) {
            futures.add(unifiedServer.stop());
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(); // Wait for all servers to stop
            
            logger.info("All signalling servers stopped");
            
        } catch (Exception e) {
            logger.error("Error stopping signalling servers", e);
        }
        
        // Shutdown connection manager
        connectionManager.shutdown();
    }

    /**
     * Get server status information.
     */
    public List<ServerStatus> getServerStatus() {
        List<ServerStatus> statusList = new ArrayList<>();
        
        // Add legacy servers status
        statusList.addAll(servers.stream()
            .map(server -> new ServerStatus(
                server.getName(),
                server.getHost(),
                server.getPort(),
                server.isRunning()
            ))
            .toList());
        
        // Add unified server status if it exists
        if (unifiedServer != null) {
            statusList.add(new ServerStatus(
                unifiedServer.getName(),
                unifiedServer.getHost(),
                unifiedServer.getPort(),
                unifiedServer.isRunning()
            ));
        }
        
        return statusList;
    }

    /**
     * Server status data class.
     */
    public static class ServerStatus {
        private final String name;
        private final String host;
        private final int port;
        private final boolean running;

        public ServerStatus(String name, String host, int port, boolean running) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.running = running;
        }

        // Getters
        public String getName() { return name; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isRunning() { return running; }
    }
}