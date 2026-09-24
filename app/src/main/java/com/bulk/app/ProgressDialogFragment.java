package com.bulk.app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.util.Locale;

public class ProgressDialogFragment extends DialogFragment {

    private TextView tvProgressFile;
    private TextView tvProgressChunk;
    private TextView tvProgressOverall;

    private String pendingFile = "-";
    private int pendingChunk = -1;
    private int pendingTotalChunks = -1;
    private int pendingProcessed = -1;
    private int pendingTotalFiles = -1;

    public static ProgressDialogFragment newInstance() {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setCancelable(false);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvProgressFile = view.findViewById(R.id.tv_progress_file);
        tvProgressChunk = view.findViewById(R.id.tv_progress_chunk);
        tvProgressOverall = view.findViewById(R.id.tv_progress_overall);

        // Apply any values received before the view was fully created
        updateUI(pendingFile, pendingChunk, pendingTotalChunks, pendingProcessed, pendingTotalFiles);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    public void updateProgress(final String fileName, final int currentChunk, final int totalChunks, final int processedFiles, final int totalFiles) {
        pendingFile = fileName;
        pendingChunk = currentChunk;
        pendingTotalChunks = totalChunks;
        pendingProcessed = processedFiles;
        pendingTotalFiles = totalFiles;

        if (getView() != null) {
            updateUI(fileName, currentChunk, totalChunks, processedFiles, totalFiles);
        }
    }

    private void updateUI(String fileName, int currentChunk, int totalChunks, int processedFiles, int totalFiles) {
        if (tvProgressFile != null) {
            tvProgressFile.setText(String.format("File: %s", fileName));
        }
        if (tvProgressChunk != null) {
            if (currentChunk >= 0 && totalChunks >= 0) {
                tvProgressChunk.setText(String.format(Locale.US, "Chunk: %d / %d", currentChunk, totalChunks));
            } else {
                tvProgressChunk.setText("Chunk: - / -");
            }
        }
        if (tvProgressOverall != null) {
            if (processedFiles >= 0 && totalFiles >= 0) {
                tvProgressOverall.setText(String.format(Locale.US, "Overall: %d / %d files", processedFiles, totalFiles));
            } else {
                tvProgressOverall.setText("Overall: - / - files");
            }
        }
    }
}
