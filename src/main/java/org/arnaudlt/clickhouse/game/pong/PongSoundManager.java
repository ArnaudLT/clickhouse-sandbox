package org.arnaudlt.clickhouse.game.pong;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sound manager for the Pong game.
 * This class handles loading and playing sound effects for various game events.
 */
public class PongSoundManager {

    // Sound types
    public enum SoundType {
        PADDLE_HIT,
        WALL_HIT,
        SCORE,
        GAME_START,
        GAME_END,
        MENU_CLICK,
        MENU_SELECT
    }

    // Map to store sound file paths for each sound type
    private final Map<SoundType, String> soundFiles = new HashMap<>();

    // Flag to enable/disable sounds
    private boolean soundEnabled = true;

    // Executor service for playing sounds asynchronously
    private final ExecutorService soundExecutor = Executors.newCachedThreadPool();

    /**
     * Constructor that initializes the sound manager with default sounds.
     */
    public PongSoundManager() {
        // Create a directory to store the sound files
        File soundDir = new File("src/main/resources/sounds");
        if (!soundDir.exists()) {
            soundDir.mkdirs();
        }

        // Initialize sounds with paths from resources
        soundFiles.put(SoundType.PADDLE_HIT, "src/main/resources/sounds/paddle_hit.wav");
        soundFiles.put(SoundType.WALL_HIT, "src/main/resources/sounds/wall_hit.wav");
        soundFiles.put(SoundType.SCORE, "src/main/resources/sounds/score.wav");
        soundFiles.put(SoundType.GAME_START, "src/main/resources/sounds/game_start.wav");
        soundFiles.put(SoundType.GAME_END, "src/main/resources/sounds/game_end.wav");
        soundFiles.put(SoundType.MENU_CLICK, "src/main/resources/sounds/menu_click.wav");
        soundFiles.put(SoundType.MENU_SELECT, "src/main/resources/sounds/menu_select.wav");

        // Verify that sound files exist
        for (Map.Entry<SoundType, String> entry : soundFiles.entrySet()) {
            File soundFile = new File(entry.getValue());
            if (!soundFile.exists()) {
                System.err.println("Sound file not found: " + entry.getValue());
            }
        }
    }

    /**
     * Play a sound of the specified type.
     * 
     * @param type The type of sound to play
     */
    public void playSound(SoundType type) {
        if (!soundEnabled) return;

        String filePath = soundFiles.get(type);
        if (filePath != null) {
            // Play sound in a separate thread to avoid blocking the game
            soundExecutor.submit(() -> {
                try {
                    // Get audio input stream from the sound file
                    File audioFile = new File(filePath);
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);

                    // Extract the format of the audio file
                    AudioFormat originalFormat = audioInputStream.getFormat();

                    // Define a compatible format (16-bit, 44100 Hz, stereo)
                    AudioFormat compatibleFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            44100,
                            16,
                            originalFormat.getChannels(),
                            originalFormat.getChannels() * 2,
                            44100,
                            false
                    );

                    // Convert the audio stream if necessary
                    AudioInputStream compatibleAudioStream;
                    if (!AudioSystem.isConversionSupported(compatibleFormat, originalFormat)) {
                        System.err.println("Warning: Audio format conversion not supported for " + filePath);
                        compatibleAudioStream = audioInputStream; // Use original format as fallback
                    } else {
                        compatibleAudioStream = AudioSystem.getAudioInputStream(compatibleFormat, audioInputStream);
                    }

                    // Get a clip resource
                    Clip clip = AudioSystem.getClip();

                    // Open audio clip with the converted audio stream
                    clip.open(compatibleAudioStream);

                    // Add a listener to close resources when playback completes
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                            try {
                                compatibleAudioStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    // Start playing
                    clip.start();

                } catch (Exception e) {
                    System.err.println("Error playing sound: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * Enable or disable all sounds.
     * 
     * @param enabled True to enable sounds, false to disable
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * Check if sound is currently enabled.
     * 
     * @return True if sound is enabled, false otherwise
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Toggle sound on/off.
     * 
     * @return The new state of sound (true if enabled, false if disabled)
     */
    public boolean toggleSound() {
        soundEnabled = !soundEnabled;
        return soundEnabled;
    }

    /**
     * Clean up resources when the sound manager is no longer needed.
     * Should be called when the game is closing.
     */
    public void shutdown() {
        soundExecutor.shutdown();
    }
}
