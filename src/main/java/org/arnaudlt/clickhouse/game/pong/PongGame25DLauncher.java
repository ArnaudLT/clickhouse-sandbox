package org.arnaudlt.clickhouse.game.pong;

/**
 * Simple launcher class for the 2.5D Pong game.
 * This class provides a direct way to start the 2.5D version of the game without going through the Spring Boot application.
 */
public class PongGame25DLauncher {
    
    /**
     * Main method to launch the 2.5D Pong game directly.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Launch the 2.5D Pong game
        PongGame25D.main(args);
    }
}