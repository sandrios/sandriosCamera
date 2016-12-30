package com.sandrios.sandriosCamera.internal.ui.view;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.sandrios.sandriosCamera.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by TedPark on 2016. 8. 30..
 */
public class ImageGalleryAdapter extends RecyclerView.Adapter<ImageGalleryAdapter.GalleryViewHolder> {


    ArrayList<PickerTile> pickerTiles;
    Context context;
    OnItemClickListener onItemClickListener;

    public ImageGalleryAdapter(Context context) {

        this.context = context;

        pickerTiles = new ArrayList<>();

        Cursor imageCursor = null;
        try {
            final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.ORIENTATION};
            final String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";


            imageCursor = context.getApplicationContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
            //imageCursor = sContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
            if (imageCursor != null) {
                int count = 0;
                while (imageCursor.moveToNext()) {
                    String imageLocation = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File imageFile = new File(imageLocation);
                    pickerTiles.add(new PickerTile(Uri.fromFile(imageFile)));
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (imageCursor != null && !imageCursor.isClosed()) {
                imageCursor.close();
            }
        }
    }

    @Override
    public GalleryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(context, R.layout.image_item, null);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final GalleryViewHolder holder, final int position) {

        PickerTile pickerTile = getItem(position);

        Uri uri = pickerTile.getImageUri();
        Glide.with(context)
                .load(uri)
                .thumbnail(0.1f)
                .dontAnimate()
                .centerCrop()
                .placeholder(ContextCompat.getDrawable(context, R.drawable.ic_gallery))
                .error(ContextCompat.getDrawable(context, R.drawable.ic_error))
                .into(holder.iv_thumbnail);

        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemClickListener.onItemClick(holder.itemView, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return pickerTiles.size();
    }

    public PickerTile getItem(int position) {
        return pickerTiles.get(position);
    }

    public void setOnItemClickListener(
            OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }


    public static class PickerTile {

        protected final Uri imageUri;

        PickerTile(@NonNull Uri imageUri) {
            this.imageUri = imageUri;
        }

        @Nullable
        public Uri getImageUri() {
            return imageUri;
        }

        @Override
        public String toString() {
            return "ImageTile: " + imageUri;
        }

    }

    class GalleryViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_thumbnail;

        public GalleryViewHolder(View view) {
            super(view);
            iv_thumbnail = (ImageView) view.findViewById(R.id.image);
        }
    }
}