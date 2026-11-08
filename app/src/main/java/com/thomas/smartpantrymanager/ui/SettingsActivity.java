package com.thomas.smartpantrymanager.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.PantryRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Screen 5 — Settings.
 *
 * Preferences live in a plain SharedPreferences file ("settings") because there are only
 * two of them and neither needs to be queried or observed; the pantry list re-reads them
 * in onResume(), which is how a change here affects the badges on Screen 1.
 */
public class SettingsActivity extends AppCompatActivity {

    /** SharedPreferences file name — shared with MainActivity via these constants. */
    public static final String PREFS_NAME = "settings";
    public static final String KEY_ALERTS_ENABLED = "alerts_enabled";
    public static final String KEY_THRESHOLD_DAYS = "threshold_days";
    public static final int DEFAULT_THRESHOLD_DAYS = 3;
    public static final int MIN_THRESHOLD_DAYS = 1;
    public static final int MAX_THRESHOLD_DAYS = 30;

    private PantryRepository mRepository;
    private SharedPreferences mPrefs;

    private SwitchMaterial mAlertsSwitch;
    private TextInputLayout mThresholdLayout;
    private TextInputEditText mThresholdEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mRepository = PantryRepository.getInstance((PantryApp) getApplication());
        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_settings);

        mAlertsSwitch = findViewById(R.id.switch_alerts);
        mThresholdLayout = findViewById(R.id.field_threshold);
        mThresholdEdit = findViewById(R.id.input_threshold);

        mAlertsSwitch.setChecked(mPrefs.getBoolean(KEY_ALERTS_ENABLED, true));
        mAlertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mThresholdLayout.setError(null); // a hidden setting must not show a stale error
            }
            mPrefs.edit().putBoolean(KEY_ALERTS_ENABLED, isChecked).apply();
        });

        mThresholdEdit.setText(String.valueOf(
                mPrefs.getInt(KEY_THRESHOLD_DAYS, DEFAULT_THRESHOLD_DAYS)));
        // Validate as soon as the user leaves the field so bad input is reported in place.
        mThresholdEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateAndSaveThreshold();
            }
        });

        findViewById(R.id.btn_delete_all).setOnClickListener(v -> confirmDeleteAll());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        setupBottomNav(bottomNav, R.id.nav_settings);
    }

    @Override
    protected void onPause() {
        // Catch a value the user edited but never blurred before leaving the screen.
        validateAndSaveThreshold();
        super.onPause();
    }

    /**
     * Validates the threshold (must be an integer in 1..30) and persists it.
     *
     * @return true when the current text is valid and was saved; on failure an inline
     *         error stays on the field and nothing is written, so a broken value can
     *         never be stored and later crash the badge logic.
     */
    private boolean validateAndSaveThreshold() {
        String text = mThresholdEdit.getText() == null
                ? "" : mThresholdEdit.getText().toString().trim();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            mThresholdLayout.setError(getString(R.string.error_threshold_range,
                    MIN_THRESHOLD_DAYS, MAX_THRESHOLD_DAYS));
            return false;
        }
        if (value < MIN_THRESHOLD_DAYS || value > MAX_THRESHOLD_DAYS) {
            mThresholdLayout.setError(getString(R.string.error_threshold_range,
                    MIN_THRESHOLD_DAYS, MAX_THRESHOLD_DAYS));
            return false;
        }
        mThresholdLayout.setError(null);
        mPrefs.edit().putInt(KEY_THRESHOLD_DAYS, value).apply();
        return true;
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_all_title)
                .setMessage(R.string.confirm_delete_all_message)
                .setPositiveButton(R.string.button_delete_all, (dialog, which) ->
                        mRepository.deleteAllItems(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(SettingsActivity.this,
                                        R.string.toast_pantry_cleared, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    // -------------------------------------------------------------- navigation

    /** Selected item first, listener second — see MainActivity.setupBottomNav. */
    private void setupBottomNav(BottomNavigationView bottomNav, int selectedItemId) {
        bottomNav.setSelectedItemId(selectedItemId);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedItemId) {
                return true;
            }
            Class<?> target;
            if (id == R.id.nav_pantry) {
                target = MainActivity.class;
            } else if (id == R.id.nav_suggestions) {
                target = SuggestedRecipesActivity.class;
            } else if (id == R.id.nav_settings) {
                target = SettingsActivity.class;
            } else {
                return false;
            }
            openTopLevel(target);
            return true;
        });
    }

    private void openTopLevel(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
