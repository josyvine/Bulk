package com.bulk.app;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileProcessor implements Runnable {

    private static final String TAG = "FileProcessor";

    public interface FileProgressCallback {
        void onStart(String fileName);
        void onChunkProgress(String fileName, int currentChunk, int totalChunks);
        void onComplete(String fileName, Uri savedZipUri);
        void onError(String fileName, String errorMessage);
    }

    private final Context context;
    private final Uri fileUri;
    private final int linesPerChunk;
    private final int fontSize;
    private final String outputFormat;
    private final int targetWidth;
    private final String folderPrefix;
    private final int jpegQuality;
    private final FileProgressCallback callback;
    private final Handler mainHandler;

    public FileProcessor(Context context, Uri fileUri, int linesPerChunk, int fontSize,
                         String outputFormat, int targetWidth, String folderPrefix,
                         int jpegQuality, FileProgressCallback callback) {
        this.context = context.getApplicationContext();
        this.fileUri = fileUri;
        this.linesPerChunk = linesPerChunk;
        this.fontSize = fontSize;
        this.outputFormat = outputFormat;
        this.targetWidth = targetWidth;
        this.folderPrefix = folderPrefix;
        this.jpegQuality = jpegQuality;
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
        String originalFileName = getFileNameFromUri(context, fileUri);
        Uri savedZipUri = null;
        
        // Notify start
        postStart(originalFileName);

        try {
            // Read lines
            List<String> allLines = new ArrayList<>();
            ContentResolver resolver = context.getContentResolver();
            try (InputStream is = resolver.openInputStream(fileUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                if (is == null) {
                    throw new IOException("Unable to open input stream for Uri: " + fileUri);
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    // Replace tabs with 4 spaces, clean carriage returns
                    String sanitizedLine = line.replace("\t", "    ");
                    allLines.add(sanitizedLine);
                }
            }

            int lineCount = allLines.size();
            if (lineCount == 0) {
                // Add a dummy line so we render at least one blank image
                allLines.add("");
                lineCount = 1;
            }

            // Calculate total chunks
            int totalChunks = (lineCount + linesPerChunk - 1) / linesPerChunk;

            // Prepare base zip filename
            String baseName = originalFileName;
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                baseName = originalFileName.substring(0, dotIndex);
            }
            // Sanitize zip base name
            String sanitizedBaseName = Utils.sanitizeFilename(baseName);
            String zipFileName = sanitizedBaseName + ".zip";

            // Create target ZIP Uri before zipping
            savedZipUri = StorageHelper.createZipFileUri(context, zipFileName, folderPrefix);

            // Stream ZIP directly to the output stream
            try (java.io.OutputStream os = StorageHelper.openOutputStream(context, savedZipUri);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                
                for (int i = 0; i < totalChunks; i++) {
                    final int chunkIndex = i;
                    postChunkProgress(originalFileName, chunkIndex + 1, totalChunks);

                    // Extract chunk lines
                    int startLine = chunkIndex * linesPerChunk;
                    int endLine = Math.min(startLine + linesPerChunk, lineCount);
                    List<String> chunkLines = allLines.subList(startLine, endLine);

                    try {
                        // Render Bitmap
                        Bitmap bitmap = ImageRenderer.renderChunk(
                                chunkLines,
                                startLine + 1, // line numbers are 1-indexed
                                targetWidth,
                                fontSize,
                                outputFormat,
                                jpegQuality
                        );

                        // Zip entry naming: "fileName chunks 1.jpg"
                        String imageExt = outputFormat.equalsIgnoreCase("PNG") ? ".png" : ".jpg";
                        String entryName = sanitizedBaseName + " chunks " + (chunkIndex + 1) + imageExt;

                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zos.putNextEntry(zipEntry);

                        Bitmap.CompressFormat compressFormat = outputFormat.equalsIgnoreCase("PNG") 
                                ? Bitmap.CompressFormat.PNG 
                                : Bitmap.CompressFormat.JPEG;
                        
                        // Compress Bitmap directly to the ZIP output stream (No intermediate memory byte array!)
                        bitmap.compress(compressFormat, jpegQuality, zos);
                        zos.closeEntry();

                        bitmap.recycle(); // Recycle immediately to avoid OOM

                        Log.d(TAG, "Rendered and compressed chunk " + (chunkIndex + 1) + "/" + totalChunks + " for " + originalFileName);

                    } catch (Throwable t) {
                        Log.e(TAG, "Error rendering chunk " + (chunkIndex + 1) + " of " + originalFileName, t);
                        throw t;
                    }
                }
            }

            // Notify completion
            postComplete(originalFileName, savedZipUri);

        } catch (Throwable t) {
            // Delete incomplete or corrupted ZIP on failure
            if (savedZipUri != null) {
                StorageHelper.deleteFile(context, savedZipUri);
            }
            Log.e(TAG, "Processing failed for " + originalFileName, t);
            postError(originalFileName, t.getMessage() != null ? t.getMessage() : t.toString());
        }
    }

    // Helper utilities to retrieve files metadata
    public static String getFileNameFromUri(Context context, Uri uri) {
        String name = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (colIndex != -1) {
                        name = cursor.getString(colIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (name == null) {
            name = uri.getPath();
            int cut = name.lastIndexOf('/');
            if (cut != -1) {
                name = name.substring(cut + 1);
            }
        }
        return name;
    }

    public static long getFileSizeFromUri(Context context, Uri uri) {
        long size = 0;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int colIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (colIndex != -1) {
                        size = cursor.getLong(colIndex);
                    }
                }
            } catch (Exception ignored) {}
        }
        return size;
    }

    // Main-thread posting wrappers
    private void postStart(final String name) {
        mainHandler.post(() -> {
            if (callback != null) callback.onStart(name);
        });
    }

    private void postChunkProgress(final String name, final int chunk, final int total) {
        mainHandler.post(() -> {
            if (callback != null) callback.onChunkProgress(name, chunk, total);
        });
    }

    private void postComplete(final String name, final Uri uri) {
        mainHandler.post(() -> {
            if (callback != null) callback.onComplete(name, uri);
        });
    }

    private void postError(final String name, final String error) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(name, error);
        });
    }
}