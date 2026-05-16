package org.backendcompas.unit;

import org.backendcompas.core.validation.NullOrNotBlankValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NullOrNotBlankValidatorTest {

    private final NullOrNotBlankValidator validator = new NullOrNotBlankValidator();

    @Test
    void nullValueIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void blankStringIsInvalid() {
        assertThat(validator.isValid("   ", null)).isFalse();
    }

    @Test
    void emptyStringIsInvalid() {
        assertThat(validator.isValid("", null)).isFalse();
    }

    @Test
    void nonBlankStringIsValid() {
        assertThat(validator.isValid("hello", null)).isTrue();
    }

    @Test
    void stringWithOnlyWhitespaceIsInvalid() {
        assertThat(validator.isValid("\t\n", null)).isFalse();
    }
}
