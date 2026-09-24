package com.bulk.app;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "bulk_screenshot_pref";
    private static final String KEY_LINES_PER_CHUNK = "lines_per_chunk";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_OUTPUT_FORMAT = "output_format";
    private static final String KEY_TARGET_WIDTH = "target_width";
    private static final String KEY_FOLDER_PREFIX = "folder_prefix";
    private static final String KEY_JPEG_QUALITY = "jpeg_quality";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getLinesPerChunk() {
        return prefs.getInt(KEY_LINES_PER_CHUNK, 500);
    }

    public void setLinesPerChunk(int lines) {
        prefs.edit().putInt(KEY_LINES_PER_CHUNK, lines).apply();
    }

    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, 12);
    }

    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply();
    }

    public String getOutputFormat() {
        return prefs.getString(KEY_OUTPUT_FORMAT, "JPG");
    }

    public void setOutputFormat(String format) {
        prefs.edit().putString(KEY_OUTPUT_FORMAT, format).apply();
    }

    public int getTargetWidth() {
        return prefs.getInt(KEY_TARGET_WIDTH, 1024);
    }

    public void setTargetWidth(int width) {
        prefs.edit().putInt(KEY_TARGET_WIDTH, width).apply();
    }

    public String getFolderPrefix() {
        return prefs.getString(KEY_FOLDER_PREFIX, "bulk screenshot");
    }

    public void setFolderPrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "bulk screenshot";
        }
        prefs.edit().putString(KEY_FOLDER_PREFIX, prefix).apply();
    }

    public int getJpegQuality() {
        return prefs.getInt(KEY_JPEG_QUALITY, 80);
    }

    public void setJpegQuality(int quality) {
        prefs.edit().putInt(KEY_JPEG_QUALITY, quality).apply();
    }
}
