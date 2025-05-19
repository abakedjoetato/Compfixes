package com.deadside.bot.services;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for running scheduled tasks
 */
public class CronService {
    private static final Logger logger = LoggerFactory.getLogger(CronService.class);
    
    private ScheduledExecutorService scheduler;
    private List<Runnable> hourlyTasks;
    private List<Runnable> dailyTasks;
    
    /**
     * Constructor
     */
    public CronService() {
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.hourlyTasks = new ArrayList<>();
        this.dailyTasks = new ArrayList<>();
        
        logger.info("CronService initialized");
    }
    
    /**
     * Add an hourly task
     * @param task The task to run hourly
     */
    public void addHourlyTask(Runnable task) {
        hourlyTasks.add(task);
        logger.debug("Hourly task added");
    }
    
    /**
     * Add a daily task
     * @param task The task to run daily
     */
    public void addDailyTask(Runnable task) {
        dailyTasks.add(task);
        logger.debug("Daily task added");
    }
    
    /**
     * Start the cron service
     */
    public void start() {
        logger.info("Starting CronService");
        
        // Schedule hourly tasks
        scheduler.scheduleAtFixedRate(() -> {
            for (Runnable task : hourlyTasks) {
                try {
                    task.run();
                } catch (Exception e) {
                    logger.error("Error running hourly task", e);
                }
            }
        }, 0, 1, TimeUnit.HOURS);
        
        // Schedule daily tasks
        scheduler.scheduleAtFixedRate(() -> {
            for (Runnable task : dailyTasks) {
                try {
                    task.run();
                } catch (Exception e) {
                    logger.error("Error running daily task", e);
                }
            }
        }, 0, 24, TimeUnit.HOURS);
        
        logger.info("CronService started");
    }
    
    /**
     * Shutdown the cron service
     */
    public void shutdown() {
        logger.info("Stopping CronService");
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("CronService stopped");
    }
}