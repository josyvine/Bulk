package com.bulk.app;

public class Utils {
    /**
     * Sanitizes a filename by replacing any characters that are not letters,
     * numbers, spaces, underscores, or dashes with an underscore.
     * Keeps the original extension intact.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_file";
        }
        
        int dotIndex = filename.lastIndexOf('.');
        String name = dotIndex >= 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex >= 0 ? filename.substring(dotIndex) : "";
        
        // Replace everything except letters, numbers, spaces, underscores, dashes
        String sanitizedName = name.replaceAll("[^a-zA-Z0-9 _-]", "_");
        
        // Prevent empty names
        if (sanitizedName.trim().isEmpty()) {
            sanitizedName = "file";
        }
        
        return sanitizedName + extension;
    }
}
