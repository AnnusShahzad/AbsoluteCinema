package com.absolutecinema.controller;

import com.absolutecinema.entity.Session;
import com.absolutecinema.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/sessions")
public class SessionAPIController {

    private final SessionService sessionService;

    @Autowired
    public SessionAPIController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<Session> getAllSessions() {
        return sessionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Session> getSessionById(@PathVariable Long id) {
        return sessionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Session createSession(@RequestBody Session session) {
        return sessionService.save(session);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Session> updateSession(@PathVariable Long id, @RequestBody Session session) {
        if (!sessionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        session.setId(id);
        return ResponseEntity.ok(sessionService.save(session));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        if (!sessionService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        sessionService.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/occupied-seats")
    public Set<String> getOccupiedSeats(@PathVariable Long id) {
        return sessionService.getOccupiedSeats(id);
    }

    @GetMapping("/movie/{movieId}")
    public List<Session> getSessionsByMovieId(@PathVariable Long movieId) {
        return sessionService.findByMovieId(movieId);
    }
} 