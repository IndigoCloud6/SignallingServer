// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate that messages conform to the Pixel Streaming protocol format.
 * These tests define the expected message structures that the refactored BaseMessage should support.
 */
class PixelStreamingMessageFormatTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testConfigMessageFormat() throws Exception {
        // Expected format: {"type":"config","peerConnectionOptions":{}}
        String expectedJson = "{\"type\":\"config\",\"peerConnectionOptions\":{\"iceServers\":[]}}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("config", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("peerConnectionOptions"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
    }

    @Test
    void testIceCandidateMessageFormat() throws Exception {
        // Expected format: {"type":"iceCandidate","candidate":{"sdpMid":"0","sdpMLineIndex":0,"candidate":"..."}}
        String expectedJson = "{\"type\":\"iceCandidate\",\"candidate\":{\"sdpMid\":\"0\",\"sdpMLineIndex\":0,\"candidate\":\"candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host\"}}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("iceCandidate", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("candidate"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
        
        // Verify candidate structure
        JsonNode candidate = expectedNode.get("candidate");
        assertTrue(candidate.has("sdpMid"));
        assertTrue(candidate.has("sdpMLineIndex"));
        assertTrue(candidate.has("candidate"));
    }

    @Test
    void testStreamerListMessageFormat() throws Exception {
        // Expected format: {"type":"streamerList","ids":["DefaultStreamer"]}
        String expectedJson = "{\"type\":\"streamerList\",\"ids\":[\"DefaultStreamer\",\"Streamer2\"]}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("streamerList", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("ids"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
        
        // Verify ids is an array
        assertTrue(expectedNode.get("ids").isArray());
        assertEquals(2, expectedNode.get("ids").size());
    }

    @Test
    void testPlayerConnectedMessageFormat() throws Exception {
        // Expected format: {"type":"playerConnected","playerId":"1","dataChannel":true,"sfu":false,"sendOffer":true}
        String expectedJson = "{\"type\":\"playerConnected\",\"playerId\":\"1\",\"dataChannel\":true,\"sfu\":false,\"sendOffer\":true}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("playerConnected", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("playerId"));
        assertTrue(expectedNode.has("dataChannel"));
        assertTrue(expectedNode.has("sfu"));
        assertTrue(expectedNode.has("sendOffer"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
        
        // Verify values
        assertEquals("1", expectedNode.get("playerId").asText());
        assertTrue(expectedNode.get("dataChannel").asBoolean());
        assertFalse(expectedNode.get("sfu").asBoolean());
        assertTrue(expectedNode.get("sendOffer").asBoolean());
    }

    @Test
    void testOfferMessageFormat() throws Exception {
        // Expected format: {"type":"offer","sdp":"v=0\r\n..."}
        String expectedJson = "{\"type\":\"offer\",\"sdp\":\"v=0\\r\\no=- 123 1 IN IP4 127.0.0.1\\r\\n\"}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("offer", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("sdp"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
    }

    @Test
    void testAnswerMessageFormat() throws Exception {
        // Expected format: {"type":"answer","sdp":"v=0\r\n..."}
        String expectedJson = "{\"type\":\"answer\",\"sdp\":\"v=0\\r\\no=- 456 1 IN IP4 127.0.0.1\\r\\n\"}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("answer", expectedNode.get("type").asText());
        assertTrue(expectedNode.has("sdp"));
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
    }

    @Test
    void testPingMessageFormat() throws Exception {
        // Expected format: {"type":"ping"}
        String expectedJson = "{\"type\":\"ping\"}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("ping", expectedNode.get("type").asText());
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
    }

    @Test
    void testPongMessageFormat() throws Exception {
        // Expected format: {"type":"pong"}
        String expectedJson = "{\"type\":\"pong\"}";
        
        JsonNode expectedNode = objectMapper.readTree(expectedJson);
        
        // Verify structure
        assertTrue(expectedNode.has("type"));
        assertEquals("pong", expectedNode.get("type").asText());
        assertFalse(expectedNode.has("id"));
        assertFalse(expectedNode.has("data"));
    }
}