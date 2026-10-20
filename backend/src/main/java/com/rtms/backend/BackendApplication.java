package com.rtms.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	static {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		loadDotenv();
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Tự động tìm và nạp các biến môi trường từ file .env vào System Properties
	 * giúp Spring Boot nhận diện GROQ_API_KEY, JWT_SECRET, etc. khi chạy từ IDE hoặc dòng lệnh.
	 */
	private static void loadDotenv() {
		File[] candidatePaths = new File[] {
				new File(".env"),
				new File("../.env"),
				new File("backend/.env"),
				new File("../../.env")
		};

		for (File envFile : candidatePaths) {
			if (envFile.exists() && envFile.isFile()) {
				log.info("Auto-loading environment variables from .env file: {}", envFile.getAbsolutePath());
				try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
					String line;
					int loadedCount = 0;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.isEmpty() || line.startsWith("#")) {
							continue;
						}
						int eqIdx = line.indexOf('=');
						if (eqIdx > 0) {
							String key = line.substring(0, eqIdx).trim();
							String val = line.substring(eqIdx + 1).trim();
							// Bỏ dấu ngoặc kép hoặc ngoặc đơn nếu có
							if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
								if (val.length() >= 2) {
									val = val.substring(1, val.length() - 1);
								}
							}
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, val);
								loadedCount++;
							}
						}
					}
					log.info("Successfully loaded {} variables from {}", loadedCount, envFile.getName());
				} catch (IOException e) {
					log.warn("Failed to read .env file at {}: {}", envFile.getAbsolutePath(), e.getMessage());
				}
				break;
			}
		}
	}
}
