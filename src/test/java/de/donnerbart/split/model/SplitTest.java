package de.donnerbart.split.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static nl.jqno.equalsverifier.Warning.ALL_FIELDS_SHOULD_BE_USED;
import static nl.jqno.equalsverifier.Warning.STRICT_INHERITANCE;

class SplitTest {

    @Test
    void test_equalsAndHashCode() {
        EqualsVerifier.forClass(Split.class).suppress(STRICT_INHERITANCE).suppress(ALL_FIELDS_SHOULD_BE_USED).verify();
    }
}
