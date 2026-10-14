package com.sozureke.ledger.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenCodecTest {

    @Test
    void generatedTokensAreUniqueAndLongEnough() {
        String first = TokenCodec.generate();
        String second = TokenCodec.generate();

        assertThat(first).isNotEqualTo(second);
        // 32 random bytes encoded as base64url without padding are 43 characters
        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashIsDeterministicHexOfFixedLengthAndDiffersFromInput() {
        String token = TokenCodec.generate();

        String hash = TokenCodec.hash(token);

        assertThat(hash).isEqualTo(TokenCodec.hash(token));
        assertThat(hash).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(token);
    }

    @Test
    void differentTokensHaveDifferentHashes() {
        assertThat(TokenCodec.hash("a")).isNotEqualTo(TokenCodec.hash("b"));
    }
}
