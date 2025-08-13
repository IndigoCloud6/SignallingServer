// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageHelperTest {

    private MessageHelper messageHelper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        messageHelper = new MessageHelper(objectMapper);
    }

    @Test
    void testParseValidMessage() {
        String json = "{\"type\":\"config\",\"id\":\"test-id\",\"data\":{\"test\":\"value\"}}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNotNull(message);
        assertEquals("config", message.getType());
        assertEquals("test-id", message.getId());
        assertNotNull(message.getData());
    }

    @Test
    void testParseMessageWithoutId() {
        String json = "{\"type\":\"ping\"}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNotNull(message);
        assertEquals("ping", message.getType());
        assertNull(message.getId());
        assertNull(message.getData());
    }

    @Test
    void testParseInvalidMessage() {
        String json = "{\"invalid\":\"json\"}";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNull(message);
    }

    @Test
    void testParseMalformedJson() {
        String json = "{invalid json";
        
        BaseMessage message = messageHelper.parseMessage(json);
        
        assertNull(message);
    }

    @Test
    void testSerializeMessage() throws Exception {
        BaseMessage message = new BaseMessage("test-type");
        message.setId("test-id");
        message.put("testData", "test-value");
        
        String json = messageHelper.serializeMessage(message);
        
        assertNotNull(json);
        assertTrue(json.contains("test-type"));
        assertTrue(json.contains("test-id"));
        assertTrue(json.contains("test-value"));
    }

    @Test
    void testCreateConfigMessage() throws Exception {
        JsonNode peerOptions = objectMapper.createObjectNode();
        
        BaseMessage message = messageHelper.createConfigMessage(peerOptions);
        
        assertNotNull(message);
        assertEquals(MessageTypes.CONFIG, message.getType());
        assertTrue(message.has("peerConnectionOptions"));
    }

    @Test
    void testCreatePlayerCountMessage() {
        BaseMessage message = messageHelper.createPlayerCountMessage(5);
        
        assertNotNull(message);
        assertEquals(MessageTypes.PLAYER_COUNT, message.getType());
        assertTrue(message.has("count"));
        assertEquals(5, message.get("count"));
    }

    @Test
    void testCreateErrorMessage() {
        BaseMessage message = messageHelper.createErrorMessage("Test error");
        
        assertNotNull(message);
        assertEquals(MessageTypes.ERROR, message.getType());
        assertTrue(message.has("message"));
        assertEquals("Test error", message.get("message"));
    }

    @Test
    void testCreatePingMessage() {
        BaseMessage message = messageHelper.createPingMessage();
        
        assertNotNull(message);
        assertEquals(MessageTypes.PING, message.getType());
    }

    @Test
    void testCreatePongMessage() {
        BaseMessage message = messageHelper.createPongMessage();
        
        assertNotNull(message);
        assertEquals(MessageTypes.PONG, message.getType());
    }

    @Test
    void testCreateIceCandidateMessage() {
        Map<String, Object> candidate = new HashMap<>();
        candidate.put("sdpMid", "0");
        candidate.put("sdpMLineIndex", 0);
        candidate.put("candidate", "candidate:1 1 UDP 2130706431 192.168.1.1 50000 typ host");
        
        BaseMessage message = messageHelper.createIceCandidateMessage(candidate);
        
        assertNotNull(message);
        assertEquals(MessageTypes.ICE_CANDIDATE, message.getType());
        assertTrue(message.has("candidate"));
        assertEquals(candidate, message.get("candidate"));
    }

    @Test
    void testCreateStreamerListMessage() {
        java.util.List<String> streamerIds = java.util.Arrays.asList("DefaultStreamer", "Streamer2");
        
        BaseMessage message = messageHelper.createStreamerListMessage(streamerIds);
        
        assertNotNull(message);
        assertEquals(MessageTypes.STREAMER_LIST, message.getType());
        assertTrue(message.has("ids"));
        assertEquals(streamerIds, message.get("ids"));
    }

    @Test
    void testCreatePlayerConnectedMessage() {
        BaseMessage message = messageHelper.createPlayerConnectedMessage("player123", true, false, true);
        
        assertNotNull(message);
        assertEquals(MessageTypes.PLAYER_CONNECTED, message.getType());
        assertTrue(message.has("playerId"));
        assertTrue(message.has("dataChannel"));
        assertTrue(message.has("sfu"));
        assertTrue(message.has("sendOffer"));
        assertEquals("player123", message.get("playerId"));
        assertEquals(true, message.get("dataChannel"));
        assertEquals(false, message.get("sfu"));
        assertEquals(true, message.get("sendOffer"));
    }

    @Test
    void testCreateOfferMessage() {
        String sdp = "v=0\\r\\no=- 123 1 IN IP4 127.0.0.1\\r\\n";
        
        BaseMessage message = messageHelper.createOfferMessage(sdp);
        
        assertNotNull(message);
        assertEquals(MessageTypes.OFFER, message.getType());
        assertTrue(message.has("sdp"));
        assertEquals(sdp, message.get("sdp"));
    }

    @Test
    void testCreateAnswerMessage() {
        String sdp = "v=0\\r\\no=- 456 1 IN IP4 127.0.0.1\\r\\n";
        
        BaseMessage message = messageHelper.createAnswerMessage(sdp);
        
        assertNotNull(message);
        assertEquals(MessageTypes.ANSWER, message.getType());
        assertTrue(message.has("sdp"));
        assertEquals(sdp, message.get("sdp"));
    }


}