package com.absolutecinema.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeatLayoutTest {

    @Test
    void validateNormalizesLabels() {
        assertThat(SeatLayout.validate(List.of(" c7", "H12", "a1"))).containsExactly("C7", "H12", "A1");
    }

    @Test
    void validateRejectsSeatsOutsideTheHall() {
        for (String seat : new String[] { "I1", "A0", "A13", "A07", "7A", "", "AA" }) {
            assertThatThrownBy(() -> SeatLayout.validate(List.of(seat)))
                    .as(seat).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> SeatLayout.validate(Arrays.asList((String) null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateRejectsEmptyAndDuplicateSelections() {
        assertThatThrownBy(() -> SeatLayout.validate(List.of())).hasMessageContaining("at least one seat");
        assertThatThrownBy(() -> SeatLayout.validate(List.of("C1", "c1"))).hasMessageContaining("twice");
    }

    @Test
    void premiumRowsAreAAndB() {
        assertThat(SeatLayout.isPremium("A5")).isTrue();
        assertThat(SeatLayout.isPremium("B12")).isTrue();
        assertThat(SeatLayout.isPremium("C1")).isFalse();
        assertThat(SeatLayout.TOTAL_SEATS).isEqualTo(96);
    }

    @Test
    void parseAndFormatRoundTrip() {
        assertThat(SeatLayout.parse(SeatLayout.format(List.of("C1", "C2")))).containsExactly("C1", "C2");
        assertThat(SeatLayout.parse(null)).isEmpty();
        assertThat(SeatLayout.parse(" ")).isEmpty();
    }
}
