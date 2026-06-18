package com.onboarding.service;

import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class FileFilterService {

    // Directories to skip entirely
    private static final Set<String> BLOCKED_DIRS = Set.of(
        "node_modules", ".git", ".idea", "target", "build", "dist",
        "__pycache__", ".gradle", "vendor", "bin", "obj"
    );

    // File extensions to skip
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        ".class", ".jar", ".war", ".exe", ".dll", ".so", ".dylib",
        ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
        ".zip", ".tar", ".gz", ".rar",
        ".pdf", ".doc", ".docx",
        ".lock", ".sum"
    );

    // Filenames to skip
    private static final Set<String> BLOCKED_FILENAMES = Set.of(
        "package-lock.json", "yarn.lock", "poetry.lock",
        "Thumbs.db", ".DS_Store"
    );

    // Minimum file size in characters to be worth summarizing
    private static final int MIN_FILE_LENGTH = 5;

    public boolean shouldInclude(String filePath, String content) {
        // Check blocked directories
        for (String dir : BLOCKED_DIRS) {
            if (filePath.contains("/" + dir + "/") || filePath.startsWith(dir + "/")) {
                return false;
            }
        }

        // Check blocked filenames
        String fileName = filePath.contains("/")
            ? filePath.substring(filePath.lastIndexOf("/") + 1)
            : filePath;

        if (BLOCKED_FILENAMES.contains(fileName)) return false;

        // Check blocked extensions
        for (String ext : BLOCKED_EXTENSIONS) {
            if (fileName.endsWith(ext)) return false;
        }

        // Skip tiny files
        if (content == null || content.strip().length() < MIN_FILE_LENGTH) return false;

        return true;
    }
}
