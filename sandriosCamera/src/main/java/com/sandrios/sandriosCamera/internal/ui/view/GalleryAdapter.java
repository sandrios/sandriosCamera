package com.sandrios.sandriosCamera.internal.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.sandrios.sandriosCamera.R;
import com.sandrios.sandriosCamera.internal.SandriosCamera;
import com.sandrios.sandriosCamera.internal.ui.model.Media;

import java.util.List;

/**
 * Created by Arpit Gandhi
 */
public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    final static int SMALL = 0;
    final static int LARGE = 1;
    private List<Media> pickerTiles;
    private Context context;
    private int type;

    GalleryAdapter(Context context, int type, List<Media> pickerTiles) {
        this.context = context;
        this.type = type;
        this.pickerTiles = pickerTiles;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (type == SMALL) {
            view = LayoutInflater.from(context).inflate(R.layout.gallery_item_small, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.gallery_item_large, parent, false);
        }
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final GalleryViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Media media = pickerTiles.get(position);
        if (media.getType() == SandriosCamera.MediaType.PHOTO) {
            Glide.with(context)
                    .load(pickerTiles.get(position).getPath())
                    .thumbnail(0.1f)
                    .apply(new RequestOptions()
                            .dontAnimate()
                            .centerCrop()
                            .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_gallery))
                            .error(ContextCompat.getDrawable(context, R.drawable.ic_error))
                    )
                    .into(holder.iv_thumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return pickerTiles.size();
    }

    class GalleryViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_thumbnail;

        GalleryViewHolder(View view) {
            super(view);
            iv_thumbnail = view.findViewById(R.id.image);
        }
    }
}