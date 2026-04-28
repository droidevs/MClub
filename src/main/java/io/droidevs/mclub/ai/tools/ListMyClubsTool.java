package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.service.ClubService;
import io.droidevs.mclub.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Tool: list clubs managed by the linked user (ADMIN/STAFF). */
@Component
@RequiredArgsConstructor
public class ListMyClubsTool implements Tool {

    private final ClubService clubService;
    private final MembershipService membershipService;

    @Override
    public String name() {
        return "list_my_clubs";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        var managed = clubService.getMyManagedClubs(email, Pageable.unpaged());
        if (managed.isEmpty()) {
            return ToolResult.of("You are not managing any clubs (ADMIN/STAFF). If you meant all memberships, that tool is not available yet.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Clubs you manage:\n");
        for (var c : managed) {
            membershipService.getMembership(c.getId(), email).ifPresent(m ->
                sb.append("- ").append(c.getName())
                        .append(" (role=").append(m.getRole()).append(")")
                        .append(" id=").append(c.getId())
                        .append("\n")
            );
        }
        return ToolResult.of(sb.toString().trim());
    }
}


