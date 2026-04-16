package io.droidevs.mclub.ai.conversation;

import org.springframework.stereotype.Component;

/** Parses user replies like "1" / "2" / "yes" to resolve pending intents. */
@Component
public class PendingIntentResolver {

    public enum ResolutionType { CHOICE, CANCEL, UNKNOWN }

    public record Resolution(ResolutionType type, int choiceIndexZeroBased) {
        public static Resolution unknown() { return new Resolution(ResolutionType.UNKNOWN, -1); }
        public static Resolution cancel() { return new Resolution(ResolutionType.CANCEL, -1); }
        public static Resolution choice(int idx0) { return new Resolution(ResolutionType.CHOICE, idx0); }
    }

    public Resolution resolve(String userText, PendingIntent pending) {
        if (pending == null) return Resolution.unknown();
        if (userText == null) return Resolution.unknown();

        String t = userText.trim().toLowerCase();
        if (t.isBlank()) return Resolution.unknown();

        if (t.equals("cancel") || t.equals("stop") || t.equals("no") || t.equals("nah")) {
            return Resolution.cancel();
        }

        try {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= pending.choices().size()) {
                return Resolution.choice(n - 1);
            }
        } catch (NumberFormatException ignored) {
        }

        return Resolution.unknown();
    }
}

