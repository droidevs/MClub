package io.droidevs.mclub.service;
import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;

    public RegistrationDto register(UUID eventId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Event event = eventRepository.findById(eventId).orElseThrow();
        if (registrationRepository.findByUserIdAndEventId(user.getId(), eventId).isPresent()) throw new RuntimeException("Already registered");
        EventRegistration r = EventRegistration.builder().user(user).event(event).build();
        return registrationMapper.toDto(registrationRepository.save(r));
    }
    public List<RegistrationDto> getRegistrations(UUID eventId) {
        return registrationRepository.findByEventId(eventId).stream().map(registrationMapper::toDto).collect(Collectors.toList());
    }
}
