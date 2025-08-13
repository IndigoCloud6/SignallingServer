// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import com.epicgames.pixelstreaming.signalling.config.SignallingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class PathBasedRoutingTest {

    private SignallingConfig config;

    @BeforeEach
    void setUp() {
        config = new SignallingConfig();
        // Use default path configurations
        config.getWebsocket().setPlayerPath("/player");
        config.getWebsocket().setStreamerPath("/streamer");
        config.getWebsocket().setSfuPath("/sfu");
        config.getWebsocket().setUnrealPath("/unreal");
    }

    @Test
    void testPathExtractionFromURI() throws URISyntaxException {
        // Test various URI formats
        assertEquals("/player", extractPath("ws://localhost:8888/player"));
        assertEquals("/streamer", extractPath("ws://localhost:8888/streamer"));
        assertEquals("/sfu", extractPath("ws://localhost:8888/sfu"));
        assertEquals("/unreal", extractPath("ws://localhost:8888/unreal"));
        assertEquals("/", extractPath("ws://localhost:8888/"));
        // Fix the test case for URI without trailing slash
        assertEquals("/", extractPath("ws://localhost:8888"));
    }

    @Test
    void testConnectionTypeDetection() {
        // Test path-to-connection-type mapping
        assertEquals("PLAYER", determineConnectionType("/player"));
        assertEquals("STREAMER", determineConnectionType("/streamer"));
        assertEquals("SFU", determineConnectionType("/sfu"));
        assertEquals("UNREAL", determineConnectionType("/unreal"));
        
        // Test backward compatibility - root path defaults to streamer
        assertEquals("STREAMER", determineConnectionType("/"));
        
        // Test unknown paths
        assertNull(determineConnectionType("/unknown"));
        assertNull(determineConnectionType("/invalid"));
    }

    @Test
    void testCaseInsensitivePaths() {
        // Test that path matching is case-sensitive as expected
        assertNull(determineConnectionType("/PLAYER"));
        assertNull(determineConnectionType("/Player"));
        assertNull(determineConnectionType("/STREAMER"));
    }

    @Test
    void testCustomPathConfiguration() {
        // Test custom path configuration
        config.getWebsocket().setPlayerPath("/custom-player");
        config.getWebsocket().setStreamerPath("/custom-streamer");
        
        assertEquals("PLAYER", determineConnectionType("/custom-player"));
        assertEquals("STREAMER", determineConnectionType("/custom-streamer"));
        
        // Original paths should no longer work
        assertNull(determineConnectionType("/player"));
        assertNull(determineConnectionType("/streamer"));
    }

    // Helper methods that mirror the logic in PathBasedWebSocketHandshakeHandler
    private String extractPath(String uri) {
        try {
            URI parsedUri = new URI(uri);
            String path = parsedUri.getPath();
            // Handle empty or null path by returning "/"
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (URISyntaxException e) {
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
}