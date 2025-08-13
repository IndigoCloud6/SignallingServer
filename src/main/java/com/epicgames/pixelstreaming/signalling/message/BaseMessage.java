// Copyright Epic Games, Inc. All Rights Reserved.
package com.epicgames.pixelstreaming.signalling.message;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all signalling messages.
 * Uses a flexible structure that can handle any JSON message format while keeping
 * the 'type' field strongly typed for the Pixel Streaming protocol.
 */
public class BaseMessage {
    
    @JsonProperty("type")
    private String type;
    
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    public BaseMessage() {}

    public BaseMessage(String type) {
        this.type = type;
    }

    // Getters and setters for type field
    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }

    /**
     * Get all additional properties (used by Jackson for serialization).
     * 
     * @return Map of additional properties
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    /**
     * Set additional property (used by Jackson for deserialization).
     * 
     * @param name Property name
     * @param value Property value
     */
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        additionalProperties.put(name, value);
    }

    /**
     * Get a property value.
     * 
     * @param name Property name
     * @return Property value or null if not found
     */
    public Object get(String name) {
        return additionalProperties.get(name);
    }

    /**
     * Set a property value.
     * 
     * @param name Property name
     * @param value Property value
     */
    public void put(String name, Object value) {
        additionalProperties.put(name, value);
    }

    /**
     * Check if a property exists.
     * 
     * @param name Property name
     * @return true if property exists
     */
    public boolean has(String name) {
        return additionalProperties.containsKey(name);
    }

    /**
     * Remove a property.
     * 
     * @param name Property name
     * @return The removed value, or null if not found
     */
    public Object remove(String name) {
        return additionalProperties.remove(name);
    }

    // Legacy compatibility methods for backward compatibility
    
    /**
     * Get the 'id' property for backward compatibility.
     * 
     * @return The id value or null
     */
    @JsonIgnore
    public String getId() {
        Object id = additionalProperties.get("id");
        return id != null ? id.toString() : null;
    }

    /**
     * Set the 'id' property for backward compatibility.
     * 
     * @param id The id value
     */
    @JsonIgnore
    public void setId(String id) {
        if (id != null) {
            additionalProperties.put("id", id);
        } else {
            additionalProperties.remove("id");
        }
    }

    /**
     * Get the 'data' property for backward compatibility.
     * 
     * @return The data value or null
     */
    @JsonIgnore
    public JsonNode getData() {
        Object data = additionalProperties.get("data");
        if (data instanceof JsonNode) {
            return (JsonNode) data;
        } else if (data != null) {
            // Convert to JsonNode if it's not already
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.valueToTree(data);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Set the 'data' property for backward compatibility.
     * 
     * @param data The data value
     */
    @JsonIgnore
    public void setData(JsonNode data) {
        if (data != null) {
            additionalProperties.put("data", data);
        } else {
            additionalProperties.remove("data");
        }
    }

    @Override
    public String toString() {
        return "BaseMessage{" +
                "type='" + type + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}