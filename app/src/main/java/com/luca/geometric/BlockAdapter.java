package com.luca.geometric;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

/**
 * RecyclerView Adapter responsible for displaying the list of quiz blocks in a category.
 * Handles creating ViewHolder item layouts, binding BlockProgress data to views,
 * and communicating click interactions to the parent fragment.
 */
public class BlockAdapter extends RecyclerView.Adapter<BlockAdapter.BlockViewHolder> {

    /**
     * Interface callback to handle user click events on unlocked quiz blocks.
     */
    public interface OnBlockClickListener {
        void onBlockClick(BlockProgress block);
    }

    private final List<BlockProgress> blocks;
    private final OnBlockClickListener listener;

    /**
     * Constructor initializing the adapter with data list and click listener callback.
     *
     * @param blocks   List of BlockProgress records to display.
     * @param listener Listener callback for click events.
     */
    public BlockAdapter(List<BlockProgress> blocks, OnBlockClickListener listener) {
        this.blocks = blocks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the XML layout for an individual block card item (instantiate from .xml)
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_block_card, parent, false);
        return new BlockViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockViewHolder h, int position) {
        // Bind the BlockProgress data object at the current position to the ViewHolder
        h.bind(blocks.get(position));
    }

    @Override
    public int getItemCount() {
        return blocks.size();
    }

    // ViewHolder
    /**
     * ViewHolder pattern class that holds references to all UI elements inside item_block_card.xml layout.
     */
    public class BlockViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoot;
        private final TextView tvBlockNumber;
        private final TextView tvBestScore;
        private final ImageView ivLock;
        private final LinearProgressIndicator barCorrect;
        private final View viewNeverPlayed;

        BlockViewHolder(@NonNull View itemView) {
            super(itemView);
            // Locate and cache UI view references from the inflated item layout
            cardRoot = itemView.findViewById(R.id.card_block_root);
            tvBlockNumber = itemView.findViewById(R.id.tv_block_number);
            tvBestScore = itemView.findViewById(R.id.tv_best_score);
            ivLock = itemView.findViewById(R.id.iv_block_lock);
            barCorrect = itemView.findViewById(R.id.bar_correct);
            viewNeverPlayed = itemView.findViewById(R.id.view_never_played);
        }

        /**
         * Binds data from a BlockProgress instance into the item views.
         * Sets text, updates score indicators, colors, and enables or disables interactions depending on lock state.
         *
         * @param bp The BlockProgress record for this item.
         */
        void bind(BlockProgress bp) {

            // Set block title (converting 0-based blockIndex to 1-based display number e.g. "Block 1")
            tvBlockNumber.setText("Block " + (bp.blockIndex + 1));

            // Configure score text and progress bar based on whether the block has been played before
            if (bp.bestScore < 0) {
                // If block has not been played yet (bestScore defaults to -1)
                tvBestScore.setText("—");
                barCorrect.setVisibility(View.GONE);
                viewNeverPlayed.setVisibility(View.VISIBLE);

            } else {
                // If block has been played at least once
                viewNeverPlayed.setVisibility(View.GONE);
                barCorrect.setVisibility(View.VISIBLE);

                // Display score fraction string (e.g. "8 / 10")
                tvBestScore.setText(bp.bestScore + " / " + bp.questionCount);

                int correct = bp.bestScore;
                int wrong = bp.questionCount - correct;

                barCorrect.setMax(bp.questionCount);
                barCorrect.setProgress(correct, true);

                // Configure progress indicator bar colors: green for correct answers, red track for wrong answers
                int greenColor = ContextCompat.getColor(
                        itemView.getContext(), R.color.answer_correct);
                int trackColor = ContextCompat.getColor(
                        itemView.getContext(),
                        wrong > 0 ? R.color.answer_wrong : R.color.progress_track);

                barCorrect.setIndicatorColor(greenColor);
                barCorrect.setTrackColor(trackColor);
            }

            // Configure layout visual state and touch handler depending on whether block is unlocked
            if (bp.isUnlocked) {
                // If unlocked: full opacity, hide lock icon, show score, attach click listener
                cardRoot.setAlpha(1.0f);
                ivLock.setVisibility(View.GONE);
                tvBestScore.setVisibility(View.VISIBLE);
                cardRoot.setOnClickListener(v -> listener.onBlockClick(bp));
            } else {
                // If locked: dimmed card opacity, show lock icon, hide score, clear click listener
                cardRoot.setAlpha(0.45f);
                ivLock.setVisibility(View.VISIBLE);
                tvBestScore.setVisibility(View.GONE);
                cardRoot.setOnClickListener(null);
            }
        }
    }
}
