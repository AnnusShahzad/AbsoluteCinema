package com.absolutecinema.service;

import com.absolutecinema.entity.User;
import com.absolutecinema.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loyalty rules. Points are stored on {@link User}; this class owns how they are earned,
 * spent and mapped to tiers.
 */
@Service
public class LoyaltyService {

    public static final int POINTS_PER_TICKET = 50;
    public static final int POINTS_PER_SNACK = 20;
    public static final int REDEMPTION_COST = 250;
    public static final int SILVER_THRESHOLD = 500;
    public static final int GOLD_THRESHOLD = 1000;

    private final UserRepository userRepository;

    public LoyaltyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static String tierFor(int points) {
        if (points >= GOLD_THRESHOLD) {
            return "GOLD";
        }
        if (points >= SILVER_THRESHOLD) {
            return "SILVER";
        }
        return "BRONZE";
    }

    public static int pointsEarned(int tickets, int snackItems) {
        return tickets * POINTS_PER_TICKET + snackItems * POINTS_PER_SNACK;
    }

    /** Customer accounts, highest balance first. */
    public List<User> getMembers() {
        return userRepository.findByAccountTypeIgnoreCaseOrderByPointsDesc("user");
    }

    public LoyaltySummary summarize(List<User> members) {
        int totalPoints = members.stream().mapToInt(User::getPoints).sum();
        long silver = members.stream().filter(m -> "SILVER".equals(tierFor(m.getPoints()))).count();
        long gold = members.stream().filter(m -> "GOLD".equals(tierFor(m.getPoints()))).count();
        int average = members.isEmpty() ? 0 : totalPoints / members.size();
        return new LoyaltySummary(members.size(), average, totalPoints, silver, gold);
    }

    public record LoyaltySummary(int members, int averagePoints, int totalPoints, long silverMembers,
            long goldMembers) {
    }
}
