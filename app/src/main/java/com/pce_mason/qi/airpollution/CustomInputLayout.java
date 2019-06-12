package com.pce_mason.qi.airpollution;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.util.AttributeSet;
import android.widget.EditText;

public class CustomInputLayout extends TextInputLayout {
    public CustomInputLayout(Context context) {
        super(context);
    }

    public CustomInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        clearEditTextColorfilter();
    }

    @Override
    public void setError(@Nullable CharSequence error) {
        super.setError(error);
        clearEditTextColorfilter();
    }

    private void clearEditTextColorfilter() {
        EditText editText = getEditText();
        if (editText != null) {
            Drawable background = editText.getBackground();
            if (background != null) {
                background.clearColorFilter();
            }
        }
    }
}