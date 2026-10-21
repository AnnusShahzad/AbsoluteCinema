package com.absolutecinema.service;

import com.absolutecinema.entity.ContactMessage;
import com.absolutecinema.repository.ContactMessageRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContactServiceTest {

    private final ContactMessageRepository repository = mock(ContactMessageRepository.class);
    private final ContactService contactService = new ContactService(repository);

    @Test
    void submitStoresATrimmedMessage() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactMessage message = contactService.submit(" Dana ", "dana@example.com", "", "Booking Issues", " Hi! ");

        assertThat(message.getName()).isEqualTo("Dana");
        assertThat(message.getPhone()).isNull();
        assertThat(message.getSubject()).isEqualTo("Booking Issues");
        assertThat(message.getMessage()).isEqualTo("Hi!");
        assertThat(message.getCreatedAt()).isNotNull();
    }

    @Test
    void unknownSubjectsFallBackToGeneralInquiry() {
        when(repository.save(any(ContactMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(contactService.submit("Dana", "dana@example.com", null, "<script>", "Hi").getSubject())
                .isEqualTo("General Inquiry");
    }

    @Test
    void submitRejectsMissingOrInvalidFields() {
        assertThatThrownBy(() -> contactService.submit("", "dana@example.com", null, null, "Hi"))
                .hasMessageContaining("name");
        assertThatThrownBy(() -> contactService.submit("Dana", "nope", null, null, "Hi"))
                .hasMessageContaining("email");
        assertThatThrownBy(() -> contactService.submit("Dana", "dana@example.com", null, null, " "))
                .hasMessageContaining("Message");
    }
}
