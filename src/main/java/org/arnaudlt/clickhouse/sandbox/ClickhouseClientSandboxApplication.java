package org.arnaudlt.clickhouse.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.arnaudlt.clickhouse.game.pong.PongGame;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javafx.application.Platform;

@Slf4j
@SpringBootApplication
public class ClickhouseClientSandboxApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ClickhouseClientSandboxApplication.class, args);
	}


	@Override
	public void run(String... args) throws Exception {
		log.info("Starting Pong Game...");

		// Launch JavaFX application from a non-JavaFX thread
		Platform.startup(() -> {
			try {
				// Create and start the Pong game
				PongGame pongGame = new PongGame();
				pongGame.start(new javafx.stage.Stage());
			} catch (Exception e) {
				log.error("Error starting Pong game", e);
			}
		});
	}
}
