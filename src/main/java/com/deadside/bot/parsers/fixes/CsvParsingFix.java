package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.GameServer;
import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import com.deadside.bot.sftp.SftpConnector;
import com.deadside.bot.sftp.SftpPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixes for CSV parsing issues
 */
public class CsvParsingFix {
    private static final Logger logger = LoggerFactory.getLogger(CsvParsingFix.class);
    
    /**
     * Resolve CSV path for a server
     * @param server The game server
     * @param connector The SFTP connector
     * @return The resolved path, or null if not found
     */
    public static String resolveServerCsvPath(GameServer server, SftpConnector connector) {
        return SftpPathUtils.findCsvPath(server, connector);
    }
    
    /**
     * Update server with resolved CSV path
     * @param server The game server
     * @param path The resolved path
     * @return True if successful
     */
    public static boolean updateServerCsvPath(GameServer server, String path) {
        try {
            if (path == null || path.isEmpty()) {
                logger.warn("Cannot update server {} with empty CSV path", server.getName());
                return false;
            }
            
            // Update server path
            String originalPath = server.getDeathlogsDirectory();
            server.setDeathlogsDirectory(path);
            
            // Register path
            ParserIntegrationHooks.recordSuccessfulCsvPath(server, path);
            
            logger.info("Updated CSV path for server {}: {} -> {}", 
                server.getName(), originalPath, path);
            
            return true;
        } catch (Exception e) {
            logger.error("Error updating CSV path for server {}: {}", 
                server.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process a death log line
     * @param server The game server
     * @param line The log line
     * @param playerRepository The player repository
     * @return True if processed successfully
     */
    public static boolean processDeathLogLineFixed(GameServer server, String line, PlayerRepository playerRepository) {
        try {
            if (line == null || line.trim().isEmpty()) {
                return false;
            }
            
            // Split by comma
            String[] parts = line.split(",");
            
            // Check if we have enough fields
            if (parts.length < 3) {
                logger.warn("Invalid death log line (not enough fields): {}", line);
                return false;
            }
            
            // Extract data
            String killer = parts[0].trim();
            String victim = parts[1].trim();
            String weapon = parts[2].trim();
            
            // Skip invalid records
            if (killer.isEmpty() || victim.isEmpty()) {
                logger.warn("Invalid death log line (empty killer or victim): {}", line);
                return false;
            }
            
            // Update player stats
            updatePlayerStats(server, killer, victim, weapon, playerRepository);
            
            return true;
        } catch (Exception e) {
            logger.error("Error processing death log line: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update player stats from a death log entry
     */
    private static void updatePlayerStats(GameServer server, String killer, String victim, 
                                         String weapon, PlayerRepository playerRepository) {
        try {
            // Find or create killer
            Player killerPlayer = playerRepository.findByNameAndServerId(killer, server.getServerId())
                .orElseGet(() -> {
                    Player p = new Player();
                    p.setName(killer);
                    p.setServerId(server.getServerId());
                    p.setGuildId(server.getGuildId());
                    p.setKills(0);
                    p.setDeaths(0);
                    return p;
                });
            
            // Find or create victim
            Player victimPlayer = playerRepository.findByNameAndServerId(victim, server.getServerId())
                .orElseGet(() -> {
                    Player p = new Player();
                    p.setName(victim);
                    p.setServerId(server.getServerId());
                    p.setGuildId(server.getGuildId());
                    p.setKills(0);
                    p.setDeaths(0);
                    return p;
                });
            
            // Update stats
            killerPlayer.setKills(killerPlayer.getKills() + 1);
            victimPlayer.setDeaths(victimPlayer.getDeaths() + 1);
            
            // Update weapon stats
            Map<String, Integer> killerWeapons = killerPlayer.getWeaponKills();
            if (killerWeapons == null) {
                killerWeapons = new HashMap<>();
                killerPlayer.setWeaponKills(killerWeapons);
            }
            
            int weaponKills = killerWeapons.getOrDefault(weapon, 0) + 1;
            killerWeapons.put(weapon, weaponKills);
            
            if (weaponKills > killerPlayer.getMostUsedWeaponKills()) {
                killerPlayer.setMostUsedWeapon(weapon);
                killerPlayer.setMostUsedWeaponKills(weaponKills);
            }
            
            // Update K/D ratio
            float kdRatio = 0.0f;
            if (killerPlayer.getDeaths() > 0) {
                kdRatio = (float) killerPlayer.getKills() / killerPlayer.getDeaths();
            } else {
                kdRatio = killerPlayer.getKills();
            }
            killerPlayer.setKdRatio(kdRatio);
            
            // Save players
            playerRepository.save(killerPlayer);
            playerRepository.save(victimPlayer);
        } catch (Exception e) {
            logger.error("Error updating player stats: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validate and synchronize stats for all players
     * @param playerRepository The player repository
     * @return Number of corrections made
     */
    public static int validateAndSyncStats(PlayerRepository playerRepository) {
        try {
            logger.info("Validating and synchronizing player statistics");
            
            // Get all players
            List<Player> players = playerRepository.findAll();
            
            // Validate and fix
            int fixedCount = 0;
            for (Player player : players) {
                if (player.getKills() < 0) {
                    player.setKills(0);
                    fixedCount++;
                }
                
                if (player.getDeaths() < 0) {
                    player.setDeaths(0);
                    fixedCount++;
                }
                
                if (player.getSuicides() < 0) {
                    player.setSuicides(0);
                    fixedCount++;
                }
                
                // Recalculate metrics if needed
                playerRepository.save(player);
            }
            
            logger.info("Fixed stats for {} players", fixedCount);
            return fixedCount;
        } catch (Exception e) {
            logger.error("Error validating and synchronizing player statistics: {}", e.getMessage(), e);
            return 0;
        }
    }
}