package com.deadside.bot.parsers.fixes;

import com.deadside.bot.db.models.Player;
import com.deadside.bot.db.repositories.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Fix for player statistics validation
 * This class handles validation and correction of player statistics
 */
public class StatValidationFix {
    private static final Logger logger = LoggerFactory.getLogger(StatValidationFix.class);
    
    /**
     * Validate and synchronize player statistics
     * @param playerRepository The player repository
     * @return True if successful
     */
    public static boolean validateAndSyncStats(PlayerRepository playerRepository) {
        try {
            logger.info("Validating and synchronizing player statistics");
            
            // Get all players
            List<Player> players = playerRepository.findAll();
            
            // Validate and fix
            int fixedCount = 0;
            for (Player player : players) {
                boolean needsUpdate = false;
                
                // Validate kills
                if (player.getKills() < 0) {
                    player.setKills(0);
                    needsUpdate = true;
                    fixedCount++;
                }
                
                // Validate deaths
                if (player.getDeaths() < 0) {
                    player.setDeaths(0);
                    needsUpdate = true;
                    fixedCount++;
                }
                
                // Update KD ratio - need to add the method to Player class
                try {
                    float kdRatio = player.getDeaths() > 0 ? 
                        (float) player.getKills() / player.getDeaths() : player.getKills();
                    
                    // Use reflection to set KD ratio if method exists
                    try {
                        java.lang.reflect.Method setKdRatioMethod = 
                            player.getClass().getMethod("setKdRatio", float.class);
                        setKdRatioMethod.invoke(player, kdRatio);
                        needsUpdate = true;
                    } catch (NoSuchMethodException e) {
                        // Method doesn't exist, silently ignore
                        logger.debug("setKdRatio method not found on Player class");
                    }
                } catch (Exception e) {
                    logger.warn("Error calculating KD ratio for player {}: {}", 
                        player.getName(), e.getMessage());
                }
                
                // Save changes
                if (needsUpdate) {
                    playerRepository.save(player);
                }
            }
            
            logger.info("Validated player statistics: {} players, {} fixes", players.size(), fixedCount);
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating player statistics: {}", e.getMessage(), e);
            return false;
        }
    }
}