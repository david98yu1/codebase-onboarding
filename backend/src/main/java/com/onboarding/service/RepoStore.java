package com.onboarding.service;

import com.onboarding.model.CodeChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for chunked repo data.
 * Keyed by repoId (returned to frontend on upload, sent back on each question).
 */
@Service
public class RepoStore {

    private final Map<String, List<CodeChunk>> store = new ConcurrentHashMap<>();

    public void save(String repoId, List<CodeChunk> chunks) {
        store.put(repoId, chunks);
    }

    public List<CodeChunk> get(String repoId) {
        return store.getOrDefault(repoId, List.of());
    }

    public boolean exists(String repoId) {
        return store.containsKey(repoId);
    }
}
