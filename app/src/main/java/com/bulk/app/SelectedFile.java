package com.bulk.app;

import android.net.Uri;

public class SelectedFile {
    private final Uri uri;
    private final String name;
    private final long size;
    private final int lineCount;
    private final int chunkCount;

    public SelectedFile(Uri uri, String name, long size, int lineCount, int chunkCount) {
        this.uri = uri;
        this.name = name;
        this.size = size;
        this.lineCount = lineCount;
        this.chunkCount = chunkCount;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public int getLineCount() {
        return lineCount;
    }

    public int getChunkCount() {
        return chunkCount;
    }
}
