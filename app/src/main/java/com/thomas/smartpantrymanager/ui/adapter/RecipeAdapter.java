package com.thomas.smartpantrymanager.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thomas.smartpantrymanager.R;
import com.thomas.smartpantrymanager.data.relation.RecipeWithIngredients;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders recipe rows.
 * Used by the two lists on the suggestions screen: the strict "you can make this" list and
 * the separate "Almost There" list. The same row layout is reused, but the subtitle differs
 * — availability summary versus the single missing ingredient — so the missing label for
 * each recipe id is handed in by the Activity (which computed it in the matching engine)
 * rather than being guessed at here.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    /** Tapped-row callback; the Activity turns it into an Intent carrying the recipe id. */
    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeWithIngredients recipe);
    }

    private final List<RecipeWithIngredients> mRecipes = new ArrayList<>();
    private final OnRecipeClickListener mListener;

    /** When true, each row shows "Missing: x" instead of the availability summary. */
    private final boolean mShowMissing;

    /** recipeId -> name of its one missing ingredient; only used in "missing" mode. */
    private final Map<Long, String> mMissingLabels = new HashMap<>();

    public RecipeAdapter(OnRecipeClickListener listener, boolean showMissing) {
        mListener = listener;
        mShowMissing = showMissing;
    }

    /** Supplies the "Missing: x" labels for the "Almost There" rows; call before submitList. */
    public void setMissingLabels(Map<Long, String> labels) {
        mMissingLabels.clear();
        if (labels != null) {
            mMissingLabels.putAll(labels);
        }
    }

    /** Replaces the backing list; empty and null lists are both safe. */
    public void submitList(List<RecipeWithIngredients> recipes) {
        mRecipes.clear();
        if (recipes != null) {
            mRecipes.addAll(recipes);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mRecipes.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(mRecipes.get(position));
    }

    /** Same rule as the pantry adapter: no scientific notation, no trailing ".0". */
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
        private final TextView mSubtitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.recipe_name);
            mSubtitle = itemView.findViewById(R.id.recipe_subtitle);
        }

        void bind(RecipeWithIngredients recipe) {
            if (recipe == null || recipe.recipe == null) {
                return;
            }
            mName.setText(recipe.recipe.name);

            int count = recipe.ingredients == null ? 0 : recipe.ingredients.size();
            if (mShowMissing) {
                String missing = mMissingLabels.get(recipe.recipe.id);
                mSubtitle.setText(missing == null
                        ? itemView.getContext().getString(R.string.recipe_all_available, count)
                        : itemView.getContext().getString(R.string.recipe_missing_one, missing));
            } else {
                mSubtitle.setText(itemView.getContext().getString(
                        R.string.recipe_all_available, count));
            }

            itemView.setContentDescription(itemView.getContext().getString(
                    R.string.recipe_row_desc, recipe.recipe.name));
            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onRecipeClick(recipe);
                }
            });
        }
    }
}
