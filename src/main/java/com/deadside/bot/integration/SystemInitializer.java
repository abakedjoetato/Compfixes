package com.deadside.bot.integration;

import com.deadside.bot.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System-wide initializer for bot subsystems
 * This class is responsible for initializing all subsystems during bot startup
 */
public class SystemInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SystemInitializer.class);
    
    /**
     * Initialize all system components
     * @param bot The Bot instance
     * @return True if all initializations were successful
     */
    public static boolean initializeAll(Bot bot) {
        if (bot == null) {
            logger.error("Cannot initialize systems with null Bot instance");
            return false;
        }
        
        try {
            logger.info("Initializing all system components");
            
            // Initialize the parser path resolution system
            boolean pathSystemSuccess = ParserPathSystemIntegrationHook.initialize(bot);
            
            if (pathSystemSuccess) {
                logger.info("All system components initialized successfully");
                return true;
            } else {
                logger.error("Failed to initialize all system components");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during system initialization: {}", e.getMessage(), e);
            return false;
        }
    }
}