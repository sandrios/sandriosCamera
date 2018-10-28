package com.sandrios.sandriosCamera.internal.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    private List<Media> pickerTiles;
    private Context context;

    GalleryAdapter(Context context, List<Media> pickerTiles) {
        this.context = context;
        this.pickerTiles = pickerTiles;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.gallery_item, null);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Media media = pickerTiles.get(position);
        if (media.getType() == SandriosCamera.MediaType.PHOTO) {
            Glide.with(context)
                    .load(pickerTiles.get(position).getPath())
                    .apply(new RequestOptions()
                            .dontAnimate()
                            .centerCrop()
                            .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_gallery))
                            .error(ContextCompat.getDrawable(context, R.drawable.ic_error))
                    )
                    .thumbnail(0.1f)
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