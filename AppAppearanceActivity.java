package com.example.flex_music;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class AppAppearanceActivity extends AppCompatActivity {

    RadioGroup themeGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before calling super
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_appearance);

        themeGroup = findViewById(R.id.radioGroup);

        if (isDark) {
            ((RadioButton) findViewById(R.id.radioDark)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radioLight)).setChecked(true);
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean enableDark = checkedId == R.id.radioDark;
            prefs.edit().putBoolean("dark_mode", enableDark).apply();
            AppCompatDelegate.setDefaultNightMode(
                    enableDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            recreate();
        });
    }
}
