package com.teinvdlugt.android.bloggerreader;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.blogger.model.Post;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int ITEM_VIEW_TYPE = 0;
    private static final int HEADER_VIEW_TYPE = 1;
    private static final int PROGRESS_BAR_VIEW_TYPE = 2;

    private List<Post> data;
    private Context context;
    private OnPostClickListener clickListener;
    private boolean header;
    private boolean progressBar = true;

    private HeaderUpdateListener headerListener;
    private LoadNextBatchListener loadNextBatchListener;

    public PostAdapter(Context context, List<Post> data, OnPostClickListener clickListener) {
        this.data = data;
        this.context = context;
        this.clickListener = clickListener;
    }

    public PostAdapter(Context context, OnPostClickListener clickListener, LoadNextBatchListener loadNextBatchListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.loadNextBatchListener = loadNextBatchListener;
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

    public List<Post> getData() {
        return data;
    }

    public void setData(List<Post> data) {
        this.data = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE:
                return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_post, parent, false));
            case HEADER_VIEW_TYPE:
                return headerListener == null ? null : headerListener.createViewHolder(parent);
            case PROGRESS_BAR_VIEW_TYPE:
                return new ProgressBarViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_progress_bar, parent, false));
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
                ((ViewHolder) holder).bind(data.get(position));
                break;
            case PROGRESS_BAR_VIEW_TYPE:
                if (loadNextBatchListener != null) loadNextBatchListener.loadNextBatch();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (getItemCount() != 0 && getItemCount() != 1 && position == getItemCount() - 1 && progressBar)
            return PROGRESS_BAR_VIEW_TYPE;
        if (position == 0 && header)
            return HEADER_VIEW_TYPE;
        else
            return ITEM_VIEW_TYPE;
    }

    @Override
    public int getItemCount() {
        int items = data == null ? 0 : data.size();
        items += (header ? 1 : 0);
        if (items != 0 && items != 1 && progressBar) items += 1;
        return items;
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

    private class ProgressBarViewHolder extends RecyclerView.ViewHolder {
        public ProgressBarViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface OnPostClickListener {
        void onClickPost(Post post);
    }

    public interface HeaderUpdateListener {
        RecyclerView.ViewHolder createViewHolder(ViewGroup parent);

        void bindViewHolder(RecyclerView.ViewHolder holder);
    }

    public interface LoadNextBatchListener {
        void loadNextBatch();
    }

    public void nextBatchLoaded(List<Post> batch, boolean moreComing) {
        this.progressBar = moreComing;
        if (!progressBar) notifyItemRemoved(getItemCount() - 1);
        if (data.isEmpty()) return;

        int dataEndPos = getItemCount() - (progressBar ? 2 : 1);
        data.addAll(batch);
        notifyItemRangeInserted(dataEndPos + 1, batch.size());
    }
}
