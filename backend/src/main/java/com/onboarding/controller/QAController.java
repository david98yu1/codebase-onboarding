package com.onboarding.controller;

import com.onboarding.model.QAResponse;
import com.onboarding.service.QAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class QAController {

    private final QAService qaService;

    public QAController(QAService qaService) {
        this.qaService = qaService;
    }

    @PostMapping("/qa")
    public ResponseEntity<?> ask(
        @RequestParam String repoId,
        @RequestParam String question
    ) {
        try {
            QAResponse response = qaService.answer(repoId, question);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
