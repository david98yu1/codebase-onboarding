package com.onboarding.model;

public record CodeChunk(
    String filePath,
    int startLine,
    int endLine,
    String content
) {
    public String header() {
        return "# file: %s | lines: %d-%d".formatted(filePath, startLine, endLine);
    }

    public String formatted() {
        return header() + "\n" + content;
    }
}
