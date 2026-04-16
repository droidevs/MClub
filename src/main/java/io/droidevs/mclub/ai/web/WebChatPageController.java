package io.droidevs.mclub.ai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebChatPageController {

    @GetMapping("/chat")
    public String chatPage() {
        return "chat";
    }
}


