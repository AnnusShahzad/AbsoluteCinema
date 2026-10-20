package com.absolutecinema.service;

import com.absolutecinema.entity.Snack;
import com.absolutecinema.repository.SnackRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SnackService {

    private final SnackRepository snackRepository;

    public SnackService(SnackRepository snackRepository) {
        this.snackRepository = snackRepository;
    }

    /** Snacks that are still in stock. */
    public List<Snack> getAllSnacks() {
        return snackRepository.findByQuantityGreaterThan(0);
    }

    public List<Snack> findAll() {
        return snackRepository.findAll();
    }

    public Optional<Snack> findById(Long id) {
        return snackRepository.findById(id);
    }

    public Snack save(Snack snack) {
        return snackRepository.save(snack);
    }

    /** Updates the editable fields and leaves the sales counter alone. */
    public Optional<Snack> updateSnack(Long id, Snack snackDetails) {
        return snackRepository.findById(id).map(snack -> {
            snack.setName(snackDetails.getName());
            snack.setDescription(snackDetails.getDescription());
            snack.setImageUrl(snackDetails.getImageUrl());
            snack.setPrice(snackDetails.getPrice());
            snack.setQuantity(snackDetails.getQuantity());
            return snackRepository.save(snack);
        });
    }

    public void deleteById(Long id) {
        snackRepository.deleteById(id);
    }
}
