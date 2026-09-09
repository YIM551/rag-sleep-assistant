package com.sleepwell.sleepwell_backend.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagPingController {
    @GetMapping("/api/v1/rag/ping")
    public String ping() {
        return "RAG:PONG";
    }
}

