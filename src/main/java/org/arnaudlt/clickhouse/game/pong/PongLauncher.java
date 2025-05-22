package org.arnaudlt.clickhouse.game.pong;

/**
 * Simple launcher class for the Pong game.
 * This class provides a direct way to start the game without going through the Spring Boot application.
 */
public class PongLauncher {
    
    /**
     * Main method to launch the Pong game directly.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Launch the Pong game
        PongGame.main(args);
    }
}