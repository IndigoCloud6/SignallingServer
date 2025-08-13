// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.service;

import io.netty.channel.Channel;
import com.epicgames.pixelstreaming.signalling.message.BaseMessage;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.message.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an Unreal Engine connection in the signalling server.
 * Unreal connections can act as both content consumers and interactive clients.
 */
public class UnrealConnection extends AbstractConnection {

    private static final Logger logger = LoggerFactory.getLogger(UnrealConnection.class);

    private String unrealId;
    private StreamerConnection subscribedStreamer;
    private final ConnectionManager connectionManager;

    public UnrealConnection(Channel channel, MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(channel, ConnectionType.UNREAL, messageHelper);
        this.connectionManager = connectionManager;
        onConnected();
    }

    @Override
    public void handleMessage(BaseMessage message) {
        updateLastActivity();
        
        if (message == null || message.getType() == null) {
            logger.warn("Received null or invalid message from unreal client {}", id);
            return;
        }

        logger.debug("Unreal client {} received message: {}", id, message.getType());

        switch (message.getType()) {
            case MessageTypes.IDENTIFY:
                handleIdentifyMessage(message);
                break;
            
            case MessageTypes.OFFER:
            case MessageTypes.ANSWER:
            case MessageTypes.ICE_CANDIDATE:
            case MessageTypes.ICE_CANDIDATE_ERROR:
                forwardToStreamer(message);
                break;
            
            case MessageTypes.DATA_CHANNEL_REQUEST:
                handleDataChannelRequest(message);
                break;
            
            case MessageTypes.PING:
                sendMessage(messageHelper.createPongMessage());
                break;
            
            case MessageTypes.DISCONNECT:
                handleDisconnect();
                break;
            
            default:
                logger.debug("Unhandled message type from unreal client {}: {}", id, message.getType());
                break;
        }
    }

    /**
     * Handle Unreal client identification message.
     */
    private void handleIdentifyMessage(BaseMessage message) {
        if (message.getData() != null && message.getData().has("unrealId")) {
            this.unrealId = message.getData().get("unrealId").asText();
            logger.info("Unreal client {} identified with ID: {}", id, unrealId);
        }

        // Send configuration to the Unreal client
        sendConfigMessage();
        
        // Try to subscribe to an available streamer
        subscribeToStreamer();
    }

    /**
     * Send configuration message to the Unreal client.
     */
    private void sendConfigMessage() {
        // Get peer options from configuration or create default
        BaseMessage configMessage = messageHelper.createConfigMessage(null);
        sendMessage(configMessage);
    }

    /**
     * Subscribe this Unreal client to an available streamer.
     */
    private void subscribeToStreamer() {
        StreamerConnection availableStreamer = connectionManager.getAvailableStreamer();
        if (availableStreamer != null) {
            subscribeToStreamer(availableStreamer);
        } else {
            logger.info("No available streamer for unreal client {}", id);
        }
    }

    /**
     * Subscribe this Unreal client to a specific streamer.
     */
    public void subscribeToStreamer(StreamerConnection streamer) {
        if (subscribedStreamer != null) {
            unsubscribeFromStreamer();
        }

        this.subscribedStreamer = streamer;
        // Note: Unreal connections might be treated similarly to players for subscription
        // This depends on the specific requirements
        
        logger.info("Unreal client {} subscribed to streamer {}", id, streamer.getId());
        
        // Notify about connection count change
        connectionManager.notifyPlayerCountChanged();
    }

    /**
     * Unsubscribe this Unreal client from their current streamer.
     */
    public void unsubscribeFromStreamer() {
        if (subscribedStreamer != null) {
            logger.info("Unreal client {} unsubscribed from streamer {}", id, subscribedStreamer.getId());
            subscribedStreamer = null;
            connectionManager.notifyPlayerCountChanged();
        }
    }

    /**
     * Forward a message to the subscribed streamer.
     */
    private void forwardToStreamer(BaseMessage message) {
        if (subscribedStreamer != null && subscribedStreamer.isActive()) {
            // Add unreal ID to the message for routing
            if (message.getId() == null) {
                message.setId(id);
            }
            subscribedStreamer.sendMessage(message);
            logger.debug("Forwarded message from unreal client {} to streamer {}: {}", 
                        id, subscribedStreamer.getId(), message.getType());
        } else {
            logger.warn("Unreal client {} attempted to send message without active streamer: {}", 
                       id, message.getType());
            BaseMessage errorMessage = messageHelper.createErrorMessage("No active streamer available");
            sendMessage(errorMessage);
        }
    }

    /**
     * Handle data channel request from Unreal client.
     */
    private void handleDataChannelRequest(BaseMessage message) {
        // Forward to streamer if available
        forwardToStreamer(message);
    }

    /**
     * Handle Unreal client disconnect.
     */
    private void handleDisconnect() {
        logger.info("Unreal client {} requested disconnect", id);
        close();
    }

    @Override
    protected void onDisconnected() {
        super.onDisconnected();
        unsubscribeFromStreamer();
        connectionManager.removeUnrealConnection(this);
    }

    // Getters
    public String getUnrealId() {
        return unrealId;
    }

    public StreamerConnection getSubscribedStreamer() {
        return subscribedStreamer;
    }

    public boolean hasSubscribedStreamer() {
        return subscribedStreamer != null && subscribedStreamer.isActive();
    }
}