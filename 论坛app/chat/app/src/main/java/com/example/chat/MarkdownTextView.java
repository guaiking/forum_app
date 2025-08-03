package com.example.chat;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;
import io.noties.markwon.Markwon;

public class MarkdownTextView extends AppCompatTextView {

    private final Markwon markwon;

    public MarkdownTextView(Context context) {
        this(context, null);
    }

    public MarkdownTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkdownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        markwon = Markwon.create(context);
    }

    public void setMarkdown(String markdown) {
        markwon.setMarkdown(this, markdown);
    }
}
