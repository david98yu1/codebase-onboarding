package com.onboarding.service;

import com.onboarding.model.CodeChunk;
import com.onboarding.model.QAResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QAService {

    private static final int TOP_K = 8;
    private static final double CONFIDENCE_THRESHOLD = 0.05;

    private final RepoStore repoStore;
    private final BedrockService bedrockService;

    public QAService(RepoStore repoStore, BedrockService bedrockService) {
        this.repoStore = repoStore;
        this.bedrockService = bedrockService;
    }

    public QAResponse answer(String repoId, String question) {
        List<CodeChunk> allChunks = repoStore.get(repoId);

        if (allChunks.isEmpty()) {
            return new QAResponse("No repository data found. Please upload a repository first.", List.of());
        }

        // Retrieve top-k relevant chunks via keyword scoring
        List<ScoredChunk> scored = allChunks.stream()
            .map(chunk -> new ScoredChunk(chunk, score(chunk, question)))
            .filter(sc -> sc.score > CONFIDENCE_THRESHOLD)
            .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
            .limit(TOP_K)
            .toList();

        if (scored.isEmpty()) {
            return new QAResponse(
                "I couldn't find relevant code for this question in the repository.",
                List.of()
            );
        }

        // Build prompt with chunks
        String chunksText = scored.stream()
            .map(sc -> "[%s]\n%s".formatted(sc.chunk.header(), sc.chunk.content()))
            .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
            You are a codebase assistant. Answer ONLY using the provided code chunks below.
            For every claim you make, cite the source as [file:line_range].
            If the chunks don't contain enough information to answer confidently, say so explicitly.

            Question: %s

            Relevant code chunks:
            %s
            """.formatted(question, chunksText);

        String answer = bedrockService.invoke(prompt);

        List<String> sources = scored.stream()
            .map(sc -> sc.chunk.filePath() + ":" + sc.chunk.startLine() + "-" + sc.chunk.endLine())
            .distinct()
            .toList();

        return new QAResponse(answer, sources);
    }

    /**
     * Simple keyword-based relevance scoring.
     * Scores a chunk based on how many question tokens appear in it.
     */
    private double score(CodeChunk chunk, String question) {
        String chunkLower = (chunk.filePath() + " " + chunk.content()).toLowerCase();
        String[] tokens = question.toLowerCase()
            .replaceAll("[^a-z0-9 ]", " ")
            .split("\\s+");

        int matches = 0;
        for (String token : tokens) {
            if (token.length() < 3) continue; // skip short words like "is", "in"
            if (chunkLower.contains(token)) matches++;
        }

        return tokens.length > 0 ? (double) matches / tokens.length : 0;
    }

    private record ScoredChunk(CodeChunk chunk, double score) {}
}
