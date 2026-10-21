package com.absolutecinema.controller;

import com.absolutecinema.entity.Feedback;
import com.absolutecinema.service.FeedbackService;
import com.absolutecinema.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Customers post feedback; everything else is admin-only (see SecurityConfig). */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    public FeedbackController(FeedbackService feedbackService, UserService userService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    @PostMapping
    public Feedback submitFeedback(Authentication auth, @RequestBody FeedbackRequest request) {
        return feedbackService.submit(userService.findByUsername(auth.getName()), request.movieId(),
                request.rating(), request.comments());
    }

    @GetMapping
    public List<Feedback> getAllFeedback() {
        return feedbackService.getAllFeedback();
    }

    @GetMapping("/{id}")
    public Feedback getFeedbackById(@PathVariable Long id) {
        return feedbackService.getFeedbackById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    public Map<String, Object> getFeedbackStatistics() {
        return feedbackService.getFeedbackStatistics();
    }

    @GetMapping("/list")
    public List<Map<String, Object>> getFeedbackList() {
        return feedbackService.getFeedbackList();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> rejected(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
    }

    public record FeedbackRequest(Long movieId, Integer rating, String comments) {
    }
}
