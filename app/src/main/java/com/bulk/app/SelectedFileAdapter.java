package com.bulk.app;

import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectedFileAdapter extends RecyclerView.Adapter<SelectedFileAdapter.ViewHolder> {

    private final List<SelectedFile> filesList = new ArrayList<>();

    public void setFiles(List<SelectedFile> files) {
        filesList.clear();
        if (files != null) {
            filesList.addAll(files);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectedFile file = filesList.get(position);
        
        holder.tvFileName.setText(file.getName());
        
        String sizeStr = Formatter.formatShortFileSize(holder.itemView.getContext(), file.getSize());
        holder.tvFileSize.setText(sizeStr);
        
        holder.tvTotalLines.setText(String.format(Locale.US, "%d lines", file.getLineCount()));
        holder.tvTotalChunks.setText(String.format(Locale.US, "%d chunks", file.getChunkCount()));
    }

    @Override
    public int getItemCount() {
        return filesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFileName;
        final TextView tvFileSize;
        final TextView tvTotalLines;
        final TextView tvTotalChunks;

        ViewHolder(View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            tvTotalLines = itemView.findViewById(R.id.tv_total_lines);
            tvTotalChunks = itemView.findViewById(R.id.tv_total_chunks);
        }
    }
}
