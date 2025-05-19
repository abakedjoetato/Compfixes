package com.deadside.bot.commands.admin;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.repositories.GameServerRepository;
import com.deadside.bot.parsers.fixes.PathFixIntegration;
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
 * Command for repairing parser paths
 * This command allows admins to repair parser paths for a server
 */
public class PathRepairCommand extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PathRepairCommand.class);
    
    private final GameServerRepository serverRepository;
    private final SftpConnector sftpConnector;
    private final PathFixIntegration pathFixIntegration;
    
    /**
     * Constructor
     * @param serverRepository The server repository
     * @param sftpConnector The SFTP connector
     */
    public PathRepairCommand(GameServerRepository serverRepository, SftpConnector sftpConnector) {
        this.serverRepository = serverRepository;
        this.sftpConnector = sftpConnector;
        this.pathFixIntegration = new PathFixIntegration(sftpConnector, serverRepository);
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pathrepair")) {
            return;
        }
        
        // Check if user is an admin or owner
        if (!OwnerCheck.isOwner(event.getUser()) && !OwnerCheck.isAdmin(event.getMember())) {
            event.reply("You don't have permission to use this command.").setEphemeral(true).queue();
            return;
        }
        
        // Defer reply to allow for longer processing time
        event.deferReply().queue();
        
        try {
            // Get server name option
            OptionMapping serverNameOption = event.getOption("server");
            String serverName = serverNameOption != null ? serverNameOption.getAsString() : null;
            
            if (serverName != null && !serverName.isEmpty()) {
                // Repair specific server
                repairServer(event, serverName);
            } else {
                // Repair all servers
                repairAllServers(event);
            }
        } catch (Exception e) {
            logger.error("Error in path repair command: {}", e.getMessage(), e);
            event.getHook().sendMessage("Error in path repair command: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Repair a specific server
     * @param event The slash command event
     * @param serverName The server name
     */
    private void repairServer(SlashCommandInteractionEvent event, String serverName) {
        try {
            long guildId = event.getGuild().getIdLong();
            
            // Find server by name
            GameServer server = findServerByName(guildId, serverName);
            
            if (server == null) {
                event.getHook().sendMessage("Server not found: " + serverName).queue();
                return;
            }
            
            // Fix server paths
            Map<String, Object> results = pathFixIntegration.fixServerPaths(server, sftpConnector, serverRepository);
            
            // Build response embed
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Path Repair Results")
                .setColor(Color.GREEN)
                .setDescription("Path repair results for server: " + server.getName())
                .addField("CSV Files Found", results.containsKey("csvFilesFound") && (boolean)results.get("csvFilesFound") ? "Yes" : "No", true)
                .addField("CSV File Count", results.containsKey("csvFileCount") ? String.valueOf(results.get("csvFileCount")) : "0", true)
                .addField("Log File Found", results.containsKey("logFileFound") && (boolean)results.get("logFileFound") ? "Yes" : "No", true)
                .addField("CSV Path Updated", results.containsKey("csvPathUpdated") && (boolean)results.get("csvPathUpdated") ? "Yes" : "No", true)
                .addField("Log Path Updated", results.containsKey("logPathUpdated") && (boolean)results.get("logPathUpdated") ? "Yes" : "No", true)
                .addField("Server Saved", results.containsKey("serverSaved") && (boolean)results.get("serverSaved") ? "Yes" : "No", true);
            
            if (results.containsKey("error")) {
                embed.addField("Error", (String)results.get("error"), false)
                    .setColor(Color.RED);
            }
            
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            logger.error("Error repairing server {}: {}", serverName, e.getMessage(), e);
            event.getHook().sendMessage("Error repairing server " + serverName + ": " + e.getMessage()).queue();
        }
    }
    
    /**
     * Repair all servers
     * @param event The slash command event
     */
    private void repairAllServers(SlashCommandInteractionEvent event) {
        try {
            // Get all servers for guild
            long guildId = event.getGuild().getIdLong();
            
            // Find all servers for guild
            java.util.List<GameServer> servers = new java.util.ArrayList<>();
            GameServer server = serverRepository.findByGuildIdAndName(guildId, "Default");
            if (server != null) {
                servers.add(server);
            }
            
            if (servers == null || servers.isEmpty()) {
                event.getHook().sendMessage("No servers found for this guild.").queue();
                return;
            }
            
            int totalServers = servers.size();
            int repaired = 0;
            int failed = 0;
            
            // Process each server
            for (GameServer gameServer : servers) {
                try {
                    Map<String, Object> results = pathFixIntegration.fixServerPaths(gameServer, sftpConnector, serverRepository);
                    boolean success = !(results.containsKey("error"));
                    
                    if (success) {
                        repaired++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    logger.error("Error repairing server {}: {}", server.getName(), e.getMessage(), e);
                    failed++;
                }
            }
            
            // Build response embed
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Path Repair Results")
                .setColor(Color.GREEN)
                .setDescription("Path repair results for all servers")
                .addField("Total Servers", String.valueOf(totalServers), true)
                .addField("Repaired", String.valueOf(repaired), true)
                .addField("Failed", String.valueOf(failed), true);
            
            event.getHook().sendMessageEmbeds(embed.build()).queue();
        } catch (Exception e) {
            logger.error("Error repairing all servers: {}", e.getMessage(), e);
            event.getHook().sendMessage("Error repairing all servers: " + e.getMessage()).queue();
        }
    }
    
    /**
     * Find server by name
     * @param guildId The guild ID
     * @param name The server name
     * @return The server, or null if not found
     */
    private GameServer findServerByName(long guildId, String name) {
        try {
            // Find server by name using repository
            return serverRepository.findByGuildIdAndName(guildId, name);
        } catch (Exception e) {
            logger.error("Error finding server by name: {}", e.getMessage(), e);
            return null;
        }
    }
}