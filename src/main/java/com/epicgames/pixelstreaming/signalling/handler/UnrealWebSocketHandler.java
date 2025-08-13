// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.handler;

import io.netty.channel.ChannelHandlerContext;
import com.epicgames.pixelstreaming.signalling.message.MessageHelper;
import com.epicgames.pixelstreaming.signalling.service.IConnection;
import com.epicgames.pixelstreaming.signalling.service.UnrealConnection;
import com.epicgames.pixelstreaming.signalling.service.ConnectionManager;

/**
 * WebSocket handler for Unreal Engine connections.
 */
public class UnrealWebSocketHandler extends AbstractWebSocketHandler {

    private final ConnectionManager connectionManager;

    public UnrealWebSocketHandler(MessageHelper messageHelper, ConnectionManager connectionManager) {
        super(messageHelper);
        this.connectionManager = connectionManager;
    }

    @Override
    protected IConnection createConnection(ChannelHandlerContext ctx) {
        UnrealConnection unrealConnection = new UnrealConnection(
            ctx.channel(), 
            messageHelper, 
            connectionManager
        );
        connectionManager.addUnrealConnection(unrealConnection);
        return unrealConnection;
    }
}