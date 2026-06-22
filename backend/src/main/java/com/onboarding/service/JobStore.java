package com.onboarding.service;

import com.onboarding.model.AnalysisJob;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobStore {
    private final Map<String, AnalysisJob> jobs = new ConcurrentHashMap<>();

    public void save(String jobId, AnalysisJob job) {
        jobs.put(jobId, job);
    }

    public AnalysisJob get(String jobId) {
        return jobs.get(jobId);
    }
}
