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
 * Tests to verify that messages serialize to exactly the expected JSON format
 * that matches the Pixel Streaming protocol specification.
 */
class ExactPixelStreamingFormatTest {

    private MessageHelper messageHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageHelper = new MessageHelper(objectMapper);
    }

    @Test
    void testConfigMessageExactFormat() throws Exception {
        JsonNode peerOptions = objectMapper.createObjectNode()
            .put("iceServers", objectMapper.createArrayNode());
        
        BaseMessage message = messageHelper.createConfigMessage(peerOptions);
        String actualJson = messageHelper.serializeMessage(message);
        
        // Parse and compare structure exactly
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match: {"type":"config","peerConnectionOptions":{...}}
        assertEquals("config", actual.get("type").asText());
        assertTrue(actual.has("peerConnectionOptions"));
        assertEquals(2, actual.size()); // Only type and peerConnectionOptions
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
    }

    @Test
    void testIceCandidateMessageExactFormat() throws Exception {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("sdpMid", "0");
        candidate.put("sdpMLineIndex", 0);
        candidate.put("candidate", "candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host");
        
        BaseMessage message = messageHelper.createIceCandidateMessage(candidate);
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match: {"type":"iceCandidate","candidate":{...}}
        assertEquals("iceCandidate", actual.get("type").asText());
        assertTrue(actual.has("candidate"));
        assertEquals(2, actual.size()); // Only type and candidate
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
        
        JsonNode candidateNode = actual.get("candidate");
        assertEquals("0", candidateNode.get("sdpMid").asText());
        assertEquals(0, candidateNode.get("sdpMLineIndex").asInt());
        assertEquals("candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host", candidateNode.get("candidate").asText());
    }

    @Test
    void testStreamerListMessageExactFormat() throws Exception {
        BaseMessage message = messageHelper.createStreamerListMessage(Arrays.asList("DefaultStreamer"));
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match: {"type":"streamerList","ids":["DefaultStreamer"]}
        assertEquals("streamerList", actual.get("type").asText());
        assertTrue(actual.has("ids"));
        assertEquals(2, actual.size()); // Only type and ids
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
        
        JsonNode idsNode = actual.get("ids");
        assertTrue(idsNode.isArray());
        assertEquals(1, idsNode.size());
        assertEquals("DefaultStreamer", idsNode.get(0).asText());
    }

    @Test
    void testPlayerConnectedMessageExactFormat() throws Exception {
        BaseMessage message = messageHelper.createPlayerConnectedMessage("1", true, false, true);
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match: {"type":"playerConnected","playerId":"1","dataChannel":true,"sfu":false,"sendOffer":true}
        assertEquals("playerConnected", actual.get("type").asText());
        assertEquals("1", actual.get("playerId").asText());
        assertTrue(actual.get("dataChannel").asBoolean());
        assertFalse(actual.get("sfu").asBoolean());
        assertTrue(actual.get("sendOffer").asBoolean());
        assertEquals(5, actual.size()); // type, playerId, dataChannel, sfu, sendOffer
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
    }

    @Test
    void testOfferMessageExactFormat() throws Exception {
        String sdp = "v=0\r\no=- 123 1 IN IP4 127.0.0.1\r\n";
        
        BaseMessage message = messageHelper.createOfferMessage(sdp);
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match: {"type":"offer","sdp":"v=0\r\n..."}
        assertEquals("offer", actual.get("type").asText());
        assertEquals(sdp, actual.get("sdp").asText());
        assertEquals(2, actual.size()); // Only type and sdp
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
    }

    @Test
    void testPingMessageExactFormat() throws Exception {
        BaseMessage message = messageHelper.createPingMessage();
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match exactly: {"type":"ping"}
        assertEquals("ping", actual.get("type").asText());
        assertEquals(1, actual.size()); // Only type
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
        
        // Verify exact JSON string format
        assertEquals("{\"type\":\"ping\"}", actualJson);
    }

    @Test
    void testPongMessageExactFormat() throws Exception {
        BaseMessage message = messageHelper.createPongMessage();
        String actualJson = messageHelper.serializeMessage(message);
        
        JsonNode actual = objectMapper.readTree(actualJson);
        
        // Should match exactly: {"type":"pong"}
        assertEquals("pong", actual.get("type").asText());
        assertEquals(1, actual.size()); // Only type
        assertFalse(actual.has("id"));
        assertFalse(actual.has("data"));
        
        // Verify exact JSON string format
        assertEquals("{\"type\":\"pong\"}", actualJson);
    }

    @Test
    void testCompareWithExpectedPixelStreamingFormat() throws Exception {
        // Test that our format exactly matches the expected Pixel Streaming protocol
        String expectedConfigJson = "{\"type\":\"config\",\"peerConnectionOptions\":{}}";
        String expectedIceCandidateJson = "{\"type\":\"iceCandidate\",\"candidate\":{\"sdpMid\":\"0\",\"sdpMLineIndex\":0,\"candidate\":\"test\"}}";
        String expectedStreamerListJson = "{\"type\":\"streamerList\",\"ids\":[\"DefaultStreamer\"]}";
        String expectedPlayerConnectedJson = "{\"type\":\"playerConnected\",\"playerId\":\"1\",\"dataChannel\":true,\"sfu\":false,\"sendOffer\":true}";
        
        // Create messages using our helper
        BaseMessage configMessage = messageHelper.createConfigMessage(objectMapper.createObjectNode());
        
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("sdpMid", "0");
        candidate.put("sdpMLineIndex", 0);
        candidate.put("candidate", "test");
        BaseMessage iceCandidateMessage = messageHelper.createIceCandidateMessage(candidate);
        
        BaseMessage streamerListMessage = messageHelper.createStreamerListMessage(Arrays.asList("DefaultStreamer"));
        BaseMessage playerConnectedMessage = messageHelper.createPlayerConnectedMessage("1", true, false, true);
        
        // Parse expected JSON
        JsonNode expectedConfig = objectMapper.readTree(expectedConfigJson);
        JsonNode expectedIceCandidate = objectMapper.readTree(expectedIceCandidateJson);
        JsonNode expectedStreamerList = objectMapper.readTree(expectedStreamerListJson);
        JsonNode expectedPlayerConnected = objectMapper.readTree(expectedPlayerConnectedJson);
        
        // Parse our generated JSON
        JsonNode actualConfig = objectMapper.readTree(messageHelper.serializeMessage(configMessage));
        JsonNode actualIceCandidate = objectMapper.readTree(messageHelper.serializeMessage(iceCandidateMessage));
        JsonNode actualStreamerList = objectMapper.readTree(messageHelper.serializeMessage(streamerListMessage));
        JsonNode actualPlayerConnected = objectMapper.readTree(messageHelper.serializeMessage(playerConnectedMessage));
        
        // Compare structures (not exact string match due to potential field ordering)
        assertTrue(expectedConfig.equals(actualConfig), "Config message structure should match");
        assertTrue(expectedIceCandidate.equals(actualIceCandidate), "ICE candidate message structure should match");
        assertTrue(expectedStreamerList.equals(actualStreamerList), "Streamer list message structure should match");
        assertTrue(expectedPlayerConnected.equals(actualPlayerConnected), "Player connected message structure should match");
    }
}