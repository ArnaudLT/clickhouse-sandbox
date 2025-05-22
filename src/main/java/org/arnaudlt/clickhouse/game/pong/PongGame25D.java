package org.arnaudlt.clickhouse.game.pong;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

/**
 * A 2.5D version of the classic Pong game using JavaFX.
 * This implementation adds perspective, shadows, and 3D-like effects
 * to create a more immersive gaming experience.
 */
public class PongGame25D extends Application {

    // Game states
    private enum GameState {
        WELCOME_SCREEN,
        PLAYING,
        GAME_OVER
    }

    // AI difficulty levels
    private enum AIDifficulty {
        EASY(0.5),
        MEDIUM(0.7),
        HARD(0.9);

        private final double speedMultiplier;

        AIDifficulty(double speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
        }

        public double getSpeedMultiplier() {
            return speedMultiplier;
        }
    }

    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;
    private static final int PADDLE_WIDTH = 20;
    private static final int PADDLE_HEIGHT = 150;
    private static final int PADDLE_DEPTH = 15; // New: depth for 3D effect
    private static final int BALL_RADIUS = 15;
    private static final double PADDLE_SPEED = 7.5;
    private static final double INITIAL_BALL_SPEED = 6.0;

    // Perspective constants
    private static final double PERSPECTIVE_ANGLE = 30.0; // Angle for the perspective view
    private static final double FLOOR_Y = HEIGHT * 0.85; // Y position of the floor
    private static final double COURT_WIDTH = WIDTH * 0.9;
    private static final double COURT_HEIGHT = HEIGHT * 0.7;
    private static final double COURT_DEPTH = 400; // Depth of the court in 3D space

    // Visual design constants
    private static final Color BACKGROUND_COLOR_1 = Color.rgb(10, 15, 30);
    private static final Color BACKGROUND_COLOR_2 = Color.rgb(30, 40, 70);
    private static final Color ACCENT_COLOR = Color.rgb(0, 200, 255);
    private static final Color ACCENT_COLOR_ALT = Color.rgb(255, 50, 100);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color PLAYER_PADDLE_COLOR = Color.rgb(0, 200, 255);
    private static final Color AI_PADDLE_COLOR = Color.rgb(255, 50, 100);
    private static final Color BALL_COLOR = Color.WHITE;
    private static final Color COURT_COLOR = Color.rgb(40, 45, 80, 0.7);
    private static final Color COURT_LINE_COLOR = Color.rgb(255, 255, 255, 0.3);
    private static final Color FLOOR_COLOR = Color.rgb(20, 25, 50);

    // Effects
    private final Glow glowEffect = new Glow(0.8);
    private final DropShadow dropShadow = new DropShadow(BlurType.GAUSSIAN, ACCENT_COLOR, 15, 0.7, 0, 0);
    private final DropShadow textShadow = new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 3, 0.7, 2, 2);
    private final Lighting lighting = createLighting();

    // Fonts
    private static final String FONT_NAME = "Verdana";

    // Animation variables
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private long lastParticleTime = 0;
    private double rotationAngle = 0; // For subtle court rotation animation

    // Sound manager
    private final PongSoundManager soundManager = new PongSoundManager();

    // Particle class for visual effects
    private class Particle {
        double x, y, z; // 3D coordinates
        double speedX, speedY, speedZ;
        double size;
        double opacity;
        Color color;

        Particle(double x, double y, double z, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.speedX = (random.nextDouble() - 0.5) * 3;
            this.speedY = (random.nextDouble() - 0.5) * 3;
            this.speedZ = (random.nextDouble() - 0.5) * 2; // Z-axis movement
            this.size = random.nextDouble() * 5 + 2;
            this.opacity = 1.0;
            this.color = color;
        }

        void update() {
            x += speedX;
            y += speedY;
            z += speedZ;
            opacity -= 0.02;
            size -= 0.1;
        }

        boolean isDead() {
            return opacity <= 0 || size <= 0;
        }

        void render(GraphicsContext gc) {
            if (!isDead()) {
                // Project 3D coordinates to 2D
                Point2D projectedPoint = project3DTo2D(x, y, z);

                // Scale size based on z-coordinate (perspective)
                double scaledSize = size * (1 - z / (COURT_DEPTH * 2));

                gc.setGlobalAlpha(opacity);
                gc.setFill(color);
                gc.fillOval(projectedPoint.getX() - scaledSize/2, 
                           projectedPoint.getY() - scaledSize/2, 
                           scaledSize, scaledSize);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    // Game variables
    private double playerPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double aiPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double ballX = WIDTH / 2;
    private double ballY = HEIGHT / 2;
    private double ballZ = 0; // Z-coordinate for the ball (depth)
    private double ballSpeedX = INITIAL_BALL_SPEED;
    private double ballSpeedY = INITIAL_BALL_SPEED;
    private double ballSpeedZ = 0; // Z-axis movement
    private int playerScore = 0;
    private int aiScore = 0;
    private GameState gameState = GameState.WELCOME_SCREEN;

    // Player name variables
    private String playerName = "";
    private boolean nameInputActive = true; // Start with name input active
    private long lastCursorBlinkTime = 0;
    private boolean showCursor = true;

    // AI difficulty selection
    private AIDifficulty selectedDifficulty = AIDifficulty.MEDIUM; // Default to medium difficulty
    private boolean difficultySelectionActive = false; // Will be set to true after name is confirmed

    // Input handling
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private final Set<KeyCode> processedKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        // Create the canvas
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Create a gradient background
        LinearGradient backgroundGradient = new LinearGradient(
            0, 0, 0, HEIGHT,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, BACKGROUND_COLOR_1),
            new Stop(1, BACKGROUND_COLOR_2)
        );

        // Create the scene with styled background
        StackPane root = new StackPane(canvas);
        root.setBackground(new Background(new BackgroundFill(
            backgroundGradient, CornerRadii.EMPTY, Insets.EMPTY
        )));
        Scene scene = new Scene(root);

        // Set up key event handlers
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> {
            activeKeys.remove(e.getCode());
            processedKeys.remove(e.getCode()); // Remove from processed keys when released
        });

        // Set up the game loop with time tracking
        final long[] lastTime = {System.nanoTime()};
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Calculate delta time for smooth animations
                double deltaTime = (now - lastTime[0]) / 1_000_000_000.0;
                lastTime[0] = now;

                update(deltaTime);
                render(gc);
            }
        };

        // Set up the stage with modern title
        primaryStage.setTitle("PONG - 2.5D Edition");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Create a fade-in effect when starting
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Start the game loop
        gameLoop.start();
    }

    private void update(double deltaTime) {
        // Update rotation angle for subtle animation
        rotationAngle += deltaTime * 5; // 5 degrees per second
        if (rotationAngle > 360) {
            rotationAngle -= 360;
        }

        // Update particles
        updateParticles();

        // Generate particles for ball trail if in PLAYING state
        if (gameState == GameState.PLAYING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastParticleTime > 50) { // Generate particles every 50ms
                addParticles(ballX, ballY, ballZ, 2, BALL_COLOR);
                lastParticleTime = currentTime;
            }
        }

        // Handle name input in welcome screen
        if (gameState == GameState.WELCOME_SCREEN) {
            // Handle cursor blinking
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCursorBlinkTime > 500) { // Blink every 500ms
                showCursor = !showCursor;
                lastCursorBlinkTime = currentTime;
            }

            // Process keyboard input for name
            if (nameInputActive) {
                // Find keys that are active but not yet processed
                Set<KeyCode> newlyPressedKeys = new HashSet<>(activeKeys);
                newlyPressedKeys.removeAll(processedKeys);

                // Process each newly pressed key
                for (KeyCode key : newlyPressedKeys) {
                    // Add to processed keys to prevent repeat processing
                    processedKeys.add(key);

                    // Handle backspace
                    if (key == KeyCode.BACK_SPACE && !playerName.isEmpty()) {
                        playerName = playerName.substring(0, playerName.length() - 1);
                        // Play menu click sound
                        soundManager.playSound(PongSoundManager.SoundType.MENU_CLICK);
                    }
                    // Handle enter key to confirm name
                    else if (key == KeyCode.ENTER && !playerName.isEmpty()) {
                        nameInputActive = false;
                        difficultySelectionActive = true; // Activate difficulty selection after name is confirmed
                        // Play menu select sound
                        soundManager.playSound(PongSoundManager.SoundType.MENU_SELECT);
                    }
                    // Handle letter keys and space
                    else if (key.isLetterKey() || key == KeyCode.SPACE) {
                        // Play menu click sound
                        soundManager.playSound(PongSoundManager.SoundType.MENU_CLICK);

                        // Only add space if it's not at the beginning and not consecutive
                        if (key == KeyCode.SPACE) {
                            if (!playerName.isEmpty() && !playerName.endsWith(" ")) {
                                playerName += " ";
                            }
                        } else {
                            // Add the letter (uppercase if shift is pressed)
                            String letter = key.getName();
                            if (!activeKeys.contains(KeyCode.SHIFT)) {
                                letter = letter.toLowerCase();
                            }
                            playerName += letter;
                        }

                        // Limit name length
                        if (playerName.length() > 20) {
                            playerName = playerName.substring(0, 20);
                        }
                    }
                }
            }

            // Handle difficulty selection if active
            if (!nameInputActive && difficultySelectionActive) {
                // Find keys that are active but not yet processed
                Set<KeyCode> newlyPressedKeys = new HashSet<>(activeKeys);
                newlyPressedKeys.removeAll(processedKeys);

                // Process each newly pressed key
                for (KeyCode key : newlyPressedKeys) {
                    // Add to processed keys to prevent repeat processing
                    processedKeys.add(key);

                    // Handle difficulty selection with number keys
                    if (key == KeyCode.DIGIT1 || key == KeyCode.NUMPAD1) {
                        selectedDifficulty = AIDifficulty.EASY;
                        soundManager.playSound(PongSoundManager.SoundType.MENU_CLICK);
                    } else if (key == KeyCode.DIGIT2 || key == KeyCode.NUMPAD2) {
                        selectedDifficulty = AIDifficulty.MEDIUM;
                        soundManager.playSound(PongSoundManager.SoundType.MENU_CLICK);
                    } else if (key == KeyCode.DIGIT3 || key == KeyCode.NUMPAD3) {
                        selectedDifficulty = AIDifficulty.HARD;
                        soundManager.playSound(PongSoundManager.SoundType.MENU_CLICK);
                    } else if (key == KeyCode.ENTER) {
                        // Confirm difficulty selection
                        difficultySelectionActive = false;
                        soundManager.playSound(PongSoundManager.SoundType.MENU_SELECT);
                    }
                }
            }

            // Check for space key to start the game (only if name is entered, confirmed, and difficulty is selected)
            if (!nameInputActive && !difficultySelectionActive && activeKeys.contains(KeyCode.SPACE)) {
                gameState = GameState.PLAYING;
                resetBall(); // Ensure the ball starts from the center
                // Play game start sound
                soundManager.playSound(PongSoundManager.SoundType.GAME_START);
                return;
            }
        } else if (gameState == GameState.GAME_OVER && activeKeys.contains(KeyCode.SPACE)) {
            resetGame(); // Reset the entire game
            gameState = GameState.PLAYING;
            // Play game start sound
            soundManager.playSound(PongSoundManager.SoundType.GAME_START);
            return;
        }

        // Check for sound toggle (M key) in any game state
        if (activeKeys.contains(KeyCode.M) && !processedKeys.contains(KeyCode.M)) {
            processedKeys.add(KeyCode.M);
            boolean soundEnabled = soundManager.toggleSound();
            System.out.println("Sound " + (soundEnabled ? "enabled" : "disabled"));
        }

        // Only update game elements when in PLAYING state
        if (gameState == GameState.PLAYING) {
            // Handle player input
            if (activeKeys.contains(KeyCode.UP)) {
                playerPaddleY = Math.max(0, playerPaddleY - PADDLE_SPEED);
            }
            if (activeKeys.contains(KeyCode.DOWN)) {
                playerPaddleY = Math.min(HEIGHT - PADDLE_HEIGHT, playerPaddleY + PADDLE_SPEED);
            }

            // Update AI paddle (simple following logic)
            double aiPaddleCenter = aiPaddleY + PADDLE_HEIGHT / 2;
            double ballCenter = ballY;

            // Use the selected difficulty's speed multiplier
            double aiSpeedMultiplier = selectedDifficulty.getSpeedMultiplier();

            if (aiPaddleCenter < ballCenter - 10) {
                aiPaddleY += PADDLE_SPEED * aiSpeedMultiplier;
            } else if (aiPaddleCenter > ballCenter + 10) {
                aiPaddleY -= PADDLE_SPEED * aiSpeedMultiplier;
            }

            // Keep AI paddle within bounds
            aiPaddleY = Math.max(0, Math.min(HEIGHT - PADDLE_HEIGHT, aiPaddleY));

            // Update ball position
            ballX += ballSpeedX;
            ballY += ballSpeedY;
            ballZ += ballSpeedZ;

            // Ball collision with top and bottom walls
            if (ballY <= BALL_RADIUS || ballY >= HEIGHT - BALL_RADIUS) {
                ballSpeedY = -ballSpeedY;
                // Adjust ball position to prevent it from going outside the screen
                if (ballY <= BALL_RADIUS) {
                    ballY = BALL_RADIUS;
                } else if (ballY >= HEIGHT - BALL_RADIUS) {
                    ballY = HEIGHT - BALL_RADIUS;
                }
                // Add particle effect for wall collision
                addCollisionParticles(ballX, ballY <= BALL_RADIUS ? BALL_RADIUS : HEIGHT - BALL_RADIUS, ballZ, 10, ACCENT_COLOR);
                // Play wall hit sound
                soundManager.playSound(PongSoundManager.SoundType.WALL_HIT);
            }

            // Ball collision with front and back walls (Z-axis)
            if (ballZ <= -COURT_DEPTH/2 || ballZ >= COURT_DEPTH/2) {
                ballSpeedZ = -ballSpeedZ;
                // Adjust ball position to prevent it from going outside the court depth
                if (ballZ <= -COURT_DEPTH/2) {
                    ballZ = -COURT_DEPTH/2;
                } else if (ballZ >= COURT_DEPTH/2) {
                    ballZ = COURT_DEPTH/2;
                }
                // Add particle effect for wall collision
                addCollisionParticles(ballX, ballY, ballZ, 10, ACCENT_COLOR_ALT);
                // Play wall hit sound
                soundManager.playSound(PongSoundManager.SoundType.WALL_HIT);
            }

            // Ball collision with paddles using trajectory-based detection
            // Store previous ball position for trajectory calculation
            double prevBallX = ballX - ballSpeedX;
            double prevBallY = ballY - ballSpeedY;

            // Player paddle (left) - trajectory-based collision detection
            if (ballSpeedX < 0) { // Only check if ball is moving towards player paddle
                // Check if ball crossed the paddle boundary between frames
                if (prevBallX > PADDLE_WIDTH && ballX <= PADDLE_WIDTH) {
                    // Calculate Y position at the moment of intersection with paddle plane
                    double intersectY = prevBallY + (ballY - prevBallY) * 
                                       ((PADDLE_WIDTH - prevBallX) / (ballX - prevBallX));

                    // Check if intersection point is within paddle height (with a small buffer)
                    if (intersectY >= playerPaddleY - BALL_RADIUS && 
                        intersectY <= playerPaddleY + PADDLE_HEIGHT + BALL_RADIUS) {

                        // Adjust ball position to prevent it from going through the paddle
                        ballX = PADDLE_WIDTH;

                        // Reverse X direction and increase speed slightly (with a maximum limit)
                        ballSpeedX = -ballSpeedX * 1.05;
                        // Limit maximum speed to prevent the ball from moving too fast
                        double maxSpeed = INITIAL_BALL_SPEED * 2.5;
                        if (Math.abs(ballSpeedX) > maxSpeed) {
                            ballSpeedX = Math.signum(ballSpeedX) * maxSpeed;
                        }

                        // Add some angle based on where the ball hits the paddle
                        double relativeIntersectY = (playerPaddleY + (PADDLE_HEIGHT / 2)) - intersectY;
                        double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                        ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;
                        // Limit maximum Y speed to prevent the ball from moving too fast vertically
                        double maxYSpeed = INITIAL_BALL_SPEED * 1.5;
                        if (Math.abs(ballSpeedY) > maxYSpeed) {
                            ballSpeedY = Math.signum(ballSpeedY) * maxYSpeed;
                        }

                        // Add some Z movement based on paddle hit position
                        ballSpeedZ = normalizedRelativeIntersectionY * INITIAL_BALL_SPEED * 0.5;
                        // Limit maximum Z speed to prevent the ball from moving too fast in depth
                        double maxZSpeed = INITIAL_BALL_SPEED * 0.8;
                        if (Math.abs(ballSpeedZ) > maxZSpeed) {
                            ballSpeedZ = Math.signum(ballSpeedZ) * maxZSpeed;
                        }

                        // Add particle effect for paddle hit
                        addCollisionParticles(PADDLE_WIDTH, intersectY, ballZ, 15, PLAYER_PADDLE_COLOR);

                        // Play paddle hit sound
                        soundManager.playSound(PongSoundManager.SoundType.PADDLE_HIT);
                    }
                }
            }

            // AI paddle (right) - trajectory-based collision detection
            if (ballSpeedX > 0) { // Only check if ball is moving towards AI paddle
                // Check if ball crossed the paddle boundary between frames
                double aiPaddleX = WIDTH - PADDLE_WIDTH;
                if (prevBallX < aiPaddleX && ballX >= aiPaddleX - BALL_RADIUS) {
                    // Calculate Y position at the moment of intersection with paddle plane
                    double intersectY = prevBallY + (ballY - prevBallY) * 
                                       ((aiPaddleX - BALL_RADIUS - prevBallX) / (ballX - prevBallX));

                    // Check if intersection point is within paddle height (with a small buffer)
                    if (intersectY >= aiPaddleY - BALL_RADIUS && 
                        intersectY <= aiPaddleY + PADDLE_HEIGHT + BALL_RADIUS) {

                        // Adjust ball position to prevent it from going through the paddle
                        ballX = aiPaddleX - BALL_RADIUS;

                        // Reverse X direction and increase speed slightly (with a maximum limit)
                        ballSpeedX = -ballSpeedX * 1.05;
                        // Limit maximum speed to prevent the ball from moving too fast
                        double maxSpeed = INITIAL_BALL_SPEED * 2.5;
                        if (Math.abs(ballSpeedX) > maxSpeed) {
                            ballSpeedX = Math.signum(ballSpeedX) * maxSpeed;
                        }

                        // Add some angle based on where the ball hits the paddle
                        double relativeIntersectY = (aiPaddleY + (PADDLE_HEIGHT / 2)) - intersectY;
                        double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                        ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;
                        // Limit maximum Y speed to prevent the ball from moving too fast vertically
                        double maxYSpeed = INITIAL_BALL_SPEED * 1.5;
                        if (Math.abs(ballSpeedY) > maxYSpeed) {
                            ballSpeedY = Math.signum(ballSpeedY) * maxYSpeed;
                        }

                        // Add some Z movement based on paddle hit position
                        ballSpeedZ = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED * 0.5;
                        // Limit maximum Z speed to prevent the ball from moving too fast in depth
                        double maxZSpeed = INITIAL_BALL_SPEED * 0.8;
                        if (Math.abs(ballSpeedZ) > maxZSpeed) {
                            ballSpeedZ = Math.signum(ballSpeedZ) * maxZSpeed;
                        }

                        // Add particle effect for paddle hit
                        addCollisionParticles(WIDTH - PADDLE_WIDTH, intersectY, ballZ, 15, AI_PADDLE_COLOR);

                        // Play paddle hit sound
                        soundManager.playSound(PongSoundManager.SoundType.PADDLE_HIT);
                    }
                }
            }

            // Ensure ball stays within the screen boundaries
            if (ballX < BALL_RADIUS) {
                // Ball hit left boundary (scoring)
                // AI scores
                aiScore++;
                // Play score sound
                soundManager.playSound(PongSoundManager.SoundType.SCORE);

                // Check if AI has reached 5 points
                if (aiScore >= 5) {
                    gameState = GameState.GAME_OVER;
                    // Play game end sound
                    soundManager.playSound(PongSoundManager.SoundType.GAME_END);
                } else {
                    resetBall();
                }
            } else if (ballX > WIDTH - BALL_RADIUS) {
                // Ball hit right boundary (scoring)
                // Player scores
                playerScore++;
                // Play score sound
                soundManager.playSound(PongSoundManager.SoundType.SCORE);

                // Check if player has reached 5 points
                if (playerScore >= 5) {
                    gameState = GameState.GAME_OVER;
                    // Play game end sound
                    soundManager.playSound(PongSoundManager.SoundType.GAME_END);
                } else {
                    resetBall();
                }
            }
        }
    }

    private void resetBall() {
        // Reset ball position to center
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballZ = 0; // Reset Z position

        // Determine which paddle the ball should move towards (randomly choose player or AI)
        boolean towardsPlayer = Math.random() > 0.5;

        // Set initial X speed with a small random variation (±20%)
        // Direction is always towards a paddle (negative for player, positive for AI)
        double speedVariation = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
        ballSpeedX = (towardsPlayer ? -1 : 1) * INITIAL_BALL_SPEED * speedVariation;

        // Set initial Y speed to 0 to ensure horizontal movement
        ballSpeedY = 0;

        // Start with a very small Z movement for more interesting gameplay
        ballSpeedZ = (Math.random() - 0.5) * INITIAL_BALL_SPEED * 0.2; // Small random Z speed

        // Ensure the ball is not moving too slowly in the X direction
        if (Math.abs(ballSpeedX) < INITIAL_BALL_SPEED * 0.8) {
            ballSpeedX = Math.signum(ballSpeedX) * INITIAL_BALL_SPEED * 0.8;
        }
    }

    private void resetGame() {
        // Reset scores
        playerScore = 0;
        aiScore = 0;

        // Reset paddle positions
        playerPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
        aiPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;

        // Reset ball
        resetBall();

        // Reset game state to welcome screen to allow entering a new name
        gameState = GameState.WELCOME_SCREEN;
        nameInputActive = true;
        playerName = "";
        showCursor = true;
        lastCursorBlinkTime = System.currentTimeMillis();

        // Reset difficulty selection (keep the selected difficulty)
        difficultySelectionActive = false;

        // Clear processed keys to ensure fresh input handling
        processedKeys.clear();

        // Clear particles
        particles.clear();
    }

    // Particle system methods
    private void updateParticles() {
        // Update all particles
        particles.forEach(Particle::update);

        // Remove dead particles
        particles.removeIf(Particle::isDead);
    }

    private void addParticles(double x, double y, double z, int count, Color color) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, z, color));
        }
    }

    private void addCollisionParticles(double x, double y, double z, int count, Color color) {
        // Add more particles with higher velocity for collisions
        for (int i = 0; i < count; i++) {
            Particle p = new Particle(x, y, z, color);
            // Make collision particles move faster
            p.speedX *= 2;
            p.speedY *= 2;
            p.speedZ *= 2;
            particles.add(p);
        }
    }

    private void render(GraphicsContext gc) {
        // Clear the canvas with gradient background
        Paint originalFill = gc.getFill();

        // Create a gradient background
        LinearGradient backgroundGradient = new LinearGradient(
            0, 0, 0, HEIGHT,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, BACKGROUND_COLOR_1),
            new Stop(1, BACKGROUND_COLOR_2)
        );

        gc.setFill(backgroundGradient);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Render the appropriate screen based on game state
        if (gameState == GameState.WELCOME_SCREEN) {
            // Render welcome screen
            renderWelcomeScreen(gc);
        } else if (gameState == GameState.GAME_OVER) {
            // Render game over screen
            renderGameOver(gc);
        } else {
            // Render game
            renderGame(gc);
        }

        // Restore original fill
        gc.setFill(originalFill);
    }

    private void renderGameOver(GraphicsContext gc) {
        // First render the game in the background
        renderGame(gc);

        // Create a semi-transparent overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw sound status indicator (on top of overlay)
        String soundStatus = soundManager.isSoundEnabled() ? "🔊" : "🔇";
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(Color.WHITE);
        gc.fillText(soundStatus, WIDTH - 30, 30);
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 14));
        gc.fillText("M", WIDTH - 30, 50);

        // Set text properties with shadow effect
        gc.setTextAlign(TextAlignment.CENTER);

        // Draw game over title with glow effect
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 72));
        gc.setEffect(glowEffect);
        gc.setFill(ACCENT_COLOR);
        gc.fillText("GAME OVER", WIDTH / 2, HEIGHT / 4);
        gc.setEffect(null);

        // Draw a decorative line
        double lineWidth = 400;
        gc.setStroke(ACCENT_COLOR);
        gc.setLineWidth(3);
        gc.strokeLine(WIDTH / 2 - lineWidth / 2, HEIGHT / 3 - 10, WIDTH / 2 + lineWidth / 2, HEIGHT / 3 - 10);

        // Draw final score with shadow
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 48));
        gc.setEffect(textShadow);
        gc.setFill(TEXT_COLOR);
        gc.fillText("Score final", WIDTH / 2, HEIGHT / 3 + 50);

        // Draw score with different colors for player and AI
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 64));
        gc.setFill(PLAYER_PADDLE_COLOR);
        gc.fillText(Integer.toString(playerScore), WIDTH / 2 - 50, HEIGHT / 3 + 130);

        gc.setFill(TEXT_COLOR);
        gc.fillText("-", WIDTH / 2, HEIGHT / 3 + 130);

        gc.setFill(AI_PADDLE_COLOR);
        gc.fillText(Integer.toString(aiScore), WIDTH / 2 + 50, HEIGHT / 3 + 130);
        gc.setEffect(null);

        // Draw winner with appropriate color
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 36));
        String winner = playerScore >= 5 ? playerName : "Ordinateur";
        gc.setFill(playerScore >= 5 ? PLAYER_PADDLE_COLOR : AI_PADDLE_COLOR);
        gc.setEffect(dropShadow);
        gc.fillText("Gagnant: " + winner, WIDTH / 2, HEIGHT / 2 + 50);
        gc.setEffect(null);

        // Show difficulty level
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 24));
        gc.setFill(Color.rgb(200, 200, 200));
        String difficultyText = "";
        switch (selectedDifficulty) {
            case EASY:
                difficultyText = "Niveau: Facile";
                break;
            case MEDIUM:
                difficultyText = "Niveau: Moyen";
                break;
            case HARD:
                difficultyText = "Niveau: Difficile";
                break;
        }
        gc.fillText(difficultyText, WIDTH / 2, HEIGHT / 2 + 90);

        // Draw restart button
        double buttonWidth = 400;
        double buttonHeight = 60;
        double buttonX = WIDTH / 2 - buttonWidth / 2;
        double buttonY = HEIGHT * 3 / 4;

        // Button background with gradient
        LinearGradient buttonGradient = new LinearGradient(
            0, buttonY, 0, buttonY + buttonHeight,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(60, 60, 80)),
            new Stop(1, Color.rgb(40, 40, 60))
        );

        gc.setFill(buttonGradient);
        gc.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

        // Button border
        gc.setStroke(ACCENT_COLOR);
        gc.setLineWidth(2);
        gc.strokeRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

        // Button text
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 24));
        gc.fillText("Appuyez sur ESPACE pour rejouer", WIDTH / 2, buttonY + buttonHeight / 2 + 8);

        // Add some decorative particles
        long currentTime = System.currentTimeMillis();
        if (currentTime % 200 < 20) {  // Occasionally add particles
            addParticles(WIDTH / 2 + (Math.random() - 0.5) * WIDTH * 0.8, 
                         HEIGHT / 2 + (Math.random() - 0.5) * HEIGHT * 0.8,
                         (Math.random() - 0.5) * COURT_DEPTH,
                         5, 
                         Math.random() > 0.5 ? ACCENT_COLOR : ACCENT_COLOR_ALT);
        }
    }

    private void renderWelcomeScreen(GraphicsContext gc) {
        // Render a 3D court in the background
        renderCourt(gc);

        // Draw sound status indicator
        String soundStatus = soundManager.isSoundEnabled() ? "🔊" : "🔇";
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(Color.WHITE);
        gc.fillText(soundStatus, WIDTH - 30, 30);
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 14));
        gc.fillText("M", WIDTH - 30, 50);

        // Add a subtle pulsing effect to the background
        long currentTime = System.currentTimeMillis();
        double pulseIntensity = 0.05 * Math.sin(currentTime / 1000.0) + 0.95;

        // Add some ambient particles
        if (currentTime % 100 < 20) {
            addParticles(Math.random() * WIDTH, 
                        Math.random() * HEIGHT, 
                        (Math.random() - 0.5) * COURT_DEPTH,
                        2, 
                        Math.random() > 0.5 ? ACCENT_COLOR : ACCENT_COLOR_ALT);
        }

        // Set text properties
        gc.setTextAlign(TextAlignment.CENTER);

        // Draw title with glow and reflection effect
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 90));
        gc.setEffect(glowEffect);
        gc.setFill(ACCENT_COLOR);

        // Apply a pulsing effect to the title
        gc.setGlobalAlpha(pulseIntensity);
        gc.fillText("PONG", WIDTH / 2, HEIGHT / 5);
        gc.setGlobalAlpha(1.0);
        gc.setEffect(null);

        // Add a subtitle
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 24));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("2.5D EDITION", WIDTH / 2, HEIGHT / 5 + 40);

        // Draw a decorative line
        double lineWidth = 500;
        gc.setStroke(ACCENT_COLOR);
        gc.setLineWidth(3);
        gc.strokeLine(WIDTH / 2 - lineWidth / 2, HEIGHT / 4 + 20, WIDTH / 2 + lineWidth / 2, HEIGHT / 4 + 20);

        // Draw name input section with modern styling
        if (nameInputActive) {
            // Name input prompt
            gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 28));
            gc.setFill(TEXT_COLOR);
            gc.setEffect(textShadow);
            gc.fillText("Entrez votre nom:", WIDTH / 2, HEIGHT / 3 + 20);
            gc.setEffect(null);

            // Draw name input box with modern styling
            double boxWidth = 400;
            double boxHeight = 50;
            double boxX = WIDTH / 2 - boxWidth / 2;
            double boxY = HEIGHT / 3 + 40;

            // Draw box with gradient background
            LinearGradient inputBoxGradient = new LinearGradient(
                0, boxY, 0, boxY + boxHeight,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(40, 40, 60)),
                new Stop(1, Color.rgb(30, 30, 50))
            );

            gc.setFill(inputBoxGradient);
            gc.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);

            // Draw box border with glow
            gc.setEffect(new DropShadow(BlurType.GAUSSIAN, ACCENT_COLOR, 5, 0.5, 0, 0));
            gc.setStroke(ACCENT_COLOR);
            gc.setLineWidth(2);
            gc.strokeRoundRect(boxX, boxY, boxWidth, boxHeight, 10, 10);
            gc.setEffect(null);

            // Draw entered name with cursor
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 24));
            gc.setFill(TEXT_COLOR);
            String displayText = playerName;
            if (showCursor) {
                displayText += "|"; // Add blinking cursor
            }
            gc.fillText(displayText, boxX + 15, boxY + 33);
            gc.setTextAlign(TextAlignment.CENTER); // Reset alignment

            // Draw instruction for confirming name
            gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 18));
            gc.setFill(Color.rgb(180, 180, 180));
            gc.fillText("Appuyez sur ENTRÉE pour confirmer", WIDTH / 2, boxY + boxHeight + 30);
        } else {
            // Show confirmed name with welcome message
            gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 32));
            gc.setFill(ACCENT_COLOR);
            gc.setEffect(textShadow);
            gc.fillText("Bienvenue, " + playerName + "!", WIDTH / 2, HEIGHT / 3 + 20);
            gc.setEffect(null);

            if (difficultySelectionActive) {
                // Show difficulty selection UI
                gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 28));
                gc.setFill(TEXT_COLOR);
                gc.fillText("Choisissez le niveau de l'IA:", WIDTH / 2, HEIGHT / 3 + 70);

                // Draw difficulty buttons
                double buttonSpacing = 20;
                double buttonWidth = 200;
                double buttonHeight = 60;
                double totalWidth = (buttonWidth * 3) + (buttonSpacing * 2);
                double startX = WIDTH / 2 - totalWidth / 2;
                double buttonY = HEIGHT / 3 + 90;

                // Draw each difficulty button
                for (int i = 0; i < AIDifficulty.values().length; i++) {
                    AIDifficulty difficulty = AIDifficulty.values()[i];
                    double buttonX = startX + (buttonWidth + buttonSpacing) * i;

                    // Button background with gradient
                    LinearGradient buttonGradient = new LinearGradient(
                        0, buttonY, 0, buttonY + buttonHeight,
                        false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(60, 60, 80)),
                        new Stop(1, Color.rgb(40, 40, 60))
                    );

                    gc.setFill(buttonGradient);
                    gc.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

                    // Highlight selected difficulty
                    if (difficulty == selectedDifficulty) {
                        gc.setStroke(ACCENT_COLOR);
                        gc.setLineWidth(3);
                    } else {
                        gc.setStroke(Color.rgb(100, 100, 150, 0.5));
                        gc.setLineWidth(1);
                    }
                    gc.strokeRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

                    // Button text
                    gc.setFill(TEXT_COLOR);
                    gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 22));

                    // Display difficulty name and key
                    String difficultyText = "";
                    switch (difficulty) {
                        case EASY:
                            difficultyText = "1 - Facile";
                            break;
                        case MEDIUM:
                            difficultyText = "2 - Moyen";
                            break;
                        case HARD:
                            difficultyText = "3 - Difficile";
                            break;
                    }
                    gc.fillText(difficultyText, buttonX + buttonWidth / 2, buttonY + buttonHeight / 2 + 8);
                }

                // Instructions for confirming selection
                gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 18));
                gc.setFill(Color.rgb(180, 180, 180));
                gc.fillText("Appuyez sur ENTRÉE pour confirmer", WIDTH / 2, buttonY + buttonHeight + 30);

            } else {
                // Draw start button
                double buttonWidth = 350;
                double buttonHeight = 60;
                double buttonX = WIDTH / 2 - buttonWidth / 2;
                double buttonY = HEIGHT / 3 + 50;

                // Button background with gradient
                LinearGradient buttonGradient = new LinearGradient(
                    0, buttonY, 0, buttonY + buttonHeight,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(60, 60, 80)),
                    new Stop(1, Color.rgb(40, 40, 60))
                );

                gc.setFill(buttonGradient);
                gc.fillRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

                // Button border with pulsing effect
                gc.setStroke(ACCENT_COLOR);
                gc.setLineWidth(2 * pulseIntensity);
                gc.strokeRoundRect(buttonX, buttonY, buttonWidth, buttonHeight, 10, 10);

                // Button text
                gc.setFill(TEXT_COLOR);
                gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 22));
                gc.fillText("Appuyez sur ESPACE pour commencer", WIDTH / 2, buttonY + buttonHeight / 2 + 8);

                // Show selected difficulty
                gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 18));
                gc.setFill(Color.rgb(180, 180, 180));
                String difficultyText = "";
                switch (selectedDifficulty) {
                    case EASY:
                        difficultyText = "Niveau: Facile";
                        break;
                    case MEDIUM:
                        difficultyText = "Niveau: Moyen";
                        break;
                    case HARD:
                        difficultyText = "Niveau: Difficile";
                        break;
                }
                gc.fillText(difficultyText, WIDTH / 2, buttonY + buttonHeight + 30);
            }
        }

        // Draw instructions panel
        double panelWidth = 600;
        double panelHeight = 320; // Increased height to accommodate additional text
        double panelX = WIDTH / 2 - panelWidth / 2;
        double panelY = HEIGHT / 2 + 30;

        // Panel background
        gc.setFill(Color.rgb(20, 20, 40, 0.7));
        gc.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);

        // Panel border
        gc.setStroke(Color.rgb(100, 100, 150, 0.5));
        gc.setLineWidth(1);
        gc.strokeRoundRect(panelX, panelY, panelWidth, panelHeight, 15, 15);

        // Panel title
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 28));
        gc.setFill(ACCENT_COLOR);
        gc.fillText("Contrôles & Règles", WIDTH / 2, panelY + 35);

        // Controls section
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(TEXT_COLOR);
        gc.fillText("Contrôles:", WIDTH / 2, panelY + 75);

        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 18));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("Flèche HAUT - Déplacer la raquette vers le haut", WIDTH / 2, panelY + 105);
        gc.fillText("Flèche BAS - Déplacer la raquette vers le bas", WIDTH / 2, panelY + 135);
        gc.fillText("Touche M - Activer/désactiver le son", WIDTH / 2, panelY + 165);

        // Rules section
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(TEXT_COLOR);
        gc.fillText("Règles:", WIDTH / 2, panelY + 195);

        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 16));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("Renvoyez la balle avec votre raquette pour marquer des points.", WIDTH / 2, panelY + 225);
        gc.fillText("Ne laissez pas la balle passer derrière votre raquette!", WIDTH / 2, panelY + 250);
        gc.fillText("Le premier à atteindre 5 points gagne la partie.", WIDTH / 2, panelY + 275);

        // Draw 3D paddles
        renderPaddle(gc, 0, playerPaddleY, PLAYER_PADDLE_COLOR, true);
        renderPaddle(gc, WIDTH - PADDLE_WIDTH, aiPaddleY, AI_PADDLE_COLOR, false);
    }

    private void renderGame(GraphicsContext gc) {
        // Render the 3D court
        renderCourt(gc);

        // Render particles
        particles.forEach(p -> p.render(gc));

        // Draw the paddles with 3D effect
        renderPaddle(gc, 0, playerPaddleY, PLAYER_PADDLE_COLOR, true);
        renderPaddle(gc, WIDTH - PADDLE_WIDTH, aiPaddleY, AI_PADDLE_COLOR, false);

        // Draw the ball with 3D effect
        renderBall(gc);

        // Draw the scores with modern styling
        gc.setTextAlign(TextAlignment.CENTER);

        // Player score
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 60));
        gc.setEffect(textShadow);
        gc.setFill(PLAYER_PADDLE_COLOR);
        gc.fillText(Integer.toString(playerScore), WIDTH / 4, 80);

        // AI score
        gc.setFill(AI_PADDLE_COLOR);
        gc.fillText(Integer.toString(aiScore), WIDTH * 3 / 4, 80);
        gc.setEffect(null);

        // Draw player names
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 20));
        gc.setFill(Color.rgb(200, 200, 200, 0.7));
        gc.fillText(playerName, WIDTH / 4, 110);
        gc.fillText("Ordinateur", WIDTH * 3 / 4, 110);

        // Draw sound status indicator
        String soundStatus = soundManager.isSoundEnabled() ? "🔊" : "🔇";
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(Color.WHITE);
        gc.fillText(soundStatus, WIDTH - 30, 30);
        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 14));
        gc.fillText("M", WIDTH - 30, 50);
    }

    // Helper method to render the 3D court
    private void renderCourt(GraphicsContext gc) {
        // Draw the floor with perspective
        double floorWidth = WIDTH;
        double floorDepth = HEIGHT * 0.3;

        // Floor polygon points (trapezoid for perspective)
        double[] floorX = {
            0,                  // Bottom left
            WIDTH,              // Bottom right
            WIDTH * 0.8,        // Top right
            WIDTH * 0.2         // Top left
        };

        double[] floorY = {
            HEIGHT,             // Bottom left
            HEIGHT,             // Bottom right
            HEIGHT - floorDepth, // Top right
            HEIGHT - floorDepth  // Top left
        };

        // Draw floor with gradient
        LinearGradient floorGradient = new LinearGradient(
            0, HEIGHT - floorDepth, 0, HEIGHT,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(30, 35, 60)),
            new Stop(1, Color.rgb(15, 20, 40))
        );

        gc.setFill(floorGradient);
        gc.fillPolygon(floorX, floorY, 4);

        // Draw court lines with perspective
        double courtLeft = WIDTH * 0.2;
        double courtRight = WIDTH * 0.8;
        double courtTop = HEIGHT * 0.2;
        double courtBottom = HEIGHT * 0.7;

        // Court outline
        gc.setStroke(COURT_LINE_COLOR);
        gc.setLineWidth(2);

        // Draw court outline
        double[] courtX = {
            courtLeft,
            courtRight,
            courtRight,
            courtLeft
        };

        double[] courtY = {
            courtTop,
            courtTop,
            courtBottom,
            courtBottom
        };

        gc.strokePolygon(courtX, courtY, 4);

        // Draw center line
        gc.setLineDashes(10);
        gc.strokeLine(WIDTH/2, courtTop, WIDTH/2, courtBottom);
        gc.setLineDashes(0);

        // Draw grid lines for floor with perspective
        gc.setStroke(Color.rgb(255, 255, 255, 0.1));
        gc.setLineWidth(1);

        // Horizontal grid lines
        for (int i = 1; i < 10; i++) {
            double y = HEIGHT - (i * floorDepth / 10);
            double leftX = WIDTH * 0.2 + (i * (WIDTH * 0.6) / 10);
            double rightX = WIDTH * 0.8 - (i * (WIDTH * 0.6) / 10);
            gc.strokeLine(leftX, y, rightX, y);
        }

        // Vertical grid lines
        for (int i = 1; i < 10; i++) {
            double x = WIDTH * 0.2 + (i * (WIDTH * 0.6) / 10);
            double topY = HEIGHT - floorDepth;
            double perspectiveX = WIDTH * 0.2 + (i * (WIDTH * 0.6) / 10);
            gc.strokeLine(x, HEIGHT, perspectiveX, topY);
        }
    }

    // Helper method to render a 3D paddle
    private void renderPaddle(GraphicsContext gc, double x, double y, Color color, boolean isPlayer) {
        // Apply lighting effect to create 3D look
        gc.setEffect(lighting);

        // Main paddle face
        gc.setFill(color);
        gc.fillRoundRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        // Side of paddle (for 3D effect)
        gc.setFill(color.darker());

        if (isPlayer) {
            // Right side for player paddle
            gc.fillRect(x + PADDLE_WIDTH, y + 5, PADDLE_DEPTH/2, PADDLE_HEIGHT - 10);
        } else {
            // Left side for AI paddle
            gc.fillRect(x - PADDLE_DEPTH/2, y + 5, PADDLE_DEPTH/2, PADDLE_HEIGHT - 10);
        }

        // Top of paddle
        gc.fillRect(x, y - PADDLE_DEPTH/3, PADDLE_WIDTH, PADDLE_DEPTH/3);

        // Bottom of paddle
        gc.fillRect(x, y + PADDLE_HEIGHT, PADDLE_WIDTH, PADDLE_DEPTH/3);

        gc.setEffect(null);

        // Add glow effect
        gc.setEffect(glowEffect);
        gc.setStroke(color.brighter());
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);
        gc.setEffect(null);
    }

    // Helper method to render a 3D ball
    private void renderBall(GraphicsContext gc) {
        // Project ball position based on Z coordinate
        Point2D projectedBall = project3DTo2D(ballX, ballY, ballZ);
        double projectedX = projectedBall.getX();
        double projectedY = projectedBall.getY();

        // Calculate ball size based on Z position (perspective)
        double perspectiveScale = 1 - (ballZ / (COURT_DEPTH * 2));
        double ballSize = BALL_RADIUS * 2 * perspectiveScale;

        // Draw ball shadow on the floor
        double shadowY = FLOOR_Y - (FLOOR_Y - projectedY) * 0.2; // Shadow is below the ball
        double shadowScale = 0.7 * perspectiveScale; // Shadow is smaller and depends on height
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(projectedX - ballSize/2 * shadowScale, 
                   shadowY - ballSize/4 * shadowScale, 
                   ballSize * shadowScale, ballSize/2 * shadowScale);

        // Draw the ball with lighting effect for 3D appearance
        gc.setEffect(lighting);
        gc.setFill(BALL_COLOR);
        gc.fillOval(projectedX - ballSize/2, projectedY - ballSize/2, ballSize, ballSize);
        gc.setEffect(null);

        // Add glow effect
        gc.setEffect(glowEffect);
        gc.setStroke(BALL_COLOR);
        gc.setLineWidth(1);
        gc.strokeOval(projectedX - ballSize/2, projectedY - ballSize/2, ballSize, ballSize);
        gc.setEffect(null);

        // Add highlight to create 3D sphere effect
        gc.setFill(Color.rgb(255, 255, 255, 0.7));
        gc.fillOval(projectedX - ballSize/4, projectedY - ballSize/4, ballSize/6, ballSize/6);
    }

    // Helper method to create lighting effect
    private Lighting createLighting() {
        Light.Distant light = new Light.Distant();
        light.setAzimuth(-135.0); // Light coming from top-left
        light.setElevation(30.0);
        light.setColor(Color.WHITE);

        Lighting lighting = new Lighting();
        lighting.setLight(light);
        lighting.setSurfaceScale(5.0);

        return lighting;
    }

    // Helper class for 2D points
    private static class Point2D {
        private final double x;
        private final double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    // Helper method to project 3D coordinates to 2D screen coordinates
    private Point2D project3DTo2D(double x, double y, double z) {
        // Simple perspective projection
        double centerX = WIDTH / 2;
        double centerY = HEIGHT / 2;
        double perspectiveFactor = 1200; // Controls the strength of perspective effect

        // Calculate perspective scaling
        double scale = perspectiveFactor / (perspectiveFactor + z);

        // Apply perspective transformation
        double projectedX = centerX + (x - centerX) * scale;
        double projectedY = centerY + (y - centerY) * scale;

        return new Point2D(projectedX, projectedY);
    }

    @Override
    public void stop() {
        // Clean up sound resources when the application is closing
        if (soundManager != null) {
            soundManager.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
