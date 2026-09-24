package com.bulk.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SettingsManager settingsManager;
    private SelectedFileAdapter adapter;

    // UI components - Dashboard
    private View containerConvert;
    private View containerSettings;
    private View containerExplore;

    private MaterialButton btnSelectFiles;
    private MaterialButton btnGenerateScreenshots;
    private RecyclerView rvSelectedFiles;
    private View tvEmptyState;

    // UI components - Settings
    private EditText etLinesPerChunk;
    private EditText etFontSize;
    private EditText etScreenshotWidth;
    private EditText etFolderPrefix;
    private RadioGroup rgOutputFormat;
    private RadioButton rbJpg;
    private RadioButton rbPng;
    private View layoutJpegQuality;
    private TextView tvJpegQuality;
    private SeekBar sbJpegQuality;

    // UI components - Explore
    private MaterialButton btnOpenDocuments;

    // State list
    private final List<SelectedFile> selectedFiles = new ArrayList<>();
    private final List<Uri> selectedUris = new ArrayList<>();

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    List<Uri> uris = new ArrayList<>();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            uris.add(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        uris.add(data.getData());
                    }
                    loadSelectedFilesMetadata(uris);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Google Mobile Ads SDK
        try {
            MobileAds.initialize(this, initializationStatus -> {});
            AdView mAdView = findViewById(R.id.adView);
            if (mAdView != null) {
                AdRequest adRequest = new AdRequest.Builder().build();
                mAdView.loadAd(adRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        settingsManager = new SettingsManager(this);

        // Bind layout views
        containerConvert = findViewById(R.id.container_convert);
        containerSettings = findViewById(R.id.container_settings);
        containerExplore = findViewById(R.id.container_explore);

        btnSelectFiles = findViewById(R.id.btn_select_files);
        btnGenerateScreenshots = findViewById(R.id.btn_generate_screenshots);
        rvSelectedFiles = findViewById(R.id.rv_selected_files);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        etLinesPerChunk = findViewById(R.id.et_lines_per_chunk);
        etFontSize = findViewById(R.id.et_font_size);
        etScreenshotWidth = findViewById(R.id.et_screenshot_width);
        etFolderPrefix = findViewById(R.id.et_folder_prefix);
        rgOutputFormat = findViewById(R.id.rg_output_format);
        rbJpg = findViewById(R.id.rb_jpg);
        rbPng = findViewById(R.id.rb_png);
        layoutJpegQuality = findViewById(R.id.layout_jpeg_quality);
        tvJpegQuality = findViewById(R.id.tv_jpeg_quality);
        sbJpegQuality = findViewById(R.id.sb_jpeg_quality);

        btnOpenDocuments = findViewById(R.id.btn_open_pictures);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        // Configure RecyclerView
        rvSelectedFiles.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SelectedFileAdapter();
        rvSelectedFiles.setAdapter(adapter);

        // Bottom Navigation listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_convert) {
                containerConvert.setVisibility(View.VISIBLE);
                containerSettings.setVisibility(View.GONE);
                containerExplore.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.menu_settings) {
                containerConvert.setVisibility(View.GONE);
                containerSettings.setVisibility(View.VISIBLE);
                containerExplore.setVisibility(View.GONE);
                return true;
            } else if (id == R.id.menu_explore) {
                containerConvert.setVisibility(View.GONE);
                containerSettings.setVisibility(View.GONE);
                containerExplore.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        // Setup File Picker Button
        btnSelectFiles.setOnClickListener(v -> {
            try {
                filePickerLauncher.launch(FilePickerHelper.createPickTextFilesIntent());
            } catch (Exception e) {
                Toast.makeText(this, "File Picker Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Setup Generate Button
        btnGenerateScreenshots.setOnClickListener(v -> startScreenshotGeneration());

        // Setup Open Directory Button
        btnOpenDocuments.setOnClickListener(v -> openOutputDirectory());

        // Initialize and bind settings fields
        initSettingsFields();

        // Android 9- storage permissions prompt
        checkAndRequestPermissions();
    }

    private void initSettingsFields() {
        // Load settings to fields
        etLinesPerChunk.setText(String.valueOf(settingsManager.getLinesPerChunk()));
        etFontSize.setText(String.valueOf(settingsManager.getFontSize()));
        etScreenshotWidth.setText(String.valueOf(settingsManager.getTargetWidth()));
        etFolderPrefix.setText(settingsManager.getFolderPrefix());

        if ("PNG".equalsIgnoreCase(settingsManager.getOutputFormat())) {
            rbPng.setChecked(true);
            layoutJpegQuality.setVisibility(View.GONE);
        } else {
            rbJpg.setChecked(true);
            layoutJpegQuality.setVisibility(View.VISIBLE);
        }

        sbJpegQuality.setProgress(settingsManager.getJpegQuality());
        tvJpegQuality.setText(String.format(Locale.US, getString(R.string.label_jpeg_quality), settingsManager.getJpegQuality()));

        // Add persistent listeners
        etLinesPerChunk.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                try {
                    int val = Integer.parseInt(text.trim());
                    if (val > 0) {
                        settingsManager.setLinesPerChunk(val);
                        recalculateChunks();
                    }
                } catch (Exception ignored) {}
            }
        });

        etFontSize.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                try {
                    int val = Integer.parseInt(text.trim());
                    if (val > 0) {
                        settingsManager.setFontSize(val);
                    }
                } catch (Exception ignored) {}
            }
        });

        etScreenshotWidth.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                try {
                    int val = Integer.parseInt(text.trim());
                    if (val > 0) {
                        settingsManager.setTargetWidth(val);
                    }
                } catch (Exception ignored) {}
            }
        });

        etFolderPrefix.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(String text) {
                if (!text.trim().isEmpty()) {
                    settingsManager.setFolderPrefix(text);
                }
            }
        });

        rgOutputFormat.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_jpg) {
                settingsManager.setOutputFormat("JPG");
                layoutJpegQuality.setVisibility(View.VISIBLE);
            } else {
                settingsManager.setOutputFormat("PNG");
                layoutJpegQuality.setVisibility(View.GONE);
            }
        });

        sbJpegQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actual = Math.max(10, progress); // prevent 0% compression quality
                settingsManager.setJpegQuality(actual);
                tvJpegQuality.setText(String.format(Locale.US, getString(R.string.label_jpeg_quality), actual));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void loadSelectedFilesMetadata(final List<Uri> uris) {
        btnSelectFiles.setEnabled(false);
        btnGenerateScreenshots.setEnabled(false);

        new Thread(() -> {
            final List<SelectedFile> metadataList = new ArrayList<>();
            final int linesPerChunk = settingsManager.getLinesPerChunk();

            for (Uri uri : uris) {
                String name = FileProcessor.getFileNameFromUri(MainActivity.this, uri);
                long size = FileProcessor.getFileSizeFromUri(MainActivity.this, uri);

                // Compute exact lines count
                int lines = 0;
                try (InputStream is = getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    if (is != null) {
                        while (reader.readLine() != null) {
                            lines++;
                        }
                    }
                } catch (Exception ignored) {}

                if (lines == 0) {
                    lines = 1;
                }

                int chunks = (lines + linesPerChunk - 1) / linesPerChunk;
                metadataList.add(new SelectedFile(uri, name, size, lines, chunks));
            }

            runOnUiThread(() -> {
                selectedFiles.clear();
                selectedFiles.addAll(metadataList);
                
                selectedUris.clear();
                for (SelectedFile sf : metadataList) {
                    selectedUris.add(sf.getUri());
                }

                adapter.setFiles(selectedFiles);
                btnSelectFiles.setEnabled(true);

                if (selectedFiles.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    btnGenerateScreenshots.setEnabled(false);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    btnGenerateScreenshots.setEnabled(true);
                }
            });
        }).start();
    }

    private void recalculateChunks() {
        int linesPerChunk = settingsManager.getLinesPerChunk();
        List<SelectedFile> updatedList = new ArrayList<>();
        for (SelectedFile sf : selectedFiles) {
            int chunkCount = (sf.getLineCount() + linesPerChunk - 1) / linesPerChunk;
            updatedList.add(new SelectedFile(sf.getUri(), sf.getName(), sf.getSize(), sf.getLineCount(), chunkCount));
        }
        selectedFiles.clear();
        selectedFiles.addAll(updatedList);
        adapter.setFiles(selectedFiles);
    }

    private void startScreenshotGeneration() {
        if (selectedUris.isEmpty()) {
            Toast.makeText(this, "No files selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enforce storage permission check on Android 9 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                checkAndRequestPermissions();
                Toast.makeText(this, "Storage permission required to save outputs.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final ProgressDialogFragment progressDialog = ProgressDialogFragment.newInstance();
        progressDialog.show(getSupportFragmentManager(), "progress_dialog");

        ScreenshotGenerator generator = new ScreenshotGenerator(
                this,
                selectedUris,
                settingsManager,
                new ScreenshotGenerator.GeneratorListener() {
                    @Override
                    public void onStart(int totalFiles) {
                        progressDialog.updateProgress("Processing files...", 0, 0, 0, totalFiles);
                    }

                    @Override
                    public void onProgress(String currentFileName, int currentChunk, int totalChunks, int processedFiles, int totalFiles) {
                        progressDialog.updateProgress(currentFileName, currentChunk, totalChunks, processedFiles, totalFiles);
                    }

                    @Override
                    public void onFileCompleted(String fileName, Uri savedZipUri) {
                        // Success log hooks
                    }

                    @Override
                    public void onAllCompleted(int totalSuccessCount, List<Uri> savedUris) {
                        try {
                            progressDialog.dismissAllowingStateLoss();
                        } catch (Exception ignored) {}

                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("Generation Complete")
                                .setMessage(String.format(Locale.US, "Successfully split and compressed %d file(s) inside /Documents/BulkScreenshotSplitter/%s/", totalSuccessCount, settingsManager.getFolderPrefix()))
                                .setPositiveButton("Open Directory", (dialog, which) -> openOutputDirectory())
                                .setNegativeButton("Done", (dialog, which) -> {
                                    // Reset Dashboard
                                    selectedFiles.clear();
                                    selectedUris.clear();
                                    adapter.setFiles(selectedFiles);
                                    tvEmptyState.setVisibility(View.VISIBLE);
                                    btnGenerateScreenshots.setEnabled(false);
                                })
                                .show();
                    }

                    @Override
                    public void onError(String fileName, String errorMessage) {
                        Toast.makeText(MainActivity.this, "Error in file: " + fileName + "\n" + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        );

        generator.start();
    }

    private void openOutputDirectory() {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "BulkScreenshotSplitter");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Method 1: Use DocumentsContract to open the specific subfolder directly (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Uri docUri = DocumentsContract.buildChildDocumentsUri(
                        "com.android.externalstorage.documents",
                        "primary:Documents/BulkScreenshotSplitter"
                );
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            } catch (Exception ignored) {}
        }

        // Method 2: Open system Documents UI
        try {
            Intent fallbackIntent = getPackageManager().getLaunchIntentForPackage("com.android.documentsui");
            if (fallbackIntent != null) {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallbackIntent);
                return;
            }
        } catch (Exception ignored) {}

        // Method 3: Generic folder view intent
        try {
            Intent altIntent = new Intent(Intent.ACTION_VIEW);
            altIntent.setDataAndType(Uri.parse(folder.getAbsolutePath()), "resource/folder");
            altIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(altIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Files saved in: Documents/BulkScreenshotSplitter/" + settingsManager.getFolderPrefix() + "/", Toast.LENGTH_LONG).show();
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 100);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Unable to save files on older Android versions.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Helper text-watcher interface
    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTextChanged(s.toString());
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
        public abstract void onTextChanged(String text);
    }
}