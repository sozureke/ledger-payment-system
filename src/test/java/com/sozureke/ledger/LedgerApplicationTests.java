package com.sozureke.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.sozureke.ledger.support.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LedgerApplicationTests {

	@Test
	void contextLoads() {
	}

}
