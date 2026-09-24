package com.bulk.app;

import android.content.Intent;
import android.os.Build;

public class FilePickerHelper {

    /**
     * Builds a standard open-document Intent configured for multi-selection
     * of text and code source files.
     */
    public static Intent createPickTextFilesIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Use broad selector with extra mimes to support logs/code files
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] mimeTypes = {
                "text/plain",
                "text/html",
                "text/css",
                "text/xml",
                "application/javascript",
                "application/json",
                "application/xml",
                "application/x-javascript"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }
        
        return intent;
    }
}
