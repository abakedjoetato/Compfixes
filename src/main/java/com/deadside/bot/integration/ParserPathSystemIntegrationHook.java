package com.deadside.bot.integration;

import com.deadside.bot.Bot;
import com.deadside.bot.commands.admin.PathRepairCommand;
import com.deadside.bot.parsers.fixes.ParserPathAutoInitializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration hook for the parser path resolution system
 * This class is responsible for integrating the parser path resolution system
 * into the Bot during its initialization
 */
public class ParserPathSystemIntegrationHook {
    private static final Logger logger = LoggerFactory.getLogger(ParserPathSystemIntegrationHook.class);
    
    /**
     * Initialize the path resolution system integration with the Bot
     * @param bot The Bot instance
     * @return True if integration was successful
     */
    public static boolean initialize(Bot bot) {
        if (bot == null) {
            logger.error("Cannot initialize path system integration with null Bot instance");
            return false;
        }
        
        try {
            logger.info("Initializing parser path system integration");
            
            // Initialize the parser path resolution system
            boolean success = ParserPathAutoInitializer.initializeWithBot(bot);
            
            if (success) {
                logger.info("Parser path resolution system integration initialized successfully");
                
                // Register the path repair command
                registerPathRepairCommand(bot);
                
                return true;
            } else {
                logger.error("Failed to initialize parser path resolution system integration");
                return false;
            }
        } catch (Exception e) {
            logger.error("Error initializing parser path system integration: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Register the path repair command with the Bot
     * @param bot The Bot instance
     */
    private static void registerPathRepairCommand(Bot bot) {
        try {
            JDA jda = bot.getJda();
            if (jda == null) {
                logger.warn("Cannot register path repair command, JDA is null");
                return;
            }
            
            // Create the path repair command
            PathRepairCommand pathRepairCommand = new PathRepairCommand(bot);
            SlashCommandData commandData = PathRepairCommand.createCommand();
            
            // Register the command globally
            jda.addEventListener(pathRepairCommand);
            
            // Update commands if needed
            List<SlashCommandData> existingCommands = new ArrayList<>(jda.retrieveCommands().complete());
            boolean commandExists = existingCommands.stream()
                .anyMatch(cmd -> cmd.getName().equals("pathrepair"));
            
            if (!commandExists) {
                existingCommands.add(commandData);
                jda.updateCommands().addCommands(existingCommands).queue();
                logger.info("Registered path repair command");
            } else {
                logger.info("Path repair command already registered");
            }
        } catch (Exception e) {
            logger.error("Error registering path repair command: {}", e.getMessage(), e);
        }
    }
}