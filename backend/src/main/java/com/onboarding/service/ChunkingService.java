package com.onboarding.service;

import com.onboarding.model.CodeChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChunkingService {

    private static final int MAX_CHUNK_LINES = 60;
    private static final int OVERLAP_LINES   = 10;

    // Function/class start patterns per language
    private static final Map<String, String> SYMBOL_PATTERNS = Map.of(
        "py",   "^(def |class |async def )",
        "java", "^\\s*(public|private|protected|static|void|class|interface|enum)",
        "js",   "^(function |class |const |export |async function )",
        "ts",   "^(function |class |const |export |async function |interface |type )",
        "jsx",  "^(function |class |const |export )",
        "tsx",  "^(function |class |const |export )"
    );

    public List<CodeChunk> chunk(String filePath, String content) {
        String ext = getExtension(filePath);
        String pattern = SYMBOL_PATTERNS.get(ext);

        if (pattern != null) {
            List<CodeChunk> chunks = chunkBySymbol(filePath, content, pattern);
            if (!chunks.isEmpty()) return chunks;
        }

        return chunkBySlidingWindow(filePath, content);
    }

    // Split at function/class boundaries
    private List<CodeChunk> chunkBySymbol(String filePath, String content, String pattern) {
        String[] lines = content.split("\n");
        List<CodeChunk> chunks = new ArrayList<>();
        List<Integer> boundaryLines = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches(pattern + ".*")) {
                boundaryLines.add(i);
            }
        }

        if (boundaryLines.isEmpty()) return chunks;

        for (int b = 0; b < boundaryLines.size(); b++) {
            int start = boundaryLines.get(b);
            int end = (b + 1 < boundaryLines.size())
                ? boundaryLines.get(b + 1) - 1
                : lines.length - 1;

            // If symbol block is too large, sub-chunk it
            if (end - start > MAX_CHUNK_LINES) {
                chunks.addAll(chunkBySlidingWindowRange(filePath, lines, start, end));
            } else {
                chunks.add(buildChunk(filePath, lines, start, end));
            }
        }

        // Add any lines before first symbol as a header chunk
        if (boundaryLines.get(0) > 0) {
            chunks.add(0, buildChunk(filePath, lines, 0, boundaryLines.get(0) - 1));
        }

        return chunks;
    }

    // Fallback: sliding window with overlap
    private List<CodeChunk> chunkBySlidingWindow(String filePath, String content) {
        String[] lines = content.split("\n");
        return chunkBySlidingWindowRange(filePath, lines, 0, lines.length - 1);
    }

    private List<CodeChunk> chunkBySlidingWindowRange(String filePath, String[] lines, int from, int to) {
        List<CodeChunk> chunks = new ArrayList<>();
        int i = from;
        while (i <= to) {
            int end = Math.min(i + MAX_CHUNK_LINES - 1, to);
            chunks.add(buildChunk(filePath, lines, i, end));
            i += MAX_CHUNK_LINES - OVERLAP_LINES;
        }
        return chunks;
    }

    private CodeChunk buildChunk(String filePath, String[] lines, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end && i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return new CodeChunk(filePath, start + 1, end + 1, sb.toString().stripTrailing());
    }

    private String getExtension(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot >= 0 ? filePath.substring(dot + 1).toLowerCase() : "";
    }
}
