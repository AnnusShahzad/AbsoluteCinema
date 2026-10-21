package com.absolutecinema.service;

import com.absolutecinema.entity.ContactMessage;
import com.absolutecinema.repository.ContactMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContactService {

    public static final Set<String> SUBJECTS = Set.of("General Inquiry", "Booking Issues", "Movie Suggestions",
            "Corporate Events", "Facilities", "Lost & Found");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final ContactMessageRepository repository;

    public ContactService(ContactMessageRepository repository) {
        this.repository = repository;
    }

    /** Validates and stores a message. Throws IllegalArgumentException with a user-facing reason. */
    public ContactMessage submit(String name, String email, String phone, String subject, String message) {
        ContactMessage contact = new ContactMessage();
        contact.setName(require(name, "Your name", 100));
        String address = require(email, "Email", 254);
        if (!EMAIL.matcher(address).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        contact.setEmail(address);
        contact.setPhone(phone == null || phone.isBlank() ? null : require(phone, "Phone", 30));
        contact.setSubject(subject != null && SUBJECTS.contains(subject) ? subject : "General Inquiry");
        contact.setMessage(require(message, "Message", 2000));
        contact.setCreatedAt(LocalDateTime.now());
        return repository.save(contact);
    }

    public List<ContactMessage> getInbox() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private static String require(String value, String field, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }
}
