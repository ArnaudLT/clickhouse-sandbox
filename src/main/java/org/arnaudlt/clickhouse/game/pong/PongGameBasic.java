package org.arnaudlt.clickhouse.game.pong;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;

public class PongGameBasic extends Application {

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

        // Create the scene
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        // Set up key event handlers
        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> {
            activeKeys.remove(e.getCode());
            processedKeys.remove(e.getCode()); // Remove from processed keys when released
        });

        // Set up the game loop
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render(gc);
            }
        };

        // Set up the stage
        primaryStage.setTitle("Pong Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Start the game loop
        gameLoop.start();
    }

    private void update() {
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
            }

            // Ball collision with paddles
            // Player paddle (left)
            if (ballX <= PADDLE_WIDTH && ballY >= playerPaddleY && ballY <= playerPaddleY + PADDLE_HEIGHT) {
                ballSpeedX = -ballSpeedX * 1.05; // Increase speed slightly on paddle hit
                // Add some angle based on where the ball hits the paddle
                double relativeIntersectY = (playerPaddleY + (PADDLE_HEIGHT / 2)) - ballY;
                double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;
            }

            // AI paddle (right)
            if (ballX >= WIDTH - PADDLE_WIDTH - BALL_RADIUS && ballY >= aiPaddleY && ballY <= aiPaddleY + PADDLE_HEIGHT) {
                ballSpeedX = -ballSpeedX * 1.05; // Increase speed slightly on paddle hit
                // Add some angle based on where the ball hits the paddle
                double relativeIntersectY = (aiPaddleY + (PADDLE_HEIGHT / 2)) - ballY;
                double normalizedRelativeIntersectionY = relativeIntersectY / (PADDLE_HEIGHT / 2);
                ballSpeedY = -normalizedRelativeIntersectionY * INITIAL_BALL_SPEED;
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
    }

    private void render(GraphicsContext gc) {
        // Clear the canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

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
    }

    private void renderGameOver(GraphicsContext gc) {
        // Set text properties
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);

        // Draw game over title
        gc.setFont(Font.font("Arial", 48));
        gc.fillText("GAME OVER", WIDTH / 2, HEIGHT / 4);

        // Draw final score
        gc.setFont(Font.font("Arial", 36));
        gc.fillText("Score final", WIDTH / 2, HEIGHT / 3);
        gc.fillText(playerScore + " - " + aiScore, WIDTH / 2, HEIGHT / 3 + 40);

        // Draw winner
        gc.setFont(Font.font("Arial", 30));
        String winner = playerScore >= 5 ? playerName : "Ordinateur";
        gc.fillText("Gagnant: " + winner, WIDTH / 2, HEIGHT / 2);

        // Draw restart instruction
        gc.setFont(Font.font("Arial", 24));
        gc.fillText("Appuyez sur ESPACE pour rejouer", WIDTH / 2, HEIGHT * 3 / 4);
    }

    private void renderWelcomeScreen(GraphicsContext gc) {
        // Set text properties
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);

        // Draw title
        gc.setFont(Font.font("Arial", 48));
        gc.fillText("PONG", WIDTH / 2, HEIGHT / 4);

        // Draw name input section
        gc.setFont(Font.font("Arial", 24));
        if (nameInputActive) {
            gc.fillText("Entrez votre nom:", WIDTH / 2, HEIGHT / 3);

            // Draw name input box
            double boxWidth = 300;
            double boxHeight = 40;
            double boxX = WIDTH / 2 - boxWidth / 2;
            double boxY = HEIGHT / 3 + 10;

            // Draw box outline
            gc.setStroke(Color.WHITE);
            gc.strokeRect(boxX, boxY, boxWidth, boxHeight);

            // Draw entered name with cursor
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(Font.font("Arial", 20));
            String displayText = playerName;
            if (showCursor) {
                displayText += "|"; // Add blinking cursor
            }
            gc.fillText(displayText, boxX + 10, boxY + 28);
            gc.setTextAlign(TextAlignment.CENTER); // Reset alignment

            // Draw instruction for confirming name
            gc.setFont(Font.font("Arial", 16));
            gc.fillText("Appuyez sur ENTRÉE pour confirmer", WIDTH / 2, boxY + boxHeight + 25);
        } else {
            // Show confirmed name
            gc.fillText("Bienvenue, " + playerName + "!", WIDTH / 2, HEIGHT / 3);
            gc.setFont(Font.font("Arial", 16));
            gc.fillText("Appuyez sur ESPACE pour commencer", WIDTH / 2, HEIGHT / 3 + 30);
        }

        // Draw instructions
        gc.setFont(Font.font("Arial", 24));
        gc.fillText("Contrôles:", WIDTH / 2, HEIGHT / 2 + 20);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("Flèche HAUT - Déplacer la raquette vers le haut", WIDTH / 2, HEIGHT / 2 + 50);
        gc.fillText("Flèche BAS - Déplacer la raquette vers le bas", WIDTH / 2, HEIGHT / 2 + 80);

        // Draw game rules
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("Renvoyez la balle avec votre raquette pour marquer des points.", WIDTH / 2, HEIGHT / 2 + 120);
        gc.fillText("Ne laissez pas la balle passer derrière votre raquette!", WIDTH / 2, HEIGHT / 2 + 140);
        gc.fillText("Le premier à atteindre 5 points gagne la partie.", WIDTH / 2, HEIGHT / 2 + 160);

        // Draw paddles to show player and AI positions
        gc.fillRect(0, playerPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        gc.fillRect(WIDTH - PADDLE_WIDTH, aiPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
    }

    private void renderGame(GraphicsContext gc) {
        // Draw the center line
        gc.setStroke(Color.WHITE);
        gc.setLineDashes(5);
        gc.strokeLine(WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        gc.setLineDashes(0);

        // Draw the paddles
        gc.setFill(Color.WHITE);
        // Player paddle (left)
        gc.fillRect(0, playerPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        // AI paddle (right)
        gc.fillRect(WIDTH - PADDLE_WIDTH, aiPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);

        // Draw the ball
        gc.fillOval(ballX - BALL_RADIUS, ballY - BALL_RADIUS, BALL_RADIUS * 2, BALL_RADIUS * 2);

        // Draw the scores
        gc.setFont(Font.font(36));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(Integer.toString(playerScore), WIDTH / 4, 50);
        gc.fillText(Integer.toString(aiScore), WIDTH * 3 / 4, 50);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
