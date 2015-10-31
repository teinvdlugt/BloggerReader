package com.teinvdlugt.android.bloggerreader;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.client.util.DateTime;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.text.DateFormat;
import java.util.Date;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private Blog.Posts data;
    private Context context;

    public PostAdapter(Blog.Posts data, Context context) {
        this.data = data;
        this.context = context;
    }

    public PostAdapter(Context context) {
        this.context = context;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView title, published, preview;
        private DateFormat dateFormat;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.post_title);
            published = (TextView) itemView.findViewById(R.id.published_time);
            preview = (TextView) itemView.findViewById(R.id.post_preview);
            dateFormat = DateFormat.getDateTimeInstance();
        }

        public void bind(Post data) {
            title.setText(data.getTitle());
            Date date = new Date(data.getPublished().getValue());
            published.setText(dateFormat.format(date));
        }
    }
}
