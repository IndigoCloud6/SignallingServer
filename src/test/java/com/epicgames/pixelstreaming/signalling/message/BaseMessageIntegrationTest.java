// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify that the new BaseMessage structure works correctly
 * with actual message parsing scenarios used by connection classes.
 */
class BaseMessageIntegrationTest {

    private MessageHelper messageHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageHelper = new MessageHelper(objectMapper);
    }

    @Test
    void testStreamerIdentifyMessageHandling() throws Exception {
        // Test message that would be sent to identify a streamer
        String identifyJson = "{\"type\":\"identify\",\"streamerId\":\"myStreamer123\"}";
        
        BaseMessage message = messageHelper.parseMessage(identifyJson);
        
        assertNotNull(message);
        assertEquals("identify", message.getType());
        assertTrue(message.has("streamerId"));
        assertEquals("myStreamer123", message.get("streamerId"));
    }

    @Test
    void testPlayerIdentifyMessageHandling() throws Exception {
        // Test message that would be sent to identify a player
        String identifyJson = "{\"type\":\"identify\",\"playerId\":\"player456\"}";
        
        BaseMessage message = messageHelper.parseMessage(identifyJson);
        
        assertNotNull(message);
        assertEquals("identify", message.getType());
        assertTrue(message.has("playerId"));
        assertEquals("player456", message.get("playerId"));
    }

    @Test
    void testSFUIdentifyMessageHandling() throws Exception {
        // Test message that would be sent to identify an SFU
        String identifyJson = "{\"type\":\"identify\",\"sfuId\":\"sfu789\"}";
        
        BaseMessage message = messageHelper.parseMessage(identifyJson);
        
        assertNotNull(message);
        assertEquals("identify", message.getType());
        assertTrue(message.has("sfuId"));
        assertEquals("sfu789", message.get("sfuId"));
    }

    @Test
    void testUnrealIdentifyMessageHandling() throws Exception {
        // Test message that would be sent to identify an Unreal client
        String identifyJson = "{\"type\":\"identify\",\"unrealId\":\"unreal101\"}";
        
        BaseMessage message = messageHelper.parseMessage(identifyJson);
        
        assertNotNull(message);
        assertEquals("identify", message.getType());
        assertTrue(message.has("unrealId"));
        assertEquals("unreal101", message.get("unrealId"));
    }

    @Test
    void testBackwardCompatibilityWithDataField() throws Exception {
        // Test that old format with data field still works
        String oldFormatJson = "{\"type\":\"identify\",\"data\":{\"streamerId\":\"oldStreamer\"}}";
        
        BaseMessage message = messageHelper.parseMessage(oldFormatJson);
        
        assertNotNull(message);
        assertEquals("identify", message.getType());
        assertTrue(message.has("data"));
        
        // Can access via data field for backward compatibility
        assertNotNull(message.getData());
        assertTrue(message.getData().has("streamerId"));
        assertEquals("oldStreamer", message.getData().get("streamerId").asText());
    }

    @Test
    void testMessageWithIdFieldRouting() throws Exception {
        // Test message that includes an ID for routing (like WebRTC messages)
        String offerJson = "{\"type\":\"offer\",\"id\":\"player123\",\"sdp\":\"v=0...\"}";
        
        BaseMessage message = messageHelper.parseMessage(offerJson);
        
        assertNotNull(message);
        assertEquals("offer", message.getType());
        assertEquals("player123", message.getId());
        assertTrue(message.has("sdp"));
        assertEquals("v=0...", message.get("sdp"));
    }

    @Test
    void testComplexMessageStructure() throws Exception {
        // Test a complex message with nested structures
        String complexJson = "{\"type\":\"iceCandidate\",\"candidate\":{\"sdpMid\":\"0\",\"sdpMLineIndex\":0,\"candidate\":\"candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host\"},\"id\":\"player456\"}";
        
        BaseMessage message = messageHelper.parseMessage(complexJson);
        
        assertNotNull(message);
        assertEquals("iceCandidate", message.getType());
        assertEquals("player456", message.getId());
        assertTrue(message.has("candidate"));
        
        // Verify complex object structure is preserved
        Object candidate = message.get("candidate");
        assertNotNull(candidate);
    }

    @Test
    void testMessagePropertyManipulation() {
        // Test that we can create and manipulate messages programmatically
        BaseMessage message = new BaseMessage("playerConnected");
        
        // Set various properties
        message.put("playerId", "player123");
        message.put("dataChannel", true);
        message.put("sfu", false);
        message.put("sendOffer", true);
        
        // Verify properties
        assertEquals("playerConnected", message.getType());
        assertTrue(message.has("playerId"));
        assertTrue(message.has("dataChannel"));
        assertTrue(message.has("sfu"));
        assertTrue(message.has("sendOffer"));
        
        assertEquals("player123", message.get("playerId"));
        assertEquals(true, message.get("dataChannel"));
        assertEquals(false, message.get("sfu"));
        assertEquals(true, message.get("sendOffer"));
        
        // Test serialization
        String json = messageHelper.serializeMessage(message);
        assertNotNull(json);
        assertTrue(json.contains("playerConnected"));
        assertTrue(json.contains("player123"));
    }
}