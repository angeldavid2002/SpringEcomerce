package com.angel.ecomerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.seed.enabled=false",
		"app.jwt.secret=test-secret-with-at-least-32-bytes!!"
})
class EcomerceApplicationTests {

	@Test
	void contextLoads() {
	}

}
