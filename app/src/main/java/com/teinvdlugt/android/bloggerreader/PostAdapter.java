package com.teinvdlugt.android.bloggerreader;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.text.DateFormat;
import java.util.Date;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_VIEW_TYPE = 0;
    private static final int HEADER_VIEW_TYPE = 1;

    private Blog.Posts data;
    private Context context;
    private OnPostClickListener clickListener;
    private boolean header;

    private HeaderUpdateListener headerListener;

    public PostAdapter(Context context, Blog.Posts data, OnPostClickListener clickListener) {
        this.data = data;
        this.context = context;
        this.clickListener = clickListener;
    }

    public PostAdapter(Context context, OnPostClickListener clickListener) {
        this.context = context;
        this.clickListener = clickListener;
    }

    public PostAdapter(Context context) {
        this.context = context;
    }

    public void setOnPostClickListener(OnPostClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setHeader(HeaderUpdateListener listener) {
        this.header = true;
        this.headerListener = listener;
    }

    public Blog.Posts getData() {
        return data;
    }

    public void setData(Blog.Posts data) {
        this.data = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE:
                return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_post, parent, false));
            case HEADER_VIEW_TYPE:
                return headerListener == null ? null : headerListener.createViewHolder(parent);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case HEADER_VIEW_TYPE:
                if (headerListener != null) headerListener.bindViewHolder(holder);
                break;
            case ITEM_VIEW_TYPE:
                if (getItemViewType(0) == HEADER_VIEW_TYPE) {
                    // A header is present
                    position--;
                }
                ((ViewHolder) holder).bind(data.getItems().get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && header) {
            return HEADER_VIEW_TYPE;
        } else {
            return ITEM_VIEW_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        int items = data == null || data.getItems() == null ? 0 : data.getItems().size();
        return items + (header ? 1 : 0);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title, published;
        private DateFormat dateFormat;
        private Post post;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.post_title);
            published = (TextView) itemView.findViewById(R.id.published_time);
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);

            itemView.findViewById(R.id.item_root).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClickPost(post);
                }
            });
        }

        public void bind(Post post) {
            this.post = post;
            title.setText(post.getTitle());
            Date date = new Date(post.getPublished().getValue());
            published.setText(post.getAuthor().getDisplayName() + ", " + dateFormat.format(date));
        }
    }

    public interface OnPostClickListener {
        void onClickPost(Post post);
    }

    public interface HeaderUpdateListener {
        RecyclerView.ViewHolder createViewHolder(ViewGroup parent);

        void bindViewHolder(RecyclerView.ViewHolder holder);
    }
}
