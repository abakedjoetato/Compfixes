package com.deadside.bot.listeners;

import com.deadside.bot.commands.CommandManager;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for slash command interactions
 */
public class CommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(CommandListener.class);
    
    private final CommandManager commandManager;
    
    /**
     * Constructor
     * @param commandManager The command manager
     */
    public CommandListener(CommandManager commandManager) {
        this.commandManager = commandManager;
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        logger.debug("Received slash command: {}", event.getName());
        
        try {
            // Handle the command
            handleCommand(event);
        } catch (Exception e) {
            logger.error("Error handling slash command {}: {}", 
                event.getName(), e.getMessage(), e);
            
            // Reply with error message
            String errorMessage = "An error occurred while processing the command";
            if (!event.isAcknowledged()) {
                event.reply(errorMessage).setEphemeral(true).queue();
            }
        }
    }
    
    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        logger.debug("Received autocomplete for command: {}, option: {}", 
            event.getName(), event.getFocusedOption().getName());
        
        try {
            // Handle the autocomplete
            handleAutoComplete(event);
        } catch (Exception e) {
            logger.error("Error handling autocomplete for command {}, option {}: {}", 
                event.getName(), event.getFocusedOption().getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Handle a slash command
     * @param event The slash command event
     */
    private void handleCommand(SlashCommandInteractionEvent event) {
        // Simple implementation for compilation
        logger.info("Handling command: {}", event.getName());
        event.reply("Command received: " + event.getName()).queue();
    }
    
    /**
     * Handle an autocomplete interaction
     * @param event The autocomplete event
     */
    private void handleAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // Simple implementation for compilation
        logger.info("Handling autocomplete for command: {}, option: {}", 
            event.getName(), event.getFocusedOption().getName());
    }
}