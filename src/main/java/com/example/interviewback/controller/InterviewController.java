package com.example.interviewback.controller;

import com.example.interviewback.dto.GeneratedQuestionDto;
import com.example.interviewback.dto.InterviewAnswerRequest;
import com.example.interviewback.dto.InterviewFeedbackResponse;
import com.example.interviewback.dto.InterviewHintRequest;
import com.example.interviewback.dto.InterviewHintResponse;
import com.example.interviewback.dto.InterviewHistoryDto;
import com.example.interviewback.dto.InterviewStatsDto;
import com.example.interviewback.dto.QuestionGenerationRequest;
import com.example.interviewback.security.AuthenticatedUser;
import com.example.interviewback.service.GroqService;
import com.example.interviewback.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de práctica de entrevistas.
 *
 * <p>El usuario autenticado se recibe con {@code @AuthenticationPrincipal}, que
 * lo toma del contexto de seguridad que carga {@code JwtAuthFilter}. Todas las
 * rutas de este controlador requieren token válido.
 */
@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;
    private final GroqService groqService;

    public InterviewController(InterviewService interviewService, GroqService groqService) {
        this.interviewService = interviewService;
        this.groqService = groqService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<GeneratedQuestionDto>> generateQuestions(
            @Valid @RequestBody QuestionGenerationRequest request
    ) {
        return ResponseEntity.ok(groqService.generateQuestions(
                request.getTopic(),
                request.getDifficulty(),
                request.getCount()
        ));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<InterviewFeedbackResponse> evaluate(
            @Valid @RequestBody InterviewAnswerRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(interviewService.evaluate(request, user.id()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<InterviewHistoryDto>> getHistory(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(interviewService.getUserHistory(user.id()));
    }

    @GetMapping("/stats")
    public ResponseEntity<InterviewStatsDto> getStats(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok(interviewService.getUserStats(user.id()));
    }

    @PostMapping("/hint")
    public ResponseEntity<InterviewHintResponse> getHint(
            @Valid @RequestBody InterviewHintRequest request
    ) {
        return ResponseEntity.ok(interviewService.getHint(request));
    }
}
