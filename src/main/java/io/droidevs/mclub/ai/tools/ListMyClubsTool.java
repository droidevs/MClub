package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Tool: list clubs managed by the linked user (ADMIN/STAFF). */
@Component
@RequiredArgsConstructor
public class ListMyClubsTool implements Tool {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    @Override
    public String name() {
        return "list_my_clubs";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));
        var user = userRepository.findByEmail(email).orElseThrow();

        var managed = membershipRepository.findManagedByUserIdWithClub(user.getId(), ClubRole.ADMIN, ClubRole.STAFF);
        if (managed.isEmpty()) {
            return ToolResult.of("You are not managing any clubs (ADMIN/STAFF). If you meant all memberships, that tool is not available yet.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Clubs you manage:\n");
        for (var m : managed) {
            sb.append("- ").append(m.getClub().getName())
                    .append(" (role=").append(m.getRole()).append(")")
                    .append(" id=").append(m.getClub().getId())
                    .append("\n");
        }
        return ToolResult.of(sb.toString().trim());
    }
}


