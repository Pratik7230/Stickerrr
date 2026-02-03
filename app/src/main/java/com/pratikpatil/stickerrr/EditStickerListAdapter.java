package com.pratikpatil.stickerrr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.stickerapi.Sticker;

import java.util.List;

public class EditStickerListAdapter extends RecyclerView.Adapter<EditStickerListAdapter.ViewHolder> {

    private final List<Sticker> stickers;
    private final OnRemoveStickerListener onRemoveStickerListener;

    public interface OnRemoveStickerListener {
        void onRemoveSticker(Sticker sticker, int position);
    }

    public EditStickerListAdapter(List<Sticker> stickers, OnRemoveStickerListener onRemoveStickerListener) {
        this.stickers = stickers;
        this.onRemoveStickerListener = onRemoveStickerListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_edit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sticker s = stickers.get(position);
        holder.txtStickerIndex.setText("#" + (position + 1));
        holder.txtStickerFile.setText(s.imageFileName);
        holder.btnRemove.setOnClickListener(v -> {
            if (onRemoveStickerListener != null) {
                onRemoveStickerListener.onRemoveSticker(s, holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStickerIndex;
        TextView txtStickerFile;
        Button btnRemove;

        ViewHolder(View itemView) {
            super(itemView);
            txtStickerIndex = itemView.findViewById(R.id.txtStickerIndex);
            txtStickerFile = itemView.findViewById(R.id.txtStickerFile);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
