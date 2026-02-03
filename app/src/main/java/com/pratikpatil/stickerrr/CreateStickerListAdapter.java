package com.pratikpatil.stickerrr;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.stickerapi.Sticker;

import java.util.List;

public class CreateStickerListAdapter extends RecyclerView.Adapter<CreateStickerListAdapter.ViewHolder> {

    private final List<Sticker> stickers;

    public CreateStickerListAdapter(List<Sticker> stickers) {
        this.stickers = stickers;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_simple, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sticker s = stickers.get(position);
        holder.txtStickerIndex.setText("#" + (position + 1));
        holder.txtStickerFile.setText(s.imageFileName);
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtStickerIndex;
        TextView txtStickerFile;

        ViewHolder(View itemView) {
            super(itemView);
            txtStickerIndex = itemView.findViewById(R.id.txtStickerIndex);
            txtStickerFile = itemView.findViewById(R.id.txtStickerFile);
        }
    }
}
