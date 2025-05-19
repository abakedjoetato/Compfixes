package com.deadside.bot.parsers.fixes;

import com.deadside.bot.Bot;
import com.deadside.bot.commands.admin.PathFixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for Deadside path fixes
 * This class provides the main entry point for initializing
 * and applying all path fixes
 */
public class DeadsidePathFix {
    private static final Logger logger = LoggerFactory.getLogger(DeadsidePathFix.class);
    
    /**
     * Apply all fixes to the Bot
     * @param bot The Bot instance
     * @return True if successful
     */
    public static boolean applyFixes(Bot bot) {
        try {
            logger.info("Applying Deadside path fixes");
            
            // Initialize direct fix
            DirectPathResolutionFix.initialize();
            
            // Create integration
            ParserIsolationIntegration integration = new ParserIsolationIntegration(bot);
            
            // Fix paths for all servers
            int fixed = integration.fixAllServerPaths();
            logger.info("Fixed paths for {} servers", fixed);
            
            // Register PathFixCommand
            registerPathFixCommand(bot);
            
            logger.info("Deadside path fixes applied successfully");
            return true;
        } catch (Exception e) {
            logger.error("Error applying Deadside path fixes: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Register the PathFixCommand with the Bot
     * @param bot The Bot instance
     */
    private static void registerPathFixCommand(Bot bot) {
        try {
            // Create the command
            PathFixCommand command = new PathFixCommand(bot);
            
            // Register the command with JDA
            bot.getJda().addEventListener(command);
            
            // Update commands
            bot.getJda().updateCommands().addCommands(PathFixCommand.createCommand()).queue();
            
            logger.info("PathFixCommand registered successfully");
        } catch (Exception e) {
            logger.error("Error registering PathFixCommand: {}", e.getMessage(), e);
        }
    }
}