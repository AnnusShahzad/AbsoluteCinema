package com.absolutecinema.service;

import com.absolutecinema.entity.Feedback;
import com.absolutecinema.entity.User;
import com.absolutecinema.repository.FeedbackRepository;
import com.absolutecinema.repository.MovieRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {

    private final FeedbackRepository feedbackRepository = mock(FeedbackRepository.class);
    private final MovieRepository movieRepository = mock(MovieRepository.class);
    private final FeedbackService feedbackService = new FeedbackService(feedbackRepository, movieRepository);

    @Test
    void sentimentBuckets() {
        assertThat(FeedbackService.sentimentOf(5)).isEqualTo("Positive");
        assertThat(FeedbackService.sentimentOf(4)).isEqualTo("Positive");
        assertThat(FeedbackService.sentimentOf(3)).isEqualTo("Neutral");
        assertThat(FeedbackService.sentimentOf(2)).isEqualTo("Negative");
    }

    @Test
    void submitValidatesMovieRatingAndComments() {
        when(movieRepository.existsById(1L)).thenReturn(true);
        User user = new User();

        assertThatThrownBy(() -> feedbackService.submit(user, 9L, 5, "Great")).hasMessageContaining("movie");
        assertThatThrownBy(() -> feedbackService.submit(user, 1L, 6, "Great")).hasMessageContaining("rating");
        assertThatThrownBy(() -> feedbackService.submit(user, 1L, null, "Great")).hasMessageContaining("rating");
        assertThatThrownBy(() -> feedbackService.submit(user, 1L, 4, "  ")).hasMessageContaining("Comments");
    }

    @Test
    void submitAssignsTheNextIdAndTimestamp() {
        when(movieRepository.existsById(1L)).thenReturn(true);
        when(feedbackRepository.findMaxId()).thenReturn(20L);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Feedback feedback = feedbackService.submit(new User(), 1L, 4, " Loved it ");

        assertThat(feedback.getId()).isEqualTo(21L);
        assertThat(feedback.getComments()).isEqualTo("Loved it");
        assertThat(feedback.getCreatedAt()).isNotNull();
    }

    @Test
    void statisticsCountSentimentsAndIgnoreUndatedFeedback() {
        when(feedbackRepository.findAll()).thenReturn(List.of(
                feedback(5, LocalDateTime.now()), feedback(3, null), feedback(1, LocalDateTime.now())));

        Map<String, Object> stats = feedbackService.getFeedbackStatistics();

        assertThat(stats).containsEntry("totalFeedback", 3)
                .containsEntry("positiveFeedback", 1L)
                .containsEntry("neutralFeedback", 1L)
                .containsEntry("negativeFeedback", 1L);
    }

    private static Feedback feedback(int rating, LocalDateTime createdAt) {
        Feedback feedback = new Feedback();
        feedback.setRating(rating);
        feedback.setCreatedAt(createdAt);
        return feedback;
    }
}
