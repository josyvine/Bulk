package com.bulk.app;

import android.content.Context;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ScreenshotGenerator {

    public interface GeneratorListener {
        void onStart(int totalFiles);
        void onProgress(String currentFileName, int currentChunk, int totalChunks, int processedFiles, int totalFiles);
        void onFileCompleted(String fileName, Uri savedZipUri);
        void onAllCompleted(int totalSuccessCount, List<Uri> savedUris);
        void onError(String fileName, String errorMessage);
    }

    private final Context context;
    private final List<Uri> fileUris;
    private final SettingsManager settingsManager;
    private final GeneratorListener listener;
    private final ExecutorService executorService;

    public ScreenshotGenerator(Context context, List<Uri> fileUris, SettingsManager settingsManager, GeneratorListener listener) {
        this.context = context.getApplicationContext();
        this.fileUris = fileUris;
        this.settingsManager = settingsManager;
        this.listener = listener;
        // Limit to exactly 4 parallel workers as specified
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void start() {
        final int totalFiles = fileUris.size();
        if (totalFiles == 0) {
            if (listener != null) {
                listener.onAllCompleted(0, new ArrayList<>());
            }
            return;
        }

        if (listener != null) {
            listener.onStart(totalFiles);
        }

        final AtomicInteger completedCount = new AtomicInteger(0);
        final AtomicInteger successCount = new AtomicInteger(0);
        final List<Uri> savedUris = Collections.synchronizedList(new ArrayList<>());

        // Retrieve settings values once on start
        final int linesPerChunk = settingsManager.getLinesPerChunk();
        final int fontSize = settingsManager.getFontSize();
        final String outputFormat = settingsManager.getOutputFormat();
        final int targetWidth = settingsManager.getTargetWidth();
        final String folderPrefix = settingsManager.getFolderPrefix();
        final int jpegQuality = settingsManager.getJpegQuality();

        for (final Uri uri : fileUris) {
            FileProcessor processor = new FileProcessor(
                    context,
                    uri,
                    linesPerChunk,
                    fontSize,
                    outputFormat,
                    targetWidth,
                    folderPrefix,
                    jpegQuality,
                    new FileProcessor.FileProgressCallback() {
                        @Override
                        public void onStart(String fileName) {
                            if (listener != null) {
                                listener.onProgress(fileName, 1, 1, completedCount.get(), totalFiles);
                            }
                        }

                        @Override
                        public void onChunkProgress(String fileName, int currentChunk, int totalChunks) {
                            if (listener != null) {
                                listener.onProgress(fileName, currentChunk, totalChunks, completedCount.get(), totalFiles);
                            }
                        }

                        @Override
                        public void onComplete(String fileName, Uri savedZipUri) {
                            savedUris.add(savedZipUri);
                            successCount.incrementAndGet();
                            int finishedCount = completedCount.incrementAndGet();
                            
                            if (listener != null) {
                                listener.onFileCompleted(fileName, savedZipUri);
                                listener.onProgress(fileName, 1, 1, finishedCount, totalFiles);
                            }
                            
                            checkCompletion(finishedCount, totalFiles, successCount.get(), savedUris);
                        }

                        @Override
                        public void onError(String fileName, String errorMessage) {
                            int finishedCount = completedCount.incrementAndGet();
                            
                            if (listener != null) {
                                listener.onError(fileName, errorMessage);
                                listener.onProgress(fileName, 1, 1, finishedCount, totalFiles);
                            }
                            
                            checkCompletion(finishedCount, totalFiles, successCount.get(), savedUris);
                        }
                    }
            );

            executorService.execute(processor);
        }
    }

    private void checkCompletion(int finished, int total, int success, List<Uri> savedList) {
        if (finished == total) {
            executorService.shutdown();
            if (listener != null) {
                listener.onAllCompleted(success, savedList);
            }
        }
    }

    public void cancel() {
        executorService.shutdownNow();
    }
}
