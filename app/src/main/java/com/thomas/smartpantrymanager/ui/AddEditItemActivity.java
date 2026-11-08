package com.thomas.smartpantrymanager.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.thomas.smartpantrymanager.PantryApp;
import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.PantryRepository;
import com.thomas.smartpantrymanager.data.entity.PantryItem;
import com.thomas.smartpantrymanager.logic.UnitConverter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Screen 2 — Add / Edit an ingredient.
 *
 * Acts as both a creation and an edit form: {@link #EXTRA_ITEM_ID} decides which mode runs.
 * Validation happens entirely in the UI layer and blocks the save, so invalid input never
 * reaches the repository (and therefore never reaches SQLite).
 */
public class AddEditItemActivity extends AppCompatActivity {

    /** Extra carrying the pantry row id to edit; absent or -1 means "create a new row". */
    public static final String EXTRA_ITEM_ID = "extra_item_id";
    private static final long NEW_ITEM_ID = -1L;
    private static final int MAX_NAME_LENGTH = 60;

    private PantryRepository mRepository;

    private TextInputLayout mNameLayout;
    private TextInputLayout mQuantityLayout;
    private TextInputLayout mUnitLayout;
    private TextInputEditText mNameEdit;
    private TextInputEditText mQuantityEdit;
    private MaterialAutoCompleteTextView mUnitEdit;
    private Button mExpiryButton;
    private Button mClearExpiryButton;
    private Button mSaveButton;

    private long mItemId = NEW_ITEM_ID;
    private boolean mEditMode;
    private PantryItem mExistingItem;
    private Long mExpiryDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        mRepository = PantryRepository.getInstance((PantryApp) getApplication());
        mItemId = getIntent().getLongExtra(EXTRA_ITEM_ID, NEW_ITEM_ID);
        mEditMode = mItemId != NEW_ITEM_ID;

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(mEditMode ? R.string.title_edit_item : R.string.title_add_item);
        toolbar.setNavigationOnClickListener(v -> finish());

        mNameLayout = findViewById(R.id.field_name);
        mQuantityLayout = findViewById(R.id.field_quantity);
        mUnitLayout = findViewById(R.id.field_unit);
        mNameEdit = findViewById(R.id.input_name);
        mQuantityEdit = findViewById(R.id.input_quantity);
        mUnitEdit = findViewById(R.id.input_unit);
        mExpiryButton = findViewById(R.id.btn_pick_expiry);
        mClearExpiryButton = findViewById(R.id.btn_clear_expiry);
        mSaveButton = findViewById(R.id.btn_save);

        setupUnitDropdown();
        setupExpiryPicker();
        updateExpiryLabel();

        mSaveButton.setOnClickListener(v -> onSaveClicked());

        if (mEditMode) {
            // Block saving until the row has loaded: otherwise a very fast tap on Save
            // would insert a duplicate instead of updating the row being edited.
            mSaveButton.setEnabled(false);
            loadItem();
        }
    }

    private void setupUnitDropdown() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.unit_suggestions, android.R.layout.simple_list_item_1);
        mUnitEdit.setAdapter(adapter);
    }

    private void setupExpiryPicker() {
        mExpiryButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (mExpiryDate != null) {
                calendar.setTimeInMillis(mExpiryDate);
            }
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, dayOfMonth, 23, 59, 59);
                picked.set(Calendar.MILLISECOND, 0);
                mExpiryDate = picked.getTimeInMillis();
                updateExpiryLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        mClearExpiryButton.setOnClickListener(v -> {
            mExpiryDate = null;
            updateExpiryLabel();
        });
    }

    private void updateExpiryLabel() {
        String label;
        if (mExpiryDate == null) {
            label = getString(R.string.action_pick_expiry);
        } else {
            label = DateFormat.getDateInstance().format(new Date(mExpiryDate));
        }
        mExpiryButton.setText(label);
        mExpiryButton.setContentDescription(getString(R.string.expiry_value_desc, label));
        mClearExpiryButton.setEnabled(mExpiryDate != null);
    }

    /** Loads the row to edit; a stale id (deleted meanwhile) exits with a toast. */
    private void loadItem() {
        mRepository.getItemById(mItemId, item -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (item == null) {
                Toast.makeText(this, R.string.toast_item_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            mExistingItem = item;
            mNameEdit.setText(item.name);
            mQuantityEdit.setText(toEditableQuantity(item.quantity));
            mUnitEdit.setText(item.unit);
            mExpiryDate = item.expiryDate;
            updateExpiryLabel();
            mSaveButton.setEnabled(true);
        });
    }

    private void onSaveClicked() {
        String name = textOf(mNameEdit).trim();
        String unit = textOf(mUnitEdit).trim();
        String quantityText = textOf(mQuantityEdit).trim();

        double quantity = 0d;
        boolean quantityValid = false;
        try { // defensive parsing: "abc" and empty strings must never reach the database
            quantity = Double.parseDouble(quantityText);
            quantityValid = true;
        } catch (NumberFormatException ignored) {
            quantityValid = false;
        }

        boolean valid = true;

        if (name.isEmpty()) {
            mNameLayout.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (name.length() > MAX_NAME_LENGTH) {
            mNameLayout.setError(getString(R.string.error_name_too_long, MAX_NAME_LENGTH));
            valid = false;
        } else {
            mNameLayout.setError(null);
        }

        if (!quantityValid) {
            mQuantityLayout.setError(getString(R.string.error_quantity_invalid));
            valid = false;
        } else if (quantity <= 0d) {
            mQuantityLayout.setError(getString(R.string.error_quantity_positive));
            valid = false;
        } else {
            mQuantityLayout.setError(null);
        }

        if (unit.isEmpty()) {
            mUnitLayout.setError(getString(R.string.error_unit_required));
            valid = false;
        } else if (!UnitConverter.isRecognised(unit)) {
            // Only units from the known set may be saved: an unknown unit would degrade to a
            // generic count, which could make the strict matcher accept a wrong quantity.
            mUnitLayout.setError(getString(R.string.error_unit_invalid));
            valid = false;
        } else {
            mUnitLayout.setError(null);
        }

        if (!valid) {
            return; // keep the form open with the inline errors showing
        }

        PantryItem item = mEditMode && mExistingItem != null ? mExistingItem : new PantryItem();
        item.name = name;
        item.quantity = quantity;
        item.unit = unit;
        item.expiryDate = mExpiryDate;

        if (mEditMode && mExistingItem != null) {
            mRepository.updateItem(item, () -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, R.string.toast_item_updated, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            mRepository.insertItem(item, () -> {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, R.string.toast_item_added, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private String textOf(MaterialAutoCompleteTextView editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    /**
     * Renders an existing quantity as editable text: "200" for 200.0, "0.5" for 0.5 and
     * never "2.0E2" — scientific notation in an input field is unreadable and uneditable.
     */
    private static String toEditableQuantity(double quantity) {
        long whole = (long) quantity;
        if (quantity == whole && !Double.isInfinite(quantity)) {
            return Long.toString(whole);
        }
        return BigDecimal.valueOf(quantity)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
