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

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private Blog.Posts data;
    private Context context;
    private OnPostClickListener clickListener;

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

    public Blog.Posts getData() {
        return data;
    }

    public void setData(Blog.Posts data) {
        this.data = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_post, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(data.getItems().get(position));
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.getItems().size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
}
