package com.absolutecinema.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The auditorium seat map: rows A-H with 12 seats each, rows A and B being premium.
 * Seats are identified by labels such as "A7".
 */
public final class SeatLayout {

    public static final String ROWS = "ABCDEFGH";
    public static final String PREMIUM_ROWS = "AB";
    public static final int SEATS_PER_ROW = 12;
    public static final int TOTAL_SEATS = ROWS.length() * SEATS_PER_ROW;

    private SeatLayout() {
    }

    /** Normalizes and validates seat labels, rejecting unknown or duplicate seats. */
    public static List<String> validate(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            throw new IllegalArgumentException("Select at least one seat");
        }
        Set<String> seats = new LinkedHashSet<>();
        for (String label : labels) {
            String seat = label == null ? "" : label.trim().toUpperCase();
            if (!isValid(seat)) {
                throw new IllegalArgumentException("Unknown seat: " + label);
            }
            if (!seats.add(seat)) {
                throw new IllegalArgumentException("Seat selected twice: " + seat);
            }
        }
        return new ArrayList<>(seats);
    }

    public static boolean isPremium(String seat) {
        return PREMIUM_ROWS.indexOf(seat.charAt(0)) >= 0;
    }

    /** Splits a stored comma-separated seat list; blank input yields no seats. */
    public static List<String> parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static String format(List<String> seats) {
        return String.join(",", seats);
    }

    private static boolean isValid(String seat) {
        if (seat.length() < 2 || ROWS.indexOf(seat.charAt(0)) < 0) {
            return false;
        }
        try {
            int number = Integer.parseInt(seat.substring(1));
            return number >= 1 && number <= SEATS_PER_ROW && seat.substring(1).equals(String.valueOf(number));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
