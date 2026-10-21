package com.absolutecinema.service;

import com.absolutecinema.entity.Snack;
import com.absolutecinema.repository.SnackRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SnackServiceTest {

    private final SnackRepository snackRepository = mock(SnackRepository.class);
    private final SnackService snackService = new SnackService(snackRepository);

    @Test
    void updateSnackKeepsTheSalesCounter() {
        Snack existing = new Snack();
        existing.setId(1L);
        existing.setName("Popcorn");
        existing.setSnackssold(340);
        when(snackRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(snackRepository.save(any(Snack.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Snack changes = new Snack();
        changes.setName("Large Popcorn");
        changes.setPrice(7.5);
        changes.setQuantity(20);
        changes.setImageUrl("  ");

        Snack updated = snackService.updateSnack(1L, changes).orElseThrow();

        assertThat(updated.getName()).isEqualTo("Large Popcorn");
        assertThat(updated.getPrice()).isEqualTo(7.5);
        assertThat(updated.getQuantity()).isEqualTo(20);
        assertThat(updated.getSnackssold()).isEqualTo(340);
        assertThat(updated.getImageUrl()).isNull();
    }

    @Test
    void updateSnackReturnsEmptyForUnknownId() {
        when(snackRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(snackService.updateSnack(9L, new Snack())).isEmpty();
    }
}
