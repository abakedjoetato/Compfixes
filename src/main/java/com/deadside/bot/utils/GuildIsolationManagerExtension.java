package com.deadside.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension utilities for GuildIsolationManager
 * Provides compatibility methods for different data types
 */
public class GuildIsolationManagerExtension {
    private static final Logger logger = LoggerFactory.getLogger(GuildIsolationManagerExtension.class);
    
    /**
     * Set context with string server ID
     * @param guildId The guild ID
     * @param serverIdStr The server ID as string
     */
    public static void setContextWithStringServerId(Long guildId, String serverIdStr) {
        if (guildId == null) {
            logger.warn("Attempted to set context with null guildId");
            return;
        }
        
        // Always set with null userId to avoid type conversion issues
        GuildIsolationManager.getInstance().setContext(guildId, null);
    }
    
    /**
     * Set context with string server ID (overload for primitive long)
     * @param guildId The guild ID as primitive long
     * @param serverIdStr The server ID as string
     */
    public static void setContextWithStringServerId(long guildId, String serverIdStr) {
        setContextWithStringServerId(Long.valueOf(guildId), serverIdStr);
    }
    
    /**
     * Get isolation context string from guild ID and server ID
     * @param guildId The guild ID
     * @param serverId The server ID
     * @return The isolation context string
     */
    public static String getIsolationContextString(Long guildId, String serverId) {
        if (guildId == null) {
            return null;
        }
        
        return guildId.toString() + (serverId != null ? ":" + serverId : "");
    }
    
    /**
     * Get isolation context string from guild ID and server ID (overload for primitive long)
     * @param guildId The guild ID as primitive long
     * @param serverId The server ID
     * @return The isolation context string
     */
    public static String getIsolationContextString(long guildId, String serverId) {
        return getIsolationContextString(Long.valueOf(guildId), serverId);
    }
}