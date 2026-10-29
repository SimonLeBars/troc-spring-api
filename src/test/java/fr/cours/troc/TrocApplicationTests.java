package fr.cours.troc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // Séance 4 : base en mémoire, pas data/troc
class TrocApplicationTests {

	@Test
	void contextLoads() {
	}

}
