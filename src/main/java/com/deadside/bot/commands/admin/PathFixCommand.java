package com.deadside.bot.commands.admin;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.fixes.DirectPathResolutionFix;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.utils.OwnerCheck;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.util.Map;

/**
 * Path fix command for admin use
 * This command allows admins to fix server paths
 */
public class PathFixCommand extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PathFixCommand.class);
    
    private final GameServerRepository repository;
    private final SftpConnector connector;
    
    /**
     * Constructor
     * @param repository The game server repository
     * @param connector The SFTP connector
     */
    public PathFixCommand(GameServerRepository repository, SftpConnector connector) {
        this.repository = repository;
        this.connector = connector;
    }
    
    /**
     * Create the command for the JDA SlashCommand system
     * @return The slash command data for this command
     */
    public net.dv8tion.jda.api.interactions.commands.build.SlashCommandData createCommand() {
        return net.dv8tion.jda.api.interactions.commands.build.Commands.slash("pathfix", "Fix server paths")
            .addOption(net.dv8tion.jda.api.interactions.commands.OptionType.STRING, "server", "Server name to fix (leave empty for all servers)", false);
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pathfix")) {
            return;
        }
        
        // Check if user is owner or admin
        if (!OwnerCheck.isOwner(event.getUser()) && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        // Defer reply
        event.deferReply().queue();
        
        try {
            // Get options
            OptionMapping serverOption = event.getOption("server");
            String serverName = serverOption != null ? serverOption.getAsString() : null;
            
            // Check if fixing specific server or all servers
            if (serverName != null && !serverName.isEmpty()) {
                // Fix specific server
                fixServer(event, serverName);
            } else {
                // Fix all servers
                fixAllServers(event);
            }
        } catch (Exception e) {
            logger.error("Error executing pathfix command: {}", e.getMessage(), e);
            event.getHook().sendMessage("Error executing path fix: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Fix paths for a server
     * @param event The slash command event
     * @param serverName The server name
     */
    private void fixServer(SlashCommandInteractionEvent event, String serverName) {
        try {
            logger.info("Fixing server paths for: {}", serverName);
            
            // Get guild ID
            long guildId = event.getGuild().getIdLong();
            
            // Find server
            GameServer server = repository.findByGuildIdAndName(guildId, serverName);
            
            if (server == null) {
                event.getHook().sendMessage("Server not found: " + serverName).queue();
                return;
            }
            
            // Create path fix
            DirectPathResolutionFix pathFix = new DirectPathResolutionFix(connector);
            
            // Fix paths
            Map<String, Object> results = pathFix.fixServerPaths(server);
            
            // Check if paths were fixed
            boolean pathsFixed = results.containsKey("pathsFixed") && (boolean)results.get("pathsFixed");
            
            if (pathsFixed) {
                // Apply updates
                server = pathFix.applyServerUpdates(server, results);
                
                // Save server
                repository.save(server);
                
                logger.info("Fixed paths for server: {}", server.getName());
            }
            
            // Create embed
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Path Fix Results")
                .setColor(pathsFixed ? Color.GREEN : Color.RED)
                .setDescription("Path fix results for server: " + server.getName());
            
            // Add CSV path results
            boolean csvPathFixed = results.containsKey("csvPathFixed") && (boolean)results.get("csvPathFixed");
            
            if (csvPathFixed) {
                String csvPath = (String)results.get("csvPath");
                String originalCsvPath = (String)results.get("originalCsvPath");
                
                embed.addField("CSV Path Fixed", "Yes", true);
                embed.addField("Original CSV Path", originalCsvPath, false);
                embed.addField("New CSV Path", csvPath, false);
            } else {
                embed.addField("CSV Path Fixed", "No", true);
            }
            
            // Add log path results
            boolean logPathFixed = results.containsKey("logPathFixed") && (boolean)results.get("logPathFixed");
            
            if (logPathFixed) {
                String logPath = (String)results.get("logPath");
                String originalLogPath = (String)results.get("originalLogPath");
                
                embed.addField("Log Path Fixed", "Yes", true);
                embed.addField("Original Log Path", originalLogPath, false);
                embed.addField("New Log Path", logPath, false);
            } else {
                embed.addField("Log Path Fixed", "No", true);
            }
            
            // Add error if present
            if (results.containsKey("error")) {
                embed.addField("Error", (String)results.get("error"), false);
            }
            
            // Send embed
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            logger.error("Error fixing server paths for {}: {}", serverName, e.getMessage(), e);
            event.getHook().sendMessage("Error fixing server paths: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Fix paths for all servers
     * @param event The slash command event
     */
    private void fixAllServers(SlashCommandInteractionEvent event) {
        try {
            logger.info("Fixing paths for all servers");
            
            // Get guild ID
            long guildId = event.getGuild().getIdLong();
            
            // Create embed
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Path Fix Results")
                .setColor(Color.BLUE)
                .setDescription("Path fix results for all servers");
            
            // Find servers for guild
            java.util.List<GameServer> servers = new java.util.ArrayList<>();
            GameServer defaultServer = repository.findByGuildIdAndName(guildId, "Default");
            
            if (defaultServer != null) {
                servers.add(defaultServer);
            }
            
            if (servers.isEmpty()) {
                event.getHook().sendMessage("No servers found for this guild.").queue();
                return;
            }
            
            // Track statistics
            int totalServers = servers.size();
            int fixedServers = 0;
            int failedServers = 0;
            
            // Create path fix
            DirectPathResolutionFix pathFix = new DirectPathResolutionFix(connector);
            
            // Fix each server
            for (GameServer currentServer : servers) {
                try {
                    // Fix paths
                    Map<String, Object> results = pathFix.fixServerPaths(currentServer);
                    
                    // Check if paths were fixed
                    boolean pathsFixed = results.containsKey("pathsFixed") && (boolean)results.get("pathsFixed");
                    
                    if (pathsFixed) {
                        // Apply updates
                        currentServer = pathFix.applyServerUpdates(currentServer, results);
                        
                        // Save server
                        repository.save(currentServer);
                        
                        logger.info("Fixed paths for server: {}", currentServer.getName());
                        fixedServers++;
                    } else {
                        logger.warn("Failed to fix paths for server: {}", currentServer.getName());
                        failedServers++;
                    }
                } catch (Exception e) {
                    logger.error("Error fixing paths for server {}: {}", currentServer.getName(), e.getMessage(), e);
                    failedServers++;
                }
            }
            
            // Add statistics
            embed.addField("Total Servers", String.valueOf(totalServers), true);
            embed.addField("Fixed Servers", String.valueOf(fixedServers), true);
            embed.addField("Failed Servers", String.valueOf(failedServers), true);
            
            // Send embed
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            logger.error("Error fixing paths for all servers: {}", e.getMessage(), e);
            event.getHook().sendMessage("Error fixing paths for all servers: " + e.getMessage()).queue();
        }
    }
}