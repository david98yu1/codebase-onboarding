package com.onboarding.model;

public class AnalysisJob {
    private JobStatus status = JobStatus.PENDING;
    private String step = "Queued...";
    private OverviewResponse result;
    private String error;

    public JobStatus getStatus() { return status; }
    public String getStep() { return step; }
    public OverviewResponse getResult() { return result; }
    public String getError() { return error; }

    public void setStatus(JobStatus status) { this.status = status; }
    public void setStep(String step) { this.step = step; }
    public void setResult(OverviewResponse result) { this.result = result; }
    public void setError(String error) { this.error = error; }
}
