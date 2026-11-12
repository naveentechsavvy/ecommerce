package com.apparel.commerce.chat;

import com.apparel.commerce.chat.ChatDtos.ChatRequest;
import com.apparel.commerce.chat.ChatDtos.ChatResponse;
import com.apparel.commerce.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    /** Simple per-client sliding window (single node). Use the API gateway / Redis limiter in production. */
    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    private final ChatService chat;
    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req, HttpServletRequest http) {
        throttle(http.getRemoteAddr());
        return chat.chat(req);
    }

    private void throttle(String clientKey) {
        Instant cutoff = Instant.now().minusSeconds(60);
        Deque<Instant> window = windows.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.pollFirst();
            if (window.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "You're sending messages quickly - please wait a moment.");
            }
            window.addLast(Instant.now());
        }
    }
}
