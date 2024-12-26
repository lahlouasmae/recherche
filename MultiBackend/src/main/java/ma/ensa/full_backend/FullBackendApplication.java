package ma.ensa.full_backend;

import ma.ensa.full_backend.test.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FullBackendApplication implements CommandLineRunner {
	@Autowired
	private TestService testDataService;

	public static void main(String[] args) {
		SpringApplication.run(FullBackendApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Create test data when the application starts
		testDataService.createTestData();
	}
}
