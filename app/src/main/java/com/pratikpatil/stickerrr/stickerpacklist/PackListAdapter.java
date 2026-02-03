package com.pratikpatil.stickerrr.stickerpacklist;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pratikpatil.stickerrr.BuildConfig;
import com.pratikpatil.stickerrr.R;
import com.pratikpatil.stickerrr.stickerapi.StickerPack;
import com.pratikpatil.stickerrr.stickerapi.StickerPackLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PackListAdapter extends RecyclerView.Adapter<PackListAdapter.ViewHolder> {

    private final List<StickerPack> packs = new ArrayList<>();
    private final ContentResolver contentResolver;
    private final AddToWhatsAppListener addToWhatsAppListener;

    public interface AddToWhatsAppListener {
        void onAddToWhatsApp(StickerPack pack);
        void onPreviewPack(StickerPack pack);
        void onEditPack(StickerPack pack);
        void onDeletePack(StickerPack pack);
    }

    public PackListAdapter(ContentResolver contentResolver, AddToWhatsAppListener addToWhatsAppListener) {
        this.contentResolver = contentResolver;
        this.addToWhatsAppListener = addToWhatsAppListener;
    }

    public void setPacks(List<StickerPack> list) {
        packs.clear();
        if (list != null) packs.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sticker_pack, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StickerPack pack = packs.get(position);
        holder.txtPackName.setText(pack.name);
        holder.txtPublisher.setText(pack.publisher);
        Uri trayUri = StickerPackLoader.getStickerAssetUri(pack.identifier, pack.trayImageFile);
        try (InputStream is = contentResolver.openInputStream(trayUri)) {
            if (is != null) {
                Bitmap b = BitmapFactory.decodeStream(is);
                if (b != null) {
                    holder.imgTray.setImageBitmap(b);
                }
            }
        } catch (Exception ignored) {
        }
        holder.btnAddToWhatsApp.setOnClickListener(v -> {
            if (addToWhatsAppListener != null) addToWhatsAppListener.onAddToWhatsApp(pack);
        });
        holder.btnPreview.setOnClickListener(v -> {
            if (addToWhatsAppListener != null) addToWhatsAppListener.onPreviewPack(pack);
        });
        holder.btnEdit.setOnClickListener(v -> {
            if (addToWhatsAppListener != null) addToWhatsAppListener.onEditPack(pack);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (addToWhatsAppListener != null) addToWhatsAppListener.onDeletePack(pack);
        });
    }

    @Override
    public int getItemCount() {
        return packs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgTray;
        TextView txtPackName;
        TextView txtPublisher;
        Button btnAddToWhatsApp;
        Button btnPreview;
        Button btnEdit;
        Button btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            imgTray = itemView.findViewById(R.id.imgTray);
            txtPackName = itemView.findViewById(R.id.txtPackName);
            txtPublisher = itemView.findViewById(R.id.txtPublisher);
            btnAddToWhatsApp = itemView.findViewById(R.id.btnAddToWhatsApp);
            btnPreview = itemView.findViewById(R.id.btnPreview);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
