package com.sozureke.ledger.support;

import org.springframework.boot.SpringApplication;

import com.sozureke.ledger.LedgerApplication;

public class TestLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.from(LedgerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
