package de.donnerbart.split.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class SplitTest {

    @Test
    void test_equalsAndHashCode() {
        EqualsVerifier.forClass(Split.class).withOnlyTheseFields("index").verify();
    }
}
