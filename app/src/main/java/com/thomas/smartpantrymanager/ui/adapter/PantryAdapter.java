package com.thomas.smartpantrymanager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.entity.PantryItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Renders the pantry list.
 *
 * The adapter holds a plain copy of the items rather than observing the database itself:
 * the Activity owns the LiveData and pushes new lists in through {@link #submitList},
 * so this class stays the only place that knows about dates, badges and click wiring.
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    /** Edit/delete actions, forwarded to the Activity so it can use dialogs and toasts. */
    public interface OnItemActionListener {
        void onEdit(PantryItem item);

        void onDelete(PantryItem item);
    }

    private final List<PantryItem> mItems = new ArrayList<>();
    private final OnItemActionListener mListener;

    private boolean mAlertsEnabled = true;
    private int mThresholdDays = 3;

    public PantryAdapter(OnItemActionListener listener) {
        mListener = listener;
    }

    /**
     * Replaces the whole backing list. A full notifyDataSetChanged() is deliberate: the
     * list is small (a household pantry) and rebuilt after every write, so a correct
     * simple refresh beats a subtle DiffUtil callback bug.
     */
    public void submitList(List<PantryItem> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    /**
     * Settings that drive the expiry badge. Call this immediately before {@link #submitList}
     * so a single notifyDataSetChanged() repaints with both the new data and new settings.
     */
    public void setAlertSettings(boolean enabled, int thresholdDays) {
        mAlertsEnabled = enabled;
        mThresholdDays = thresholdDays;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mItems.get(position));
    }

    /**
     * Formats a quantity for display: never scientific notation and never a pointless
     * trailing ".0" (users see "200 g", not "200.0 g" and not "2.0E2 g").
     */
    static String formatQuantity(double quantity) {
        if (Double.isNaN(quantity) || Double.isInfinite(quantity)) {
            return "0";
        }
        long whole = (long) quantity;
        if (quantity == whole) {
            return Long.toString(whole);
        }
        return BigDecimal.valueOf(quantity)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mName;
        private final TextView mQuantity;
        private final TextView mExpiry;
        private final TextView mBadge;
        private final ImageButton mEdit;
        private final ImageButton mDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.item_name);
            mQuantity = itemView.findViewById(R.id.item_quantity);
            mExpiry = itemView.findViewById(R.id.item_expiry);
            mBadge = itemView.findViewById(R.id.item_badge);
            mEdit = itemView.findViewById(R.id.action_edit);
            mDelete = itemView.findViewById(R.id.action_delete);
        }

        void bind(PantryItem item) {
            mName.setText(item.name);
            mQuantity.setText(itemView.getContext().getString(
                    R.string.item_quantity_line,
                    formatQuantity(item.quantity),
                    item.unit == null ? "" : item.unit));

            if (item.expiryDate == null) {
                mExpiry.setText(R.string.no_expiry);
            } else {
                mExpiry.setText(DateFormat.getDateInstance().format(new Date(item.expiryDate)));
            }

            bindBadge(item);

            mEdit.setContentDescription(itemView.getContext().getString(
                    R.string.action_edit_desc, item.name));
            mDelete.setContentDescription(itemView.getContext().getString(
                    R.string.action_delete_desc, item.name));

            mEdit.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onEdit(item);
                }
            });
            mDelete.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onDelete(item);
                }
            });
        }

        /**
         * Shows "Expired" or "Expiring in N day(s)" when alerts are enabled and the item
         * has a date; hides the badge otherwise (alerts off, or no expiry set).
         */
        private void bindBadge(PantryItem item) {
            if (!mAlertsEnabled || item.expiryDate == null) {
                mBadge.setVisibility(View.GONE);
                return;
            }

            long daysLeft = daysUntilToday(item.expiryDate);
            if (daysLeft < 0) {
                mBadge.setText(R.string.badge_expired);
                mBadge.setBackgroundResource(R.drawable.bg_badge_expired);
                mBadge.setTextColor(itemView.getContext().getColor(R.color.badge_expired_text));
                mBadge.setVisibility(View.VISIBLE);
            } else if (daysLeft <= mThresholdDays) {
                mBadge.setText(itemView.getContext().getString(
                        R.string.badge_expiring_soon, (int) daysLeft));
                mBadge.setBackgroundResource(R.drawable.bg_badge_expiring);
                mBadge.setTextColor(itemView.getContext().getColor(R.color.badge_expiring_text));
                mBadge.setVisibility(View.VISIBLE);
            } else {
                mBadge.setVisibility(View.GONE);
            }
        }

        /**
         * Whole calendar days from today to the item's expiry day (0 = expires today,
         * -1 = the expiry day has already passed). Calendar days, not 24-hour blocks, so a
         * daylight-saving shift cannot make an item look a day younger than it is.
         */
        private long daysUntilToday(long expiryMillis) {
            long diff = startOfDay(expiryMillis) - startOfDay(System.currentTimeMillis());
            // Rounding absorbs the 23h/25h midnights caused by daylight-saving changes.
            return Math.round((double) diff / (double) TimeUnit.DAYS.toMillis(1));
        }

        private long startOfDay(long millis) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }
    }
}
