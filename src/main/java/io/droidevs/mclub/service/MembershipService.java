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
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final MembershipMapper membershipMapper;

    public MembershipDto joinClub(UUID clubId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        if (membershipRepository.findByUserIdAndClubId(user.getId(), clubId).isPresent()) throw new RuntimeException("Already joined");
        Membership m = Membership.builder().user(user).club(club).role(user.getRole()).status("PENDING").build();
        return membershipMapper.toDto(membershipRepository.save(m));
    }
    public MembershipDto updateStatus(UUID membershipId, String status) {
        Membership m = membershipRepository.findById(membershipId).orElseThrow();
        m.setStatus(status);
        return membershipMapper.toDto(membershipRepository.save(m));
    }
    public List<MembershipDto> getMembers(UUID clubId) {
        return membershipRepository.findByClubId(clubId).stream().map(membershipMapper::toDto).collect(Collectors.toList());
    }
}
