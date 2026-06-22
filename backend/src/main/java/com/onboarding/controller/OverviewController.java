package com.onboarding.controller;

import com.onboarding.model.*;
import com.onboarding.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OverviewController {

    private final FileFilterService fileFilterService;
    private final SkeletonService skeletonService;
    private final SummaryService summaryService;
    private final ChunkingService chunkingService;
    private final RepoStore repoStore;
    private final JobStore jobStore;

    private final ExecutorService jobExecutor = Executors.newFixedThreadPool(3);

    public OverviewController(FileFilterService fileFilterService,
                               SkeletonService skeletonService,
                               SummaryService summaryService,
                               ChunkingService chunkingService,
                               RepoStore repoStore,
                               JobStore jobStore) {
        this.fileFilterService = fileFilterService;
        this.skeletonService = skeletonService;
        this.summaryService = summaryService;
        this.chunkingService = chunkingService;
        this.repoStore = repoStore;
        this.jobStore = jobStore;
    }

    // Step 1: Upload ZIP → returns jobId immediately
    @PostMapping("/overview")
    public ResponseEntity<?> startOverview(@RequestParam("file") MultipartFile file) {
        try {
            String jobId = UUID.randomUUID().toString();
            AnalysisJob job = new AnalysisJob();
            jobStore.save(jobId, job);

            byte[] fileBytes = file.getBytes();

            jobExecutor.submit(() -> runAnalysis(jobId, fileBytes));

            return ResponseEntity.ok(Map.of("jobId", jobId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Step 2: Poll for job status
    @GetMapping("/overview/status/{jobId}")
    public ResponseEntity<?> getStatus(@PathVariable String jobId) {
        AnalysisJob job = jobStore.get(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", job.getStatus());
        response.put("step", job.getStep());

        if (job.getStatus() == JobStatus.DONE) {
            response.put("result", job.getResult());
        } else if (job.getStatus() == JobStatus.FAILED) {
            response.put("error", job.getError());
        }

        return ResponseEntity.ok(response);
    }

    private void runAnalysis(String jobId, byte[] fileBytes) {
        AnalysisJob job = jobStore.get(jobId);
        try {
            job.setStatus(JobStatus.PROCESSING);

            // Step 1: Extract ZIP
            job.setStep("Extracting files...");
            Map<String, String> rawFiles = extractZip(fileBytes);

            // Step 2: Filter
            job.setStep("Filtering files...");
            Map<String, String> filteredFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : rawFiles.entrySet()) {
                if (fileFilterService.shouldInclude(entry.getKey(), entry.getValue())) {
                    filteredFiles.put(entry.getKey(), entry.getValue());
                }
            }

            // Step 3: Chunk for Q&A
            job.setStep("Chunking files for Q&A...");
            String repoId = UUID.randomUUID().toString();
            List<CodeChunk> allChunks = new ArrayList<>();
            for (Map.Entry<String, String> entry : filteredFiles.entrySet()) {
                allChunks.addAll(chunkingService.chunk(entry.getKey(), entry.getValue()));
            }
            repoStore.save(repoId, allChunks);

            // Step 4: Skeleton extraction
            job.setStep("Extracting code skeletons...");
            Map<String, String> preparedFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : filteredFiles.entrySet()) {
                preparedFiles.put(entry.getKey(), skeletonService.prepare(entry.getKey(), entry.getValue()));
            }

            // Step 5: Summarize files
            job.setStep("Summarizing files (this may take a while)...");
            Map<String, String> fileSummaries = summaryService.summarizeFiles(preparedFiles);

            // Step 6: Directory summaries
            job.setStep("Generating module summaries...");
            Map<String, List<Map.Entry<String, String>>> grouped = summaryService.groupByDirectory(fileSummaries);
            Map<String, String> directorySummaries = summaryService.summarizeDirectories(grouped);

            // Step 7: Final overview
            job.setStep("Generating repository overview...");
            String repoOverview = summaryService.generateRepoOverview(directorySummaries);

            job.setResult(new OverviewResponse(repoId, repoOverview, directorySummaries, fileSummaries));
            job.setStatus(JobStatus.DONE);
            job.setStep("Done!");

        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setError(e.getMessage());
        }
    }

    private Map<String, String> extractZip(byte[] bytes) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    files.put(entry.getName(), content);
                }
                zis.closeEntry();
            }
        }
        return files;
    }
}
