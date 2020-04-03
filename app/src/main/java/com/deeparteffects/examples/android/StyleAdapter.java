package com.deeparteffects.examples.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.deeparteffects.sdk.android.model.Styles;

public class StyleAdapter extends RecyclerView.Adapter<StyleAdapter.ViewHolder> {

    private Context mContext;
    private Styles mStyles;
    private ClickListener mClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView styleImage;

        ViewHolder(View view) {
            super(view);
            styleImage = view.findViewById(R.id.styleImage);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (styleImage == null) {
                return;
            }
            mClickListener.onClick(mStyles.get(getAdapterPosition()).getId());
        }
    }

    StyleAdapter(Context context, Styles styles, ClickListener clickListener) {
        mContext = context;
        mStyles = styles;
        mClickListener = clickListener;
    }

    @NonNull
    @Override
    public StyleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_style, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String imageUrl = mStyles.get(position).getUrl();
        Glide.with(mContext).load(imageUrl).centerCrop().crossFade().into(holder.styleImage);
    }

    @Override
    public int getItemCount() {
        return mStyles.size();
    }

    public interface ClickListener {
        void onClick(String styleId);
    }
}
