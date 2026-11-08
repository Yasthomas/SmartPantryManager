package com.thomas.smartpantrymanager.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.PantryRepository;
import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.ui.adapter.PantryAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 1 — "My Pantry": the user's ingredient inventory.
 *
 * Observes {@code PantryItemDao.getAllItems()} through LiveData, so adding or editing an
 * ingredient on another screen updates this list the moment control comes back, with no
 * manual refresh code.
 */
public class MainActivity extends AppCompatActivity implements PantryAdapter.OnItemActionListener {

    private PantryRepository mRepository;
    private PantryAdapter mAdapter;
    private RecyclerView mList;
    private View mEmptyState;

    private List<PantryItem> mItems = new ArrayList<>();
    private boolean mAlertsEnabled = true;
    private int mThresholdDays = SettingsActivity.DEFAULT_THRESHOLD_DAYS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRepository = PantryRepository.getInstance((PantryApp) getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_pantry);

        mList = findViewById(R.id.pantry_list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PantryAdapter(this);
        mList.setAdapter(mAdapter);

        mEmptyState = findViewById(R.id.empty_state);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setContentDescription(getString(R.string.action_add_desc));
        fab.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditItemActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        setupBottomNav(bottomNav, R.id.nav_pantry);

        mRepository.getPantryLiveData().observe(this, this::onPantryChanged);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Settings may have changed on the Settings screen, so re-read them every resume.
        loadSettings();
        render();
    }

    private void onPantryChanged(@Nullable List<PantryItem> items) {
        mItems = items == null ? new ArrayList<>() : items;
        render();
    }

    /** Pushes the current data + settings into the adapter and toggles the empty state. */
    private void render() {
        mAdapter.setAlertSettings(mAlertsEnabled, mThresholdDays);
        mAdapter.submitList(mItems);
        mEmptyState.setVisibility(mItems.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        mAlertsEnabled = prefs.getBoolean(SettingsActivity.KEY_ALERTS_ENABLED, true);
        mThresholdDays = prefs.getInt(
                SettingsActivity.KEY_THRESHOLD_DAYS, SettingsActivity.DEFAULT_THRESHOLD_DAYS);
    }

    // ------------------------------------------------------------ list actions

    @Override
    public void onEdit(PantryItem item) {
        Intent intent = new Intent(this, AddEditItemActivity.class);
        intent.putExtra(AddEditItemActivity.EXTRA_ITEM_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void onDelete(PantryItem item) {
        // Deleting data is destructive, so it always goes through a confirmation dialog.
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.confirm_delete_item, item.name))
                .setPositiveButton(R.string.button_delete, (dialog, which) ->
                        mRepository.deleteItem(item, () -> {
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(MainActivity.this,
                                        R.string.toast_item_deleted, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    // -------------------------------------------------------------- navigation

    /**
     * Wires the bottom navigation shared by the three top-level screens.
     *
     * The selected item is set <em>before</em> the listener is attached: setSelectedItemId
     * would otherwise fire the listener and immediately re-open the screen we are already
     * on. Taps on the current tab are ignored so the screen is not rebuilt needlessly.
     */
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

    /**
     * Starts another top-level screen without stacking duplicates: CLEAR_TOP removes
     * anything above an existing instance, SINGLE_TOP delivers to it instead of creating
     * a second one, and finish() keeps exactly one instance of each screen alive.
     */
    private void openTopLevel(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
