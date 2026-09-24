package com.bulk.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StorageHelper {

    /**
     * Creates a file entry (URI) for the ZIP file in /Pictures/BulkScreenshotSplitter/{folderPrefix}/
     * Returns the Uri of the created file.
     */
    public static Uri createZipFileUri(Context context, String zipFileName, String folderPrefix) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        
        // Sanitize the subfolder name
        String sanitizedPrefixFolder = folderPrefix.replaceAll("[^a-zA-Z0-9 _-]", "_");
        if (sanitizedPrefixFolder.trim().isEmpty()) {
            sanitizedPrefixFolder = "bulk_screenshot";
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/zip");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BulkScreenshotSplitter/" + sanitizedPrefixFolder);

            // Accessing public folders for files is done via MediaStore.Files on API 29+
            Uri externalUri = MediaStore.Files.getContentUri("external");
            Uri fileUri = resolver.insert(externalUri, values);
            if (fileUri == null) {
                throw new IOException("Failed to create MediaStore entry for " + zipFileName);
            }
            return fileUri;
        } else {
            // Android 9 and below: Use legacy java.io.File directly
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File targetDir = new File(picturesDir, "BulkScreenshotSplitter/" + sanitizedPrefixFolder);
            
            if (!targetDir.exists()) {
                if (!targetDir.mkdirs()) {
                    throw new IOException("Failed to create output directory: " + targetDir.getAbsolutePath());
                }
            }
            
            File zipFile = new File(targetDir, zipFileName);
            return Uri.fromFile(zipFile);
        }
    }

    /**
     * Opens an OutputStream for the given Uri.
     */
    public static OutputStream openOutputStream(Context context, Uri uri) throws IOException {
        if ("content".equals(uri.getScheme())) {
            OutputStream os = context.getContentResolver().openOutputStream(uri);
            if (os == null) {
                throw new IOException("Failed to open output stream for MediaStore Uri: " + uri);
            }
            return os;
        } else {
            File file = new File(uri.getPath());
            return new FileOutputStream(file);
        }
    }

    /**
     * Deletes the file at the given Uri if it exists.
     */
    public static void deleteFile(Context context, Uri uri) {
        try {
            if ("content".equals(uri.getScheme())) {
                context.getContentResolver().delete(uri, null, null);
            } else {
                File file = new File(uri.getPath());
                if (file.exists()) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            // Ignored
        }
    }

    /**
     * Saves the byte array of a ZIP file into /Pictures/BulkScreenshotSplitter/{folderPrefix}/
     * Returns the Uri of the saved file.
     */
    public static Uri saveZipFile(Context context, String zipFileName, String folderPrefix, byte[] zipData) throws IOException {
        Uri fileUri = createZipFileUri(context, zipFileName, folderPrefix);
        try (OutputStream os = openOutputStream(context, fileUri)) {
            os.write(zipData);
        }
        return fileUri;
    }
}
