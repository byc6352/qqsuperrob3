package com.byc.qqsuperrob3;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;


public class SeekBarPreference extends DialogPreference {
    private SeekBar seekBar;
    private TextView textView;
    private String hintText = "²ð¿ªºì°ü", prefKind;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_seekbar);

        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            if (attr.equalsIgnoreCase("pref_kind")) {
                prefKind = attrs.getAttributeValue(i);
                break;
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        SharedPreferences pref = getSharedPreferences();

        int delay = pref.getInt(prefKind, 0);
        this.seekBar = (SeekBar) view.findViewById(R.id.delay_seekBar);
        this.seekBar.setProgress(delay);

        this.textView = (TextView) view.findViewById(R.id.pref_seekbar_textview);
        if (delay == 0) {
            this.textView.setText("Á¢¼´" + hintText);
        } else {
            this.textView.setText("ÑÓ³Ù" + delay + "ºÁÃë" + hintText);
        }

        this.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 0) {
                    textView.setText("Á¢¼´" + hintText);
                } else {
                    textView.setText("ÑÓ³Ù" + i + "ºÁÃë" + hintText);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            SharedPreferences.Editor editor = getEditor();
            editor.putInt(prefKind, this.seekBar.getProgress());
            editor.commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
