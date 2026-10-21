package com.absolutecinema.controller;

import com.absolutecinema.entity.Movie;
import com.absolutecinema.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public String listMovies(Model model) {
        model.addAttribute("movies", movieService.getMoviesByPopularity());
        return "movies/list";
    }

    @GetMapping("/{id}")
    public String showMovieDetails(@PathVariable Long id, Model model) {
        Movie movie = movieService.findById(id);
        if (movie == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("movie", movie);
        return "movies/movie-details";
    }

    @GetMapping("/search")
    public String searchMovies(@RequestParam(required = false) String query,
            @RequestParam(required = false) String genre, Model model) {
        model.addAttribute("movies", movieService.search(query, genre));
        model.addAttribute("currentQuery", query == null ? "" : query.trim());
        model.addAttribute("currentGenre", genre == null ? "" : genre.trim());
        return "movies/list";
    }
}
