package com.absolutecinema.service;

import com.absolutecinema.entity.Feedback;
import com.absolutecinema.entity.Movie;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.FeedbackRepository;
import com.absolutecinema.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int TREND_DAYS = 30;

    private final FeedbackRepository feedbackRepository;
    private final MovieRepository movieRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, MovieRepository movieRepository) {
        this.feedbackRepository = feedbackRepository;
        this.movieRepository = movieRepository;
    }

    /** Positive is 4-5 stars, negative is 1-2 stars and 3 stars is neutral. */
    public static String sentimentOf(int rating) {
        if (rating >= 4) {
            return "Positive";
        }
        return rating <= 2 ? "Negative" : "Neutral";
    }

    @Transactional
    public Feedback submit(User user, Long movieId, Integer rating, String comments) {
        if (movieId == null || !movieRepository.existsById(movieId)) {
            throw new IllegalArgumentException("Please choose a movie");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please give a rating from 1 to 5 stars");
        }
        String text = comments == null ? "" : comments.trim();
        if (text.isEmpty() || text.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comments are required (up to " + MAX_COMMENT_LENGTH + " characters)");
        }
        Feedback feedback = new Feedback();
        // Ids are assigned by the application because the feedback table has no identity column
        feedback.setId(feedbackRepository.findMaxId() + 1);
        feedback.setUser(user);
        feedback.setMovieId(movieId);
        feedback.setRating(rating);
        feedback.setComments(text);
        feedback.setCreatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public Feedback getFeedbackById(Long id) {
        return feedbackRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Feedback " + id));
    }

    public void deleteFeedback(Long id) {
        feedbackRepository.delete(getFeedbackById(id));
    }

    /** Totals by sentiment and the daily average rating over the last 30 days. */
    public Map<String, Object> getFeedbackStatistics() {
        List<Feedback> all = feedbackRepository.findAll();
        Map<String, Long> bySentiment = all.stream()
                .collect(Collectors.groupingBy(f -> sentimentOf(f.getRating()), Collectors.counting()));

        LocalDate firstDay = LocalDate.now().minusDays(TREND_DAYS - 1);
        Map<LocalDate, Double> averageByDay = all.stream()
                .filter(f -> f.getCreatedAt() != null && !f.getCreatedAt().toLocalDate().isBefore(firstDay))
                .collect(Collectors.groupingBy(f -> f.getCreatedAt().toLocalDate(),
                        Collectors.averagingInt(Feedback::getRating)));

        List<String> labels = new ArrayList<>();
        List<Double> averageRatings = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM d");
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = firstDay.plusDays(i);
            labels.add(day.format(label));
            averageRatings.add(averageByDay.getOrDefault(day, 0.0));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedback", all.size());
        stats.put("positiveFeedback", bySentiment.getOrDefault("Positive", 0L));
        stats.put("neutralFeedback", bySentiment.getOrDefault("Neutral", 0L));
        stats.put("negativeFeedback", bySentiment.getOrDefault("Negative", 0L));
        stats.put("chartData", Map.of("labels", labels, "averageRatings", averageRatings));
        return stats;
    }

    /** Feedback rows for the admin table, newest first. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFeedbackList() {
        List<Feedback> feedbacks = getAllFeedback();
        Map<Long, String> titles = movieRepository.findAllById(
                feedbacks.stream().map(Feedback::getMovieId).distinct().toList()).stream()
                .collect(Collectors.toMap(Movie::getId, Movie::getTitle));
        return feedbacks.stream().map(feedback -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", feedback.getId());
            row.put("rating", feedback.getRating());
            row.put("comment", feedback.getComments());
            row.put("createdAt", feedback.getCreatedAt());
            row.put("movieTitle", titles.getOrDefault(feedback.getMovieId(), "Removed movie"));
            User user = feedback.getUser();
            row.put("userName", user == null ? "Anonymous" : user.getFirstName() + " " + user.getLastName());
            row.put("sentiment", sentimentOf(feedback.getRating()));
            return row;
        }).toList();
    }
}
