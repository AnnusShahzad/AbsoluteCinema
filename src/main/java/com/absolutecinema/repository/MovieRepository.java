package com.absolutecinema.repository;

import com.absolutecinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByTitleContainingIgnoreCase(String title);


    List<Movie> findByTitleContainingIgnoreCaseAndGenreIgnoreCase(String title, String genre);
}