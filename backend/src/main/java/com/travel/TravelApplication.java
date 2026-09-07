package com.travel;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(TravelApplication.class, args);
    }

    private static void loadDotenv() {
        Dotenv dotenv = null;
        // Try current working directory
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception ignored) {}

        // If no entries found, try ./backend
        if (dotenv == null || dotenv.entries().isEmpty()) {
            try {
                dotenv = Dotenv.configure().directory("./backend").ignoreIfMissing().load();
            } catch (Exception ignored) {}
        }

        // If still no entries, try parent directory ../
        if (dotenv == null || dotenv.entries().isEmpty()) {
            try {
                dotenv = Dotenv.configure().directory("../").ignoreIfMissing().load();
            } catch (Exception ignored) {}
        }

        if (dotenv != null) {
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        }
    }
}
