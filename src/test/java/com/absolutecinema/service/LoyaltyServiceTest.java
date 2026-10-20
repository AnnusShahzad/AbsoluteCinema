package com.absolutecinema.service;

import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LoyaltyServiceTest {

    private final LoyaltyService loyaltyService = new LoyaltyService(mock(UserRepository.class));

    @Test
    void tierForUsesInclusiveThresholds() {
        assertThat(LoyaltyService.tierFor(0)).isEqualTo("BRONZE");
        assertThat(LoyaltyService.tierFor(LoyaltyService.SILVER_THRESHOLD - 1)).isEqualTo("BRONZE");
        assertThat(LoyaltyService.tierFor(LoyaltyService.SILVER_THRESHOLD)).isEqualTo("SILVER");
        assertThat(LoyaltyService.tierFor(LoyaltyService.GOLD_THRESHOLD - 1)).isEqualTo("SILVER");
        assertThat(LoyaltyService.tierFor(LoyaltyService.GOLD_THRESHOLD)).isEqualTo("GOLD");
    }

    @Test
    void pointsEarnedCombinesTicketsAndSnacks() {
        assertThat(LoyaltyService.pointsEarned(2, 3))
                .isEqualTo(2 * LoyaltyService.POINTS_PER_TICKET + 3 * LoyaltyService.POINTS_PER_SNACK);
        assertThat(LoyaltyService.pointsEarned(0, 0)).isZero();
    }

    @Test
    void summarizeCountsTiersAndAveragesPoints() {
        LoyaltyService.LoyaltySummary summary = loyaltyService.summarize(
                List.of(member(100), member(600), member(1200), member(1500)));

        assertThat(summary.members()).isEqualTo(4);
        assertThat(summary.totalPoints()).isEqualTo(3400);
        assertThat(summary.averagePoints()).isEqualTo(850);
        assertThat(summary.silverMembers()).isEqualTo(1);
        assertThat(summary.goldMembers()).isEqualTo(2);
    }

    @Test
    void summarizeHandlesNoMembers() {
        LoyaltyService.LoyaltySummary summary = loyaltyService.summarize(List.of());

        assertThat(summary.members()).isZero();
        assertThat(summary.averagePoints()).isZero();
    }

    private static User member(int points) {
        User user = new User();
        user.setPoints(points);
        return user;
    }
}
