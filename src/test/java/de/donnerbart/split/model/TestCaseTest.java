package de.donnerbart.split.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

class TestCaseTest {

    @Test
    void test_equalsAndHashCode() {
        EqualsVerifier.forClass(TestCase.class).withOnlyTheseFields("name").verify();
    }
}
