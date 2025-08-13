// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class for handling message serialization and deserialization.
 * Provides methods to convert between JSON strings and message objects.
 */
@Component
public class MessageHelper {

    private static final Logger logger = LoggerFactory.getLogger(MessageHelper.class);
    private final ObjectMapper objectMapper;

    public MessageHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse a JSON string into a BaseMessage object.
     *
     * @param json The JSON string to parse
     * @return The parsed BaseMessage, or null if parsing fails
     */
    public BaseMessage parseMessage(String json) {
        try {
            BaseMessage message = objectMapper.readValue(json, BaseMessage.class);
            
            // Validate that the message has a type field
            if (message.getType() == null) {
                logger.warn("Message missing 'type' field: {}", json);
                return null;
            }
            
            return message;
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse message JSON: {}", json, e);
            return null;
        }
    }

    /**
     * Serialize a BaseMessage to JSON string.
     *
     * @param message The message to serialize
     * @return The JSON string, or null if serialization fails
     */
    public String serializeMessage(BaseMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message: {}", message, e);
            return null;
        }
    }

    /**
     * Create a config message with peer options.
     *
     * @param peerOptions The peer configuration options
     * @return The config message
     */
    public BaseMessage createConfigMessage(JsonNode peerOptions) {
        BaseMessage message = new BaseMessage(MessageTypes.CONFIG);
        if (peerOptions != null) {
            message.put("peerConnectionOptions", peerOptions);
        } else {
            // Create default empty peer connection options
            try {
                JsonNode defaultOptions = objectMapper.createObjectNode();
                message.put("peerConnectionOptions", defaultOptions);
            } catch (Exception e) {
                logger.error("Failed to create default peer connection options", e);
            }
        }
        return message;
    }

    /**
     * Create a player count message.
     *
     * @param count The current player count
     * @return The player count message
     */
    public BaseMessage createPlayerCountMessage(int count) {
        BaseMessage message = new BaseMessage(MessageTypes.PLAYER_COUNT);
        message.put("count", count);
        return message;
    }

    /**
     * Create an error message.
     *
     * @param errorMessage The error message text
     * @return The error message
     */
    public BaseMessage createErrorMessage(String errorMessage) {
        BaseMessage message = new BaseMessage(MessageTypes.ERROR);
        message.put("message", errorMessage);
        return message;
    }

    /**
     * Create a ping message.
     *
     * @return The ping message
     */
    public BaseMessage createPingMessage() {
        return new BaseMessage(MessageTypes.PING);
    }

    /**
     * Create a pong message.
     *
     * @return The pong message
     */
    public BaseMessage createPongMessage() {
        return new BaseMessage(MessageTypes.PONG);
    }

    /**
     * Create an ICE candidate message.
     *
     * @param candidate The ICE candidate object
     * @return The ICE candidate message
     */
    public BaseMessage createIceCandidateMessage(Object candidate) {
        BaseMessage message = new BaseMessage(MessageTypes.ICE_CANDIDATE);
        message.put("candidate", candidate);
        return message;
    }

    /**
     * Create a streamer list message.
     *
     * @param streamerIds List of streamer IDs
     * @return The streamer list message
     */
    public BaseMessage createStreamerListMessage(java.util.List<String> streamerIds) {
        BaseMessage message = new BaseMessage(MessageTypes.STREAMER_LIST);
        message.put("ids", streamerIds);
        return message;
    }

    /**
     * Create a player connected message.
     *
     * @param playerId The player ID
     * @param dataChannel Whether data channel is enabled
     * @param sfu Whether SFU is enabled
     * @param sendOffer Whether to send offer
     * @return The player connected message
     */
    public BaseMessage createPlayerConnectedMessage(String playerId, boolean dataChannel, boolean sfu, boolean sendOffer) {
        BaseMessage message = new BaseMessage(MessageTypes.PLAYER_CONNECTED);
        message.put("playerId", playerId);
        message.put("dataChannel", dataChannel);
        message.put("sfu", sfu);
        message.put("sendOffer", sendOffer);
        return message;
    }

    /**
     * Create an offer message.
     *
     * @param sdp The SDP offer
     * @return The offer message
     */
    public BaseMessage createOfferMessage(String sdp) {
        BaseMessage message = new BaseMessage(MessageTypes.OFFER);
        message.put("sdp", sdp);
        return message;
    }

    /**
     * Create an answer message.
     *
     * @param sdp The SDP answer
     * @return The answer message
     */
    public BaseMessage createAnswerMessage(String sdp) {
        BaseMessage message = new BaseMessage(MessageTypes.ANSWER);
        message.put("sdp", sdp);
        return message;
    }

}