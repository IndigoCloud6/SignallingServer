// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "signalling.server.enable-unified-port=true",
    "signalling.server.unified-port=8887",
    "signalling.websocket.player-path=/test-player",
    "signalling.websocket.streamer-path=/test-streamer",
    "signalling.websocket.sfu-path=/test-sfu",
    "signalling.websocket.unreal-path=/test-unreal"
})
class SignallingConfigIntegrationTest {

    @Autowired
    private SignallingConfig signallingConfig;

    @Test
    void testUnifiedPortConfiguration() {
        // Test unified port configuration
        assertTrue(signallingConfig.getServer().isEnableUnifiedPort());
        assertEquals(8887, signallingConfig.getServer().getUnifiedPort());
        
        // Test that legacy ports are still available for backward compatibility
        assertEquals(8888, signallingConfig.getServer().getStreamerPort());
        assertEquals(8889, signallingConfig.getServer().getPlayerPort());
        assertEquals(8890, signallingConfig.getServer().getSfuPort());
    }

    @Test
    void testWebSocketPathConfiguration() {
        // Test custom path configuration
        assertEquals("/test-player", signallingConfig.getWebsocket().getPlayerPath());
        assertEquals("/test-streamer", signallingConfig.getWebsocket().getStreamerPath());
        assertEquals("/test-sfu", signallingConfig.getWebsocket().getSfuPath());
        assertEquals("/test-unreal", signallingConfig.getWebsocket().getUnrealPath());
    }

    @Test
    void testOtherConfigurationValues() {
        // Test that other configuration values are properly loaded
        assertEquals("0.0.0.0", signallingConfig.getServer().getHost());
        assertEquals(100, signallingConfig.getServer().getMaxSubscribers());
        assertTrue(signallingConfig.getServer().isEnableSfu());
        
        // Test WebSocket configuration
        assertEquals(65536, signallingConfig.getWebsocket().getMaxFrameSize());
        assertEquals(30, signallingConfig.getWebsocket().getPingIntervalSeconds());
        assertEquals(60, signallingConfig.getWebsocket().getConnectionTimeoutSeconds());
    }
}