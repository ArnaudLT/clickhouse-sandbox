# Pong Game

A basic implementation of the classic Pong game using JavaFX.

## Game Features

- Welcome screen with game instructions and controls
- Player name input on the welcome screen
- Player-controlled paddle using UP and DOWN arrow keys
- Basic AI opponent that follows the ball
- Score tracking with game ending at 5 points
- Game over screen showing final score and winner with the player's name
- Ball physics with angle changes based on paddle hit position
- Increasing ball speed as the game progresses

## How to Run

There are two ways to run the Pong game:

### 1. Through the Spring Boot Application

Run the main Spring Boot application:

```bash
mvn spring-boot:run
```

This will start the Spring Boot application, which will then launch the Pong game.

### 2. Directly Using the PongLauncher

Run the PongLauncher class directly:

```bash
mvn exec:java -Dexec.mainClass="org.arnaudlt.clickhouse.game.pong.PongLauncher"
```

## Game Controls

### Welcome Screen
- **Letter Keys**: Type your name
- **Backspace**: Delete the last character of your name
- **Enter**: Confirm your name
- **SPACE**: Start the game (after confirming your name)

### During Game
- **UP Arrow**: Move player paddle up
- **DOWN Arrow**: Move player paddle down

### Game Over Screen
- **SPACE**: Restart the game

## Game Rules

- When the game starts, you'll be prompted to enter your name on the welcome screen
- After entering and confirming your name, press SPACE to start the game
- The ball starts in the center of the screen and moves in a random direction
- Players must hit the ball with their paddle to return it to the opponent
- If the ball passes your paddle, the opponent scores a point
- The first player to reach 5 points wins the game
- After a player wins, a game over screen displays the final score and winner (with your name if you win)
- Press SPACE to restart the game after it ends, which will take you back to the name entry screen

## Implementation Details

The game is implemented using JavaFX and consists of the following components:

- **PongGame.java**: The main game class that extends JavaFX Application
- **PongLauncher.java**: A simple launcher class to start the game directly

The game uses a canvas for rendering and an animation timer for the game loop. The AI opponent uses a simple following algorithm to track the ball's position.
