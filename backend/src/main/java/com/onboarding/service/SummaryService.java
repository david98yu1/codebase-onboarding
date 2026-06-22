package com.onboarding.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SummaryService {

    private final BedrockService bedrockService;

    // Max files to summarize total
    private static final int MAX_FILES = 20;

    // Files per batch (one Claude call per batch)
    private static final int BATCH_SIZE = 5;

    // Significant filenames that score higher
    private static final Set<String> SIGNIFICANT_NAMES = Set.of(
        "main", "app", "index", "server", "router", "config",
        "controller", "service", "model", "handler", "core", "api"
    );

    public SummaryService(BedrockService bedrockService) {
        this.bedrockService = bedrockService;
    }

    // Step 1: Pick top N files by importance, then summarize in batches
    public Map<String, String> summarizeFiles(Map<String, String> fileContents) {
        // Score and cap files
        List<Map.Entry<String, String>> topFiles = fileContents.entrySet().stream()
            .sorted((a, b) -> Double.compare(scoreFile(b.getKey(), b.getValue()), scoreFile(a.getKey(), a.getValue())))
            .limit(MAX_FILES)
            .toList();

        // Batch into groups of BATCH_SIZE
        Map<String, String> results = new LinkedHashMap<>();
        for (int i = 0; i < topFiles.size(); i += BATCH_SIZE) {
            List<Map.Entry<String, String>> batch = topFiles.subList(i, Math.min(i + BATCH_SIZE, topFiles.size()));
            Map<String, String> batchResults = summarizeBatch(batch);
            results.putAll(batchResults);
        }
        return results;
    }

    // Summarize a batch of files in a single Claude call
    private Map<String, String> summarizeBatch(List<Map.Entry<String, String>> batch) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Summarize each of the following files in 2-3 sentences covering its purpose and key responsibilities.\n");
        prompt.append("Return your response as a list where each entry starts with the exact filename on its own line followed by the summary.\n\n");

        for (Map.Entry<String, String> entry : batch) {
            prompt.append("=== FILE: ").append(entry.getKey()).append(" ===\n");
            prompt.append(entry.getValue()).append("\n\n");
        }

        String response = invokeWithRetry(prompt.toString());

        // Parse response back into per-file summaries
        return parseBatchResponse(response, batch);
    }

    // Parse Claude's batch response into a map of filename -> summary
    private Map<String, String> parseBatchResponse(String response, List<Map.Entry<String, String>> batch) {
        Map<String, String> results = new LinkedHashMap<>();
        String[] lines = response.split("\n");
        String currentFile = null;
        StringBuilder currentSummary = new StringBuilder();

        for (String line : lines) {
            // Detect file header lines like "=== FILE: path/to/file.java ===" or just "path/to/file.java:"
            String matchedFile = null;
            for (Map.Entry<String, String> entry : batch) {
                if (line.contains(entry.getKey())) {
                    matchedFile = entry.getKey();
                    break;
                }
            }

            if (matchedFile != null) {
                // Save previous
                if (currentFile != null && !currentSummary.isEmpty()) {
                    results.put(currentFile, currentSummary.toString().strip());
                }
                currentFile = matchedFile;
                currentSummary = new StringBuilder();
            } else if (currentFile != null && !line.isBlank()) {
                currentSummary.append(line).append(" ");
            }
        }

        // Save last entry
        if (currentFile != null && !currentSummary.isEmpty()) {
            results.put(currentFile, currentSummary.toString().strip());
        }

        // Fallback: if parsing failed for any file, use a placeholder
        for (Map.Entry<String, String> entry : batch) {
            results.putIfAbsent(entry.getKey(), "Summary not available.");
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

    // Step 3: Summarize all directories in one Claude call
    public Map<String, String> summarizeDirectories(Map<String, List<Map.Entry<String, String>>> groups) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("For each module/directory below, write a 1-2 sentence summary of its overall responsibility.\n");
        prompt.append("Return each as 'MODULE: <name>\\n<summary>'\n\n");

        for (Map.Entry<String, List<Map.Entry<String, String>>> entry : groups.entrySet()) {
            prompt.append("=== MODULE: ").append(entry.getKey()).append(" ===\n");
            for (Map.Entry<String, String> file : entry.getValue()) {
                prompt.append("- ").append(file.getKey()).append(": ").append(file.getValue()).append("\n");
            }
            prompt.append("\n");
        }

        String response = invokeWithRetry(prompt.toString());
        return parseDirectoryResponse(response, groups.keySet());
    }

    private Map<String, String> parseDirectoryResponse(String response, Set<String> dirNames) {
        Map<String, String> results = new LinkedHashMap<>();
        String[] lines = response.split("\n");
        String currentDir = null;
        StringBuilder currentSummary = new StringBuilder();

        for (String line : lines) {
            String matchedDir = null;
            for (String dir : dirNames) {
                if (line.contains(dir)) {
                    matchedDir = dir;
                    break;
                }
            }

            if (matchedDir != null) {
                if (currentDir != null && !currentSummary.isEmpty()) {
                    results.put(currentDir, currentSummary.toString().strip());
                }
                currentDir = matchedDir;
                currentSummary = new StringBuilder();
            } else if (currentDir != null && !line.isBlank()) {
                currentSummary.append(line).append(" ");
            }
        }

        if (currentDir != null && !currentSummary.isEmpty()) {
            results.put(currentDir, currentSummary.toString().strip());
        }

        for (String dir : dirNames) {
            results.putIfAbsent(dir, "No summary available.");
        }

        return results;
    }

    // Step 4: Final repo overview
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

    // Score a file by importance for prioritization
    private double scoreFile(String filePath, String content) {
        double score = 0;
        String fileName = filePath.contains("/")
            ? filePath.substring(filePath.lastIndexOf('/') + 1).toLowerCase()
            : filePath.toLowerCase();
        String nameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        // Significant name bonus
        if (SIGNIFICANT_NAMES.contains(nameNoExt)) score += 10;

        // Shallower path = more important
        int depth = filePath.split("/").length;
        score += Math.max(0, 5 - depth);

        // Larger files tend to have more responsibility
        score += Math.min(content.length() / 500.0, 5);

        return score;
    }

    // Retry with exponential backoff on throttling
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
                        delayMs *= 2;
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
