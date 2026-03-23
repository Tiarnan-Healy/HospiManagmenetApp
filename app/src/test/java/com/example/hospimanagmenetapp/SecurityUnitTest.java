package com.example.hospimanagmenetapp;

import org.junit.Test;
import static org.junit.Assert.*;
import com.example.hospimanagmenetapp.util.ValidationUtils;
public class SecurityUnitTest {

    // NHS Number Validation

    @Test
    public void validNhsNumber_returnsTrue() {
        assertTrue(ValidationUtils.validateNhsNumber("0123456789"));
    }

    @Test
    public void invalidNhsNumber_wrongCheckDigit_returnsFalse() {
        assertFalse(ValidationUtils.validateNhsNumber("0101010101"));
    }

    @Test
    public void nhsNumber_withSpaces_isStrippedAndValidated() {
        assertTrue(ValidationUtils.validateNhsNumber("012 345 6789"));
    }

    @Test
    public void nullNhsNumber_returnsFalse() {
        assertFalse(ValidationUtils.validateNhsNumber(null));
    }

    @Test
    public void nhsNumber_tooShort_returnsFalse() {
        assertFalse(ValidationUtils.validateNhsNumber("123456789"));
    }

    // PIN Hashing

    @Test
    public void sha256_sameInput_producesSameHash() {
        String h1 = ValidationUtils.sha256("1234");
        String h2 = ValidationUtils.sha256("1234");
        assertEquals(h1, h2);
    }

    @Test
    public void sha256_differentInputs_produceDifferentHashes() {
        assertNotEquals(
                ValidationUtils.sha256("1234"),
                ValidationUtils.sha256("1235")
        );
    }

    @Test
    public void sha256_isNotReversible() {
        // Hash does not equal input
        String pin = "9999";
        assertNotEquals(pin, ValidationUtils.sha256(pin));
    }

    // Input Sanitisation

    @Test
    public void sanitise_removesHtmlTags() {
        assertEquals("script", ValidationUtils.sanitiseInput("<script>"));
    }

    @Test
    public void sanitise_removesSqlKeywords() {
        assertEquals("patients", ValidationUtils.sanitiseInput("DROP patients"));
    }

    @Test
    public void sanitise_removesSemicolons() {
        assertFalse(ValidationUtils.sanitiseInput("value; DROP TABLE").contains(";"));
    }

    @Test
    public void sanitise_nullInput_returnsEmptyString() {
        assertEquals("", ValidationUtils.sanitiseInput(null));
    }

    @Test
    public void sanitise_cleanInput_isUnchanged() {
        assertEquals("Clean input", ValidationUtils.sanitiseInput("Clean input"));
    }
}
