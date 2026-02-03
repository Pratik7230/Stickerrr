package com.pratikpatil.stickerrr.stickerpacklist;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.R;
import com.pratikpatil.stickerrr.stickerapi.Sticker;
import com.pratikpatil.stickerrr.stickerapi.StickerPackLoader;

import java.io.InputStream;
import java.util.List;

public class StickerPreviewAdapter extends RecyclerView.Adapter<StickerPreviewAdapter.ViewHolder> {

    private final String packIdentifier;
    private final List<Sticker> stickers;
    private final ContentResolver contentResolver;

    public StickerPreviewAdapter(String packIdentifier, List<Sticker> stickers, ContentResolver contentResolver) {
        this.packIdentifier = packIdentifier;
        this.stickers = stickers != null ? stickers : java.util.Collections.emptyList();
        this.contentResolver = contentResolver;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_preview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sticker sticker = stickers.get(position);
        Uri uri = StickerPackLoader.getStickerAssetUri(packIdentifier, sticker.imageFileName);
        try (InputStream is = contentResolver.openInputStream(uri)) {
            if (is != null) {
                Bitmap b = BitmapFactory.decodeStream(is);
                if (b != null) {
                    holder.imgSticker.setImageBitmap(b);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;

        ViewHolder(View itemView) {
            super(itemView);
            imgSticker = itemView.findViewById(R.id.imgSticker);
        }
    }
}
