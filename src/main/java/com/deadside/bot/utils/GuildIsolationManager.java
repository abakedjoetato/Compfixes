package com.deadside.bot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for guild isolation context
 * This class provides thread-local storage for guild isolation context
 * to ensure proper data isolation between guilds
 */
public class GuildIsolationManager {
    private static final Logger logger = LoggerFactory.getLogger(GuildIsolationManager.class);
    
    // Singleton instance
    private static GuildIsolationManager instance;
    
    // Thread-local storage for guild ID
    private final ThreadLocal<Long> guildIdContext = new ThreadLocal<>();
    
    // Thread-local storage for user ID
    private final ThreadLocal<Long> userIdContext = new ThreadLocal<>();
    
    /**
     * Private constructor for singleton pattern
     */
    private GuildIsolationManager() {
        logger.info("GuildIsolationManager initialized");
    }
    
    /**
     * Get the singleton instance
     * @return The singleton instance
     */
    public static synchronized GuildIsolationManager getInstance() {
        if (instance == null) {
            instance = new GuildIsolationManager();
        }
        return instance;
    }
    
    /**
     * Set the current context
     * @param guildId The guild ID
     * @param userId The user ID (nullable)
     */
    public void setContext(Long guildId, Long userId) {
        if (guildId == null) {
            logger.error("Attempted to set null guildId in isolation context");
            return;
        }
        
        guildIdContext.set(guildId);
        userIdContext.set(userId);
        
        logger.debug("Set isolation context: guildId={}, userId={}", 
            guildId, userId != null ? userId : "null");
    }
    
    /**
     * Get the current guild ID
     * @return The current guild ID
     */
    public Long getGuildId() {
        return guildIdContext.get();
    }
    
    /**
     * Get the current user ID
     * @return The current user ID
     */
    public Long getUserId() {
        return userIdContext.get();
    }
    
    /**
     * Clear the current context
     */
    public void clearContext() {
        guildIdContext.remove();
        userIdContext.remove();
        
        logger.debug("Cleared isolation context");
    }
    
    /**
     * Run a task with a specific context
     * @param guildId The guild ID
     * @param userId The user ID (nullable)
     * @param task The task to run
     */
    public void withContext(Long guildId, Long userId, Runnable task) {
        if (guildId == null) {
            logger.error("Attempted to run task with null guildId");
            return;
        }
        
        // Save current context
        Long prevGuildId = guildIdContext.get();
        Long prevUserId = userIdContext.get();
        
        try {
            // Set new context
            setContext(guildId, userId);
            
            // Run the task
            task.run();
        } finally {
            // Restore previous context
            if (prevGuildId != null) {
                setContext(prevGuildId, prevUserId);
            } else {
                clearContext();
            }
        }
    }
    
    /**
     * Check if a guild is the current context
     * @param guildId The guild ID to check
     * @return True if the guild is the current context
     */
    public boolean isCurrentGuild(Long guildId) {
        if (guildId == null) {
            return false;
        }
        
        Long currentGuildId = guildIdContext.get();
        return currentGuildId != null && currentGuildId.equals(guildId);
    }
}