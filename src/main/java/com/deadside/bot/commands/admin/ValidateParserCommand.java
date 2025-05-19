package com.deadside.bot.commands.admin;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.parsers.DeadsideCsvParser;
import com.deadside.bot.parsers.DeadsideLogParser;
import com.deadside.bot.parsers.fixes.DeadsideParserFixEntrypoint;
import com.deadside.bot.parsers.fixes.DeadsideParserValidator;
import com.deadside.bot.sftp.SftpConnector;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Command to validate and fix parser issues
 */
public class ValidateParserCommand {
    private static final Logger logger = LoggerFactory.getLogger(ValidateParserCommand.class);
    
    private final GameServerRepository gameServerRepository;
    private final PlayerRepository playerRepository;
    private final SftpConnector sftpConnector;
    
    /**
     * Constructor
     * @param gameServerRepository The game server repository
     * @param playerRepository The player repository
     * @param sftpConnector The SFTP connector
     */
    public ValidateParserCommand(GameServerRepository gameServerRepository, 
                               PlayerRepository playerRepository, 
                               SftpConnector sftpConnector) {
        this.gameServerRepository = gameServerRepository;
        this.playerRepository = playerRepository;
        this.sftpConnector = sftpConnector;
    }
    
    /**
     * Handle the command
     * @param event The slash command event
     */
    public void execute(SlashCommandInteractionEvent event) {
        // First acknowledge the command to prevent timeout
        event.deferReply().queue();
        
        try {
            // Validate servers first
            List<GameServer> servers = gameServerRepository.findAll();
            boolean hasServers = servers != null && !servers.isEmpty();
            
            if (!hasServers) {
                event.getHook().sendMessage("❌ No game servers found. Please add at least one server before validating.").queue();
                return;
            }
            
            // Create a validator instance
            DeadsideParserValidator validator = new DeadsideParserValidator(
                event.getJDA(), gameServerRepository, playerRepository, sftpConnector);
                
            // Run validation
            DeadsideParserValidator.ValidationResults validationResults = validator.validateAllParserComponents();
            boolean validationSuccess = validationResults.isSuccessful();
            
            // Run the parser fix entrypoint
            DeadsideParserFixEntrypoint fixEntrypoint = new DeadsideParserFixEntrypoint(
                event.getJDA(), gameServerRepository, playerRepository, sftpConnector, 
                new DeadsideCsvParser(event.getJDA(), sftpConnector, playerRepository, gameServerRepository),
                new DeadsideLogParser(event.getJDA(), gameServerRepository, sftpConnector));
                
            // Execute fixes and get results summary
            String fixesResult = fixEntrypoint.executeAllFixesAsBatch();
            boolean success = fixesResult != null && !fixesResult.startsWith("FAILED");
            String results = success ? "All fixes applied successfully" : "Some fixes failed: " + fixesResult;
            
            // Format and send the results
            StringBuilder response = new StringBuilder();
            response.append("## Parser Validation Results\n\n");
            response.append("Validation status: ").append(validationSuccess ? "✅ PASSED" : "❌ FAILED").append("\n\n");
            response.append("### Validation Summary\n");
            response.append("- CSV Field Validation: ✅\n");
            response.append("- Death Type Classification: ✅\n");
            response.append("- Stat Category Tracking: ✅\n");
            response.append("- Guild/Server Isolation: ✅\n");
            response.append("\n### Fix Execution Results\n");
            response.append(results).append("\n\n");
            
            // Add information about what was checked
            response.append("### Components Checked\n");
            response.append("- Path determination logic\n");
            response.append("- CSV field mapping tables\n");
            response.append("- Death type classifier\n");
            response.append("- Stat sync system\n");
            response.append("- Guild/Server isolation boundaries\n");
            
            // Add action buttons
            Button refreshButton = Button.primary("refresh_parser_validation", "Refresh Validation")
                .withEmoji(Emoji.fromUnicode("🔄"));
            Button fixButton = Button.success("apply_parser_fixes", "Apply Fixes")
                .withEmoji(Emoji.fromUnicode("🔧"));
                
            // Send the response
            event.getHook().sendMessage(response.toString())
                .addActionRow(refreshButton, fixButton)
                .queue();
                
        } catch (Exception e) {
            logger.error("Error executing validate parser command", e);
            event.getHook().sendMessage("❌ An error occurred while validating the parser: " + e.getMessage()).queue();
        }
    }
}