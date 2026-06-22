package com.onboarding.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final BedrockService bedrockService;

    // Limit to 2 concurrent API calls to avoid throttling
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public SummaryService(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
    }

    // Step 1: Summarize each file individually (rate-limited parallel)
    public Map<String, String> summarizeFiles(Map<String, String> fileContents) throws InterruptedException, ExecutionException {
        List<Map.Entry<String, String>> entries = new ArrayList<>(fileContents.entrySet());
        List<CompletableFuture<Map.Entry<String, String>>> futures = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, String> entry = entries.get(i);
            // Small stagger between submissions to avoid burst
            final int index = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(index * 300L); // stagger by 300ms each
                } catch (InterruptedException ignored) {}

                String prompt = """
                    Summarize this file in 3-5 sentences.
                    Cover: its purpose, key functions/classes, and what it is responsible for.

                    File: %s
                    Content:
                    %s
                    """.formatted(entry.getKey(), entry.getValue());

                String summary = invokeWithRetry(prompt);
                return Map.entry(entry.getKey(), summary);
            }, executor));
        }

        Map<String, String> results = new LinkedHashMap<>();
        for (CompletableFuture<Map.Entry<String, String>> future : futures) {
            Map.Entry<String, String> entry = future.get();
            results.put(entry.getKey(), entry.getValue());
        }
        return results;
    }

    // Step 2: Group file summaries by top-level directory
    public Map<String, List<Map.Entry<String, String>>> groupByDirectory(Map<String, String> fileSummaries) {
        Map<String, List<Map.Entry<String, String>>> groups = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : fileSummaries.entrySet()) {
            String dir = getTopLevelDir(entry.getKey());
            groups.computeIfAbsent(dir, k -> new ArrayList<>()).add(entry);
        }
        return groups;
    }

    // Step 3: Summarize each directory group (sequential to avoid throttling)
    public Map<String, String> summarizeDirectories(Map<String, List<Map.Entry<String, String>>> groups)
            throws InterruptedException, ExecutionException {

        Map<String, String> results = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map.Entry<String, String>>> entry : groups.entrySet()) {
            String fileSummariesText = entry.getValue().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));

            String prompt = """
                Given these file summaries from the '%s' directory,
                write a concise 2-3 sentence module-level summary covering
                its overall responsibility and role in the codebase.

                Files:
                %s
                """.formatted(entry.getKey(), fileSummariesText);

            results.put(entry.getKey(), invokeWithRetry(prompt));
        }
        return results;
    }

    // Step 4: Generate final repo overview from directory summaries
    public String generateRepoOverview(Map<String, String> directorySummaries) {
        String summariesText = directorySummaries.entrySet().stream()
            .map(e -> "- " + e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining("\n"));

        String prompt = """
            Given these module/directory summaries from a codebase,
            provide a high-level repository overview covering:
            1. What the project does
            2. Major modules and their responsibilities
            3. Likely entry points
            4. General data flow between components

            Modules:
            %s
            """.formatted(summariesText);

        return invokeWithRetry(prompt);
    }

    // Retry up to 3 times with exponential backoff on throttling
    private String invokeWithRetry(String prompt) {
        int maxRetries = 3;
        long delayMs = 2000;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return bedrockService.invoke(prompt);
            } catch (RuntimeException e) {
                if (attempt < maxRetries - 1 && e.getMessage().contains("ThrottlingException")) {
                    try {
                        Thread.sleep(delayMs);
                        delayMs *= 2; // exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    private String getTopLevelDir(String filePath) {
        String path = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        int slash = path.indexOf('/');
        if (slash == -1) return "root";
        String topDir = path.substring(0, slash);
        if (Set.of("src", "lib", "app", "main").contains(topDir)) {
            int nextSlash = path.indexOf('/', slash + 1);
            if (nextSlash != -1) return path.substring(slash + 1, nextSlash);
        }
        return topDir;
    }
}
