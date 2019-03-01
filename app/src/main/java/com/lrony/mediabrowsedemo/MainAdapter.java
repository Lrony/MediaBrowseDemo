package com.lrony.mediabrowsedemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Lrony on 19-2-22.
 */
@SuppressLint("NewApi")
public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ViewHolder> {

    private static final String TAG = "MainAdapter";

    private Context mContext;
    private List<MediaBrowser.MediaItem> mMediaItems;

    private OnItemClickListener mClickListener;

    MainAdapter(Context context, List<MediaBrowser.MediaItem> mediaItems) {
        mContext = context;
        mMediaItems = mediaItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_media, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, @SuppressLint("RecyclerView") final int i) {
        MediaBrowser.MediaItem mediaItem = mMediaItems.get(i);
        viewHolder.ivTitle.setText(mediaItem.getDescription().getTitle());
        if (mediaItem.isBrowsable()) {
            viewHolder.imgIcon.setImageResource(R.drawable.ic_folder);
        } else {
            viewHolder.imgIcon.setImageResource(R.drawable.ic_music);
        }

        // ============================================= Listener

        if (mClickListener != null) {
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mClickListener.onItemClick(view, i);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mMediaItems == null ? 0 : mMediaItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgIcon;
        TextView ivTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_icon);
            ivTitle = itemView.findViewById(R.id.tv_title);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int pos);
    }

    public void setOnItemClickListener(OnItemClickListener mListener) {
        this.mClickListener = mListener;
    }
}
