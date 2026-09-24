package com.bulk.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogReportManager {
    private static final String TAG = "LogReportManager";
    private static final long ONE_HOUR_MS = 60 * 60 * 1000L; // 1 hour

    /**
     * Deletes log files in the public directory that are older than an hour.
     */
    public static void cleanOldLogs(Context context, String folderPrefix) {
        try {
            String sanitizedPrefix = folderPrefix.replaceAll("[^a-zA-Z0-9 _-]", "_");
            if (sanitizedPrefix.trim().isEmpty()) {
                sanitizedPrefix = "bulk_screenshot";
            }
            long now = System.currentTimeMillis();

            // 1. Clean using java.io.File (for API < 29 or with legacy storage enabled)
            try {
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File targetDir = new File(documentsDir, "BulkScreenshotSplitter/" + sanitizedPrefix);
                if (targetDir.exists()) {
                    File[] files = targetDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().startsWith("chunk_report_") && file.getName().endsWith(".txt")) {
                                long age = now - file.lastModified();
                                if (age > ONE_HOUR_MS) {
                                    boolean deleted = file.delete();
                                    Log.d(TAG, "Deleted old log file (File API): " + file.getName() + " -> " + deleted);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Non-fatal: cleanOldLogs File API failed: " + e.getMessage());
            }

            // 2. Clean using MediaStore (for API >= 29)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    ContentResolver resolver = context.getContentResolver();
                    Uri externalUri = MediaStore.Files.getContentUri("external");
                    String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?";
                    String[] selectionArgs = new String[] {
                        Environment.DIRECTORY_DOCUMENTS + "/BulkScreenshotSplitter/" + sanitizedPrefix + "/",
                        "chunk_report_%.txt"
                    };

                    String[] projection = new String[] {
                        MediaStore.MediaColumns._ID,
                        MediaStore.MediaColumns.DATE_MODIFIED,
                        MediaStore.MediaColumns.DISPLAY_NAME
                    };

                    try (android.database.Cursor cursor = resolver.query(externalUri, projection, selection, selectionArgs, null)) {
                        if (cursor != null) {
                            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                            int dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
                            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);

                            while (cursor.moveToNext()) {
                                long id = cursor.getLong(idColumn);
                                long dateModifiedSec = cursor.getLong(dateModifiedColumn);
                                String displayName = cursor.getString(nameColumn);
                                
                                long ageMs = now - (dateModifiedSec * 1000L);
                                if (ageMs > ONE_HOUR_MS) {
                                    Uri fileUri = android.content.ContentUris.withAppendedId(externalUri, id);
                                    int deletedRows = resolver.delete(fileUri, null, null);
                                    Log.d(TAG, "Deleted old log file (MediaStore): " + displayName + " -> " + (deletedRows > 0));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error cleaning logs via MediaStore: " + e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in cleanOldLogs: " + e.getMessage(), e);
        }
    }

    /**
     * Saves a log report to the public directory.
     */
    public static void saveReport(Context context, String folderPrefix, String fileName, int chunkIndex, boolean isSuccess, String details, Throwable error) {
        // Clean old logs first
        cleanOldLogs(context, folderPrefix);

        try {
            String sanitizedPrefix = folderPrefix.replaceAll("[^a-zA-Z0-9 _-]", "_");
            if (sanitizedPrefix.trim().isEmpty()) {
                sanitizedPrefix = "bulk_screenshot";
            }

            long timestamp = System.currentTimeMillis();
            String safeFileName = (fileName != null ? fileName : "unknown").replaceAll("[^a-zA-Z0-9 _-]", "_");
            String reportFileName;
            if (chunkIndex >= 0) {
                reportFileName = "chunk_report_" + safeFileName + "_chunk" + chunkIndex + "_" + timestamp + ".txt";
            } else {
                reportFileName = "chunk_report_" + safeFileName + "_overall_" + timestamp + ".txt";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            String formattedDate = sdf.format(new Date(timestamp));

            StringBuilder sb = new StringBuilder();
            sb.append("========================================\n");
            sb.append("          CHUNK CREATION REPORT         \n");
            sb.append("========================================\n");
            sb.append("Time: ").append(formattedDate).append("\n");
            sb.append("File: ").append(fileName != null ? fileName : "unknown").append("\n");
            if (chunkIndex >= 0) {
                sb.append("Chunk Index: ").append(chunkIndex).append("\n");
            } else {
                sb.append("Scope: Overall Processing\n");
            }
            sb.append("Status: ").append(isSuccess ? "SUCCESS" : "CRASH/FAILED").append("\n");
            sb.append("Details: ").append(details).append("\n");

            if (error != null) {
                sb.append("Exception: ").append(error.toString()).append("\n");
                sb.append("Stack Trace:\n");
                for (StackTraceElement ste : error.getStackTrace()) {
                    sb.append("    at ").append(ste.toString()).append("\n");
                }
                if (error.getCause() != null) {
                    sb.append("Caused by: ").append(error.getCause().toString()).append("\n");
                }
            }
            sb.append("========================================\n");
            String reportContent = sb.toString();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, reportFileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/BulkScreenshotSplitter/" + sanitizedPrefix);

                Uri externalUri = MediaStore.Files.getContentUri("external");
                Uri fileUri = resolver.insert(externalUri, values);
                if (fileUri != null) {
                    try (java.io.OutputStream os = resolver.openOutputStream(fileUri);
                         OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        osw.write(reportContent);
                        osw.flush();
                        Log.d(TAG, "Saved chunk report (MediaStore): " + fileUri.toString());
                    }
                } else {
                    Log.e(TAG, "Failed to insert MediaStore entry for " + reportFileName);
                }
            } else {
                File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                File targetDir = new File(documentsDir, "BulkScreenshotSplitter/" + sanitizedPrefix);
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                File reportFile = new File(targetDir, reportFileName);
                try (FileOutputStream fos = new FileOutputStream(reportFile);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                    osw.write(reportContent);
                    osw.flush();
                    Log.d(TAG, "Saved chunk report (File API): " + reportFile.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving report: " + e.getMessage(), e);
        }
    }
}