package com.onboarding.controller;

import com.onboarding.model.CodeChunk;
import com.onboarding.model.OverviewResponse;
import com.onboarding.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    public OverviewController(FileFilterService fileFilterService,
                               SkeletonService skeletonService,
                               SummaryService summaryService,
                               ChunkingService chunkingService,
                               RepoStore repoStore) {
        this.fileFilterService = fileFilterService;
        this.skeletonService = skeletonService;
        this.summaryService = summaryService;
        this.chunkingService = chunkingService;
        this.repoStore = repoStore;
    }

    @PostMapping("/overview")
    public ResponseEntity<?> overview(@RequestParam("file") MultipartFile file) {
        try {
            // Step 1: Extract ZIP contents
            Map<String, String> rawFiles = extractZip(file.getInputStream());

            // Step 2: Filter irrelevant files
            Map<String, String> filteredFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : rawFiles.entrySet()) {
                if (fileFilterService.shouldInclude(entry.getKey(), entry.getValue())) {
                    filteredFiles.put(entry.getKey(), entry.getValue());
                }
            }

            // Step 3: Chunk full files and store for Q&A
            String repoId = UUID.randomUUID().toString();
            List<CodeChunk> allChunks = new ArrayList<>();
            for (Map.Entry<String, String> entry : filteredFiles.entrySet()) {
                allChunks.addAll(chunkingService.chunk(entry.getKey(), entry.getValue()));
            }
            repoStore.save(repoId, allChunks);

            // Step 4: Prepare skeletons for summarization
            Map<String, String> preparedFiles = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : filteredFiles.entrySet()) {
                String prepared = skeletonService.prepare(entry.getKey(), entry.getValue());
                preparedFiles.put(entry.getKey(), prepared);
            }

            // Step 5: Summarize each file
            Map<String, String> fileSummaries = summaryService.summarizeFiles(preparedFiles);

            // Step 6: Group by directory and summarize each group
            Map<String, List<Map.Entry<String, String>>> grouped = summaryService.groupByDirectory(fileSummaries);
            Map<String, String> directorySummaries = summaryService.summarizeDirectories(grouped);

            // Step 7: Generate final repo overview
            String repoOverview = summaryService.generateRepoOverview(directorySummaries);

            return ResponseEntity.ok(new OverviewResponse(repoId, repoOverview, directorySummaries, fileSummaries));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, String> extractZip(InputStream inputStream) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
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
