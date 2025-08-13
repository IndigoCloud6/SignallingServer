// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate that MessageHelper creates messages that serialize to the correct
 * Pixel Streaming protocol format.
 */
class MessageHelperPixelStreamingFormatTest {

    private MessageHelper messageHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageHelper = new MessageHelper(objectMapper);
    }

    @Test
    void testCreateConfigMessageFormat() throws Exception {
        JsonNode peerOptions = objectMapper.createObjectNode().put("iceServers", objectMapper.createArrayNode());
        
        BaseMessage message = messageHelper.createConfigMessage(peerOptions);
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("config", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("peerConnectionOptions"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
    }

    @Test
    void testCreateIceCandidateMessageFormat() throws Exception {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("sdpMid", "0");
        candidate.put("sdpMLineIndex", 0);
        candidate.put("candidate", "candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host");
        
        BaseMessage message = messageHelper.createIceCandidateMessage(candidate);
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("iceCandidate", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("candidate"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        // Verify candidate structure
        JsonNode candidateNode = jsonNode.get("candidate");
        assertEquals("0", candidateNode.get("sdpMid").asText());
        assertEquals(0, candidateNode.get("sdpMLineIndex").asInt());
        assertEquals("candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host", candidateNode.get("candidate").asText());
    }

    @Test
    void testCreateStreamerListMessageFormat() throws Exception {
        BaseMessage message = messageHelper.createStreamerListMessage(Arrays.asList("DefaultStreamer", "Streamer2"));
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("streamerList", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("ids"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        // Verify ids array
        JsonNode idsNode = jsonNode.get("ids");
        assertTrue(idsNode.isArray());
        assertEquals(2, idsNode.size());
        assertEquals("DefaultStreamer", idsNode.get(0).asText());
        assertEquals("Streamer2", idsNode.get(1).asText());
    }

    @Test
    void testCreatePlayerConnectedMessageFormat() throws Exception {
        BaseMessage message = messageHelper.createPlayerConnectedMessage("player123", true, false, true);
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("playerConnected", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("playerId"));
        assertTrue(jsonNode.has("dataChannel"));
        assertTrue(jsonNode.has("sfu"));
        assertTrue(jsonNode.has("sendOffer"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        // Verify values
        assertEquals("player123", jsonNode.get("playerId").asText());
        assertTrue(jsonNode.get("dataChannel").asBoolean());
        assertFalse(jsonNode.get("sfu").asBoolean());
        assertTrue(jsonNode.get("sendOffer").asBoolean());
    }

    @Test
    void testCreateOfferMessageFormat() throws Exception {
        String sdp = "v=0\\r\\no=- 123 1 IN IP4 127.0.0.1\\r\\n";
        
        BaseMessage message = messageHelper.createOfferMessage(sdp);
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("offer", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("sdp"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        assertEquals(sdp, jsonNode.get("sdp").asText());
    }

    @Test
    void testCreateAnswerMessageFormat() throws Exception {
        String sdp = "v=0\\r\\no=- 456 1 IN IP4 127.0.0.1\\r\\n";
        
        BaseMessage message = messageHelper.createAnswerMessage(sdp);
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("answer", jsonNode.get("type").asText());
        assertTrue(jsonNode.has("sdp"));
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        assertEquals(sdp, jsonNode.get("sdp").asText());
    }

    @Test
    void testCreatePingMessageFormat() throws Exception {
        BaseMessage message = messageHelper.createPingMessage();
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("ping", jsonNode.get("type").asText());
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        // Should only have type field
        assertEquals(1, jsonNode.size());
    }

    @Test
    void testCreatePongMessageFormat() throws Exception {
        BaseMessage message = messageHelper.createPongMessage();
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        
        // Parse the JSON to verify structure
        JsonNode jsonNode = objectMapper.readTree(json);
        assertEquals("pong", jsonNode.get("type").asText());
        assertFalse(jsonNode.has("id"));
        assertFalse(jsonNode.has("data"));
        
        // Should only have type field
        assertEquals(1, jsonNode.size());
    }

    @Test
    void testBackwardCompatibilityParsing() throws Exception {
        // Test that old format messages can still be parsed
        String oldFormatJson = "{\"type\":\"config\",\"id\":\"test-id\",\"data\":{\"test\":\"value\"}}";
        
        BaseMessage message = messageHelper.parseMessage(oldFormatJson);
        
        assertNotNull(message);
        assertEquals("config", message.getType());
        assertEquals("test-id", message.getId());
        assertNotNull(message.getData());
        assertEquals("value", message.getData().get("test").asText());
    }
}