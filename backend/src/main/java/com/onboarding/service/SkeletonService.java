package com.onboarding.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SkeletonService {

    // Approximate token threshold — files above this get skeletonized
    private static final int TOKEN_THRESHOLD = 500;

    // Roughly estimate tokens as chars / 4
    private int estimateTokens(String content) {
        return content.length() / 4;
    }

    public String prepare(String filePath, String content) {
        if (estimateTokens(content) <= TOKEN_THRESHOLD) {
            return content; // small enough, use full content
        }
        return extractSkeleton(filePath, content);
    }

    private String extractSkeleton(String filePath, String content) {
        String ext = getExtension(filePath);
        return switch (ext) {
            case "py"         -> extractPythonSkeleton(content);
            case "java"       -> extractJavaSkeleton(content);
            case "js", "ts",
                 "jsx", "tsx" -> extractJsSkeleton(content);
            default           -> truncate(content);
        };
    }

    // Python: keep imports, class/def signatures, strip bodies
    private String extractPythonSkeleton(String content) {
        String[] lines = content.split("\n");
        List<String> result = new ArrayList<>();
        boolean skipBody = false;
        int signatureIndent = -1;

        for (String line : lines) {
            String stripped = line.stripLeading();
            int indent = line.length() - stripped.length();

            // Always keep imports and top-level constants
            if (stripped.startsWith("import ") || stripped.startsWith("from ")) {
                result.add(line);
                skipBody = false;
                continue;
            }

            // Class or function definition
            if (stripped.startsWith("def ") || stripped.startsWith("class ") ||
                stripped.startsWith("async def ")) {
                result.add(line);
                result.add(" ".repeat(indent + 4) + "...");
                skipBody = true;
                signatureIndent = indent;
                continue;
            }

            // If we're skipping a body, only stop when we return to same/lower indent
            if (skipBody) {
                if (!stripped.isEmpty() && indent <= signatureIndent) {
                    skipBody = false;
                    result.add(line); // this line is back at outer scope
                }
                // else skip body lines
            } else {
                result.add(line);
            }
        }
        return String.join("\n", result);
    }

    // Java: keep package, imports, class declaration, method signatures
    private String extractJavaSkeleton(String content) {
        String[] lines = content.split("\n");
        List<String> result = new ArrayList<>();
        int braceDepth = 0;
        boolean inMethodBody = false;
        int methodBraceStart = 0;

        for (String line : lines) {
            String stripped = line.strip();

            // Always keep package/import
            if (stripped.startsWith("package ") || stripped.startsWith("import ")) {
                result.add(line);
                continue;
            }

            // Count braces
            long openBraces  = line.chars().filter(c -> c == '{').count();
            long closeBraces = line.chars().filter(c -> c == '}').count();

            if (inMethodBody) {
                braceDepth += (int)(openBraces - closeBraces);
                if (braceDepth <= methodBraceStart) {
                    inMethodBody = false;
                    result.add(line); // closing brace of method
                }
                // skip body lines
            } else {
                result.add(line);
                braceDepth += (int)(openBraces - closeBraces);

                // Detect method body start: line ends with '{' and we're inside a class
                if (stripped.endsWith("{") && braceDepth >= 2) {
                    inMethodBody = true;
                    methodBraceStart = braceDepth - 1;
                    result.add("    ..."); // placeholder
                }
            }
        }
        return String.join("\n", result);
    }

    // JS/TS: keep imports, function/class signatures
    private String extractJsSkeleton(String content) {
        String[] lines = content.split("\n");
        List<String> result = new ArrayList<>();
        int braceDepth = 0;
        boolean inFunctionBody = false;
        int functionBraceStart = 0;

        // Patterns for function declarations
        Pattern funcPattern = Pattern.compile(
            "^\\s*(export\\s+)?(default\\s+)?(async\\s+)?function\\s+\\w+|" +
            "^\\s*(export\\s+)?const\\s+\\w+\\s*=\\s*(async\\s*)?\\(|" +
            "^\\s*(export\\s+)?class\\s+\\w+"
        );

        for (String line : lines) {
            String stripped = line.strip();

            if (stripped.startsWith("import ") || stripped.startsWith("require(")) {
                result.add(line);
                continue;
            }

            long openBraces  = line.chars().filter(c -> c == '{').count();
            long closeBraces = line.chars().filter(c -> c == '}').count();

            if (inFunctionBody) {
                braceDepth += (int)(openBraces - closeBraces);
                if (braceDepth <= functionBraceStart) {
                    inFunctionBody = false;
                    result.add(line);
                }
            } else {
                result.add(line);
                braceDepth += (int)(openBraces - closeBraces);

                Matcher m = funcPattern.matcher(line);
                if (m.find() && stripped.endsWith("{")) {
                    inFunctionBody = true;
                    functionBraceStart = braceDepth - 1;
                    result.add("  // ...");
                }
            }
        }
        return String.join("\n", result);
    }

    // Fallback: just take the first N lines
    private String truncate(String content) {
        String[] lines = content.split("\n");
        int limit = Math.min(lines.length, 80);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) result.add(lines[i]);
        if (lines.length > limit) result.add("... (truncated)");
        return String.join("\n", result);
    }

    private String getExtension(String filePath) {
        int dot = filePath.lastIndexOf('.');
        return dot >= 0 ? filePath.substring(dot + 1).toLowerCase() : "";
    }
}
