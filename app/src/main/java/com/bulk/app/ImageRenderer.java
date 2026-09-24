package com.bulk.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImageRenderer {

    /**
     * Converts a list of text lines into an Android Bitmap.
     * Adheres strictly to the color, spacing, and font specifications.
     */
    public static Bitmap renderChunk(List<String> lines, int startLineNumber, int width, int fontSize, String format, int quality) {
        // Initialize paint for measuring and rendering
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setTextSize(fontSize);
        textPaint.setColor(Color.rgb(226, 232, 240)); // #e2e8f0

        Paint lineNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineNumberPaint.setTypeface(Typeface.MONOSPACE);
        lineNumberPaint.setTextSize(fontSize);
        lineNumberPaint.setColor(Color.rgb(100, 116, 139)); // #64748b

        // Calculate available width for code text
        // Line number starts at 10px, code text starts at 65px
        int codeTextX = 65;
        int paddingRight = 10;
        int availableTextWidth = width - codeTextX - paddingRight;
        if (availableTextWidth < 100) {
            availableTextWidth = 100; // safety fallback
        }

        // Preprocess lines to wrap long text
        List<String> textToDraw = new ArrayList<>();
        List<Integer> lineNumbersToDraw = new ArrayList<>();

        int currentLineNum = startLineNumber;
        for (String line : lines) {
            List<String> wrapped = wrapLine(line, textPaint, availableTextWidth);
            boolean isFirstSegment = true;
            for (String segment : wrapped) {
                textToDraw.add(segment);
                lineNumbersToDraw.add(isFirstSegment ? currentLineNum : -1);
                isFirstSegment = false;
            }
            currentLineNum++;
        }

        // Layout measurements
        int linesToRender = textToDraw.size();
        if (linesToRender == 0) {
            linesToRender = 1;
            textToDraw.add("");
            lineNumbersToDraw.add(startLineNumber);
        }

        int verticalPadding = 10; // top/bottom padding
        int lineHeight = fontSize + 4;
        int bitmapHeight = (linesToRender * lineHeight) + (verticalPadding * 2);

        // Prevent invalid dimensions
        if (width <= 0) width = 1024;
        if (bitmapHeight <= 0) bitmapHeight = 100;

        // Create the Bitmap with ARGB_8888 for high-quality rendering
        Bitmap bitmap = Bitmap.createBitmap(width, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fill background Color: RGB(11, 15, 25) / #0b0f19
        canvas.drawColor(Color.rgb(11, 15, 25));

        // Render each wrapped line
        for (int i = 0; i < linesToRender; i++) {
            int yPosition = verticalPadding + (i * lineHeight) + fontSize;

            // Draw line numbers
            int lineNum = lineNumbersToDraw.get(i);
            String lineNumStr;
            if (lineNum != -1) {
                lineNumStr = String.format(Locale.US, "%4d | ", lineNum);
            } else {
                lineNumStr = "     | ";
            }
            canvas.drawText(lineNumStr, 10, yPosition, lineNumberPaint);

            // Draw text code/log
            String content = textToDraw.get(i);
            canvas.drawText(content, codeTextX, yPosition, textPaint);
        }

        return bitmap;
    }

    /**
     * Character-level text wrapping to fit perfectly in available width
     * without breaking the layout structure of standard logs/code text.
     */
    private static List<String> wrapLine(String text, Paint paint, float maxWidth) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            wrapped.add("");
            return wrapped;
        }

        float textWidth = paint.measureText(text);
        if (textWidth <= maxWidth) {
            wrapped.add(text);
            return wrapped;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            if (paint.measureText(sb.toString()) > maxWidth) {
                // Remove the last character that caused overflow
                sb.setLength(sb.length() - 1);
                wrapped.add(sb.toString());
                sb.setLength(0);
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            wrapped.add(sb.toString());
        }

        return wrapped;
    }
}
