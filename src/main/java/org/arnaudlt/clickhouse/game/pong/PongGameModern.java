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
import javafx.scene.effect.Reflection;
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

public class PongGameModern extends Application {

    // Game states
    private enum GameState {
        WELCOME_SCREEN,
        PLAYING,
        GAME_OVER
    }

    // Game constants
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 900;
    private static final int PADDLE_WIDTH = 20;
    private static final int PADDLE_HEIGHT = 150;
    private static final int BALL_RADIUS = 15;
    private static final double PADDLE_SPEED = 7.5;
    private static final double INITIAL_BALL_SPEED = 6.0;

    // Visual design constants
    private static final Color BACKGROUND_COLOR_1 = Color.rgb(10, 15, 30);
    private static final Color BACKGROUND_COLOR_2 = Color.rgb(30, 40, 70);
    private static final Color ACCENT_COLOR = Color.rgb(0, 200, 255);
    private static final Color ACCENT_COLOR_ALT = Color.rgb(255, 50, 100);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color PLAYER_PADDLE_COLOR = Color.rgb(0, 200, 255);
    private static final Color AI_PADDLE_COLOR = Color.rgb(255, 50, 100);
    private static final Color BALL_COLOR = Color.WHITE;

    // Effects
    private final Glow glowEffect = new Glow(0.8);
    private final DropShadow dropShadow = new DropShadow(BlurType.GAUSSIAN, ACCENT_COLOR, 15, 0.7, 0, 0);
    private final DropShadow textShadow = new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 3, 0.7, 2, 2);

    // Fonts
    private static final String FONT_NAME = "Verdana";

    // Animation variables
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private long lastParticleTime = 0;

    // Particle class for visual effects
    private class Particle {
        double x, y;
        double speedX, speedY;
        double size;
        double opacity;
        Color color;

        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.speedX = (random.nextDouble() - 0.5) * 3;
            this.speedY = (random.nextDouble() - 0.5) * 3;
            this.size = random.nextDouble() * 5 + 2;
            this.opacity = 1.0;
            this.color = color;
        }

        void update() {
            x += speedX;
            y += speedY;
            opacity -= 0.02;
            size -= 0.1;
        }

        boolean isDead() {
            return opacity <= 0 || size <= 0;
        }

        void render(GraphicsContext gc) {
            if (!isDead()) {
                gc.setGlobalAlpha(opacity);
                gc.setFill(color);
                gc.fillOval(x - size/2, y - size/2, size, size);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    // Game variables
    private double playerPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double aiPaddleY = HEIGHT / 2 - PADDLE_HEIGHT / 2;
    private double ballX = WIDTH / 2;
    private double ballY = HEIGHT / 2;
    private double ballSpeedX = INITIAL_BALL_SPEED;
    private double ballSpeedY = INITIAL_BALL_SPEED;
    private int playerScore = 0;
    private int aiScore = 0;
    private GameState gameState = GameState.WELCOME_SCREEN;

    // Player name variables
    private String playerName = "";
    private boolean nameInputActive = true; // Start with name input active
    private long lastCursorBlinkTime = 0;
    private boolean showCursor = true;

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
        primaryStage.setTitle("PONG - Modern Edition");
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
        // Update particles
        updateParticles();

        // Generate particles for ball trail if in PLAYING state
        if (gameState == GameState.PLAYING) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastParticleTime > 50) { // Generate particles every 50ms
                addParticles(ballX, ballY, 2, BALL_COLOR);
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
                    }
                    // Handle enter key to confirm name
                    else if (key == KeyCode.ENTER && !playerName.isEmpty()) {
                        nameInputActive = false;
                    }
                    // Handle letter keys and space
                    else if (key.isLetterKey() || key == KeyCode.SPACE) {
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

            // Check for space key to start the game (only if name is entered and confirmed)
            if (!nameInputActive && activeKeys.contains(KeyCode.SPACE)) {
                gameState = GameState.PLAYING;
                resetBall(); // Ensure the ball starts from the center
                return;
            }
        } else if (gameState == GameState.GAME_OVER && activeKeys.contains(KeyCode.SPACE)) {
            resetGame(); // Reset the entire game
            gameState = GameState.PLAYING;
            return;
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

            if (aiPaddleCenter < ballCenter - 10) {
                aiPaddleY += PADDLE_SPEED * 0.7; // AI is slightly slower than player
            } else if (aiPaddleCenter > ballCenter + 10) {
                aiPaddleY -= PADDLE_SPEED * 0.7;
            }

            // Keep AI paddle within bounds
            aiPaddleY = Math.max(0, Math.min(HEIGHT - PADDLE_HEIGHT, aiPaddleY));

            // Update ball position
            ballX += ballSpeedX;
            ballY += ballSpeedY;

            // Ball collision with top and bottom walls
            if (ballY <= 0 || ballY >= HEIGHT) {
                ballSpeedY = -ballSpeedY;
                // Add particle effect for wall collision
                addCollisionParticles(ballX, ballY <= 0 ? 0 : HEIGHT, 10, ACCENT_COLOR);
            }

            // Ball collision with paddles
            // Player paddle (left)
            if (ballX <= PADDLE_WIDTH && ballY >= playerPaddleY && ballY <= playerPaddleY + PADDLE_HEIGHT) {
                ballSpeedX = -ballSpeedX * 1.05; // Increase speed slightly on paddle hit
                // Add some angle based on where the ball hits the paddle
                double relativeIntersectY = (playerPaddleY + (PADDLE_HEIGHT / 2)) - ballY;
                double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;

                // Add particle effect for paddle hit
                addCollisionParticles(PADDLE_WIDTH, ballY, 15, PLAYER_PADDLE_COLOR);
            }

            // AI paddle (right)
            if (ballX >= WIDTH - PADDLE_WIDTH - BALL_RADIUS && ballY >= aiPaddleY && ballY <= aiPaddleY + PADDLE_HEIGHT) {
                ballSpeedX = -ballSpeedX * 1.05; // Increase speed slightly on paddle hit
                // Add some angle based on where the ball hits the paddle
                double relativeIntersectY = (aiPaddleY + (PADDLE_HEIGHT / 2)) - ballY;
                double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;

                // Add particle effect for paddle hit
                addCollisionParticles(WIDTH - PADDLE_WIDTH, ballY, 15, AI_PADDLE_COLOR);
            }

            // Ball out of bounds (scoring)
            if (ballX < 0) {
                // AI scores
                aiScore++;
                // Check if AI has reached 5 points
                if (aiScore >= 5) {
                    gameState = GameState.GAME_OVER;
                } else {
                    resetBall();
                }
            } else if (ballX > WIDTH) {
                // Player scores
                playerScore++;
                // Check if player has reached 5 points
                if (playerScore >= 5) {
                    gameState = GameState.GAME_OVER;
                } else {
                    resetBall();
                }
            }
        }
    }

    private void resetBall() {
        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballSpeedX = Math.random() > 0.5 ? INITIAL_BALL_SPEED : -INITIAL_BALL_SPEED;
        ballSpeedY = Math.random() > 0.5 ? INITIAL_BALL_SPEED : -INITIAL_BALL_SPEED;
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

    private void addParticles(double x, double y, int count, Color color) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color));
        }
    }

    private void addCollisionParticles(double x, double y, int count, Color color) {
        // Add more particles with higher velocity for collisions
        for (int i = 0; i < count; i++) {
            Particle p = new Particle(x, y, color);
            // Make collision particles move faster
            p.speedX *= 2;
            p.speedY *= 2;
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

        // Draw a grid pattern for a more modern look
        gc.setStroke(Color.rgb(255, 255, 255, 0.05));
        gc.setLineWidth(1);

        // Vertical grid lines
        for (int x = 0; x < WIDTH; x += 40) {
            gc.strokeLine(x, 0, x, HEIGHT);
        }

        // Horizontal grid lines
        for (int y = 0; y < HEIGHT; y += 40) {
            gc.strokeLine(0, y, WIDTH, y);
        }

        // Render particles
        particles.forEach(p -> p.render(gc));

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
        // Create a semi-transparent overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

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
                         5, 
                         Math.random() > 0.5 ? ACCENT_COLOR : ACCENT_COLOR_ALT);
        }
    }

    private void renderWelcomeScreen(GraphicsContext gc) {
        // Add a subtle pulsing effect to the background
        long currentTime = System.currentTimeMillis();
        double pulseIntensity = 0.05 * Math.sin(currentTime / 1000.0) + 0.95;

        // Add some ambient particles
        if (currentTime % 100 < 20) {
            addParticles(Math.random() * WIDTH, Math.random() * HEIGHT, 2, 
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
        gc.fillText("MODERN EDITION", WIDTH / 2, HEIGHT / 5 + 40);

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
        }

        // Draw instructions panel
        double panelWidth = 600;
        double panelHeight = 280;
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

        // Rules section
        gc.setFont(Font.font(FONT_NAME, FontWeight.BOLD, 20));
        gc.setFill(TEXT_COLOR);
        gc.fillText("Règles:", WIDTH / 2, panelY + 175);

        gc.setFont(Font.font(FONT_NAME, FontWeight.NORMAL, 16));
        gc.setFill(Color.rgb(200, 200, 200));
        gc.fillText("Renvoyez la balle avec votre raquette pour marquer des points.", WIDTH / 2, panelY + 205);
        gc.fillText("Ne laissez pas la balle passer derrière votre raquette!", WIDTH / 2, panelY + 230);
        gc.fillText("Le premier à atteindre 5 points gagne la partie.", WIDTH / 2, panelY + 255);

        // Draw paddles with glow effect
        gc.setEffect(glowEffect);
        gc.setFill(PLAYER_PADDLE_COLOR);
        gc.fillRect(0, playerPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        gc.setFill(AI_PADDLE_COLOR);
        gc.fillRect(WIDTH - PADDLE_WIDTH, aiPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.setEffect(null);
    }

    private void renderGame(GraphicsContext gc) {
        // Draw a stylized center line
        gc.setStroke(Color.rgb(255, 255, 255, 0.3));
        gc.setLineDashes(10);
        gc.setLineWidth(3);
        gc.strokeLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        gc.setLineDashes(0);

        // Draw a court boundary
        gc.setStroke(Color.rgb(255, 255, 255, 0.15));
        gc.setLineWidth(2);
        gc.strokeRect(10, 10, WIDTH - 20, HEIGHT - 20);

        // Draw the paddles with glow effect
        // Player paddle (left)
        gc.setEffect(glowEffect);
        gc.setFill(PLAYER_PADDLE_COLOR);
        gc.fillRoundRect(0, playerPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        // AI paddle (right)
        gc.setFill(AI_PADDLE_COLOR);
        gc.fillRoundRect(WIDTH - PADDLE_WIDTH, aiPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        // Draw the ball with glow effect
        gc.setFill(BALL_COLOR);
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);
        gc.setEffect(null);

        // Draw a shadow under the ball
        gc.setFill(Color.rgb(0, 0, 0, 0.3));
        gc.fillOval(ballX - BALL_RADIUS + 3, ballY - BALL_RADIUS + 3, BALL_RADIUS * 2, BALL_RADIUS * 2);

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

        // Draw a subtle reflection on the floor
        Reflection reflection = new Reflection();
        reflection.setFraction(0.2);
        reflection.setTopOpacity(0.1);
        reflection.setBottomOpacity(0);
        gc.setEffect(reflection);

        // Draw reflections of paddles and ball
        gc.setFill(PLAYER_PADDLE_COLOR);
        gc.fillRoundRect(0, playerPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        gc.setFill(AI_PADDLE_COLOR);
        gc.fillRoundRect(WIDTH - PADDLE_WIDTH, aiPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT, 5, 5);

        gc.setFill(BALL_COLOR);
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

        gc.setEffect(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
