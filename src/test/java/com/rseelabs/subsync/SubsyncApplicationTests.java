package com.rseelabs.subsync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {"cloudinary.cloud-name=test", "cloudinary.api-key=test", "cloudinary.api-secret=test", "jwt.secret=mytestsecretmytestsecretmytestsecretmytestsecret", "jwt.expiration=86400000", "jwt.refresh-expiration=604800000", "tink.client.id=test", "tink.client.secret=test", "tink.redirect.uri=http://localhost"})
class SubsyncApplicationTests {

	@Test
	void contextLoads() {
	}

}
