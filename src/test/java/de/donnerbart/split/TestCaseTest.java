package de.donnerbart.split;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.ALL_FIELDS_SHOULD_BE_USED;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

class TestCaseTest {

    @Test
    void test_equalsAndHashCode() {
        EqualsVerifier.forClass(TestCase.class)
                .suppress(STRICT_INHERITANCE)
                .suppress(ALL_FIELDS_SHOULD_BE_USED)
                .verify();
    }
}
