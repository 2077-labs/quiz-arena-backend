package com.example.quizarenabackend;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// This class is needed for @Scheduled methods: currently these are idle room sweeper and WebSocket heartbeat checker.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
