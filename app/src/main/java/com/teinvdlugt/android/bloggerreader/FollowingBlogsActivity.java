package com.teinvdlugt.android.bloggerreader;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.services.blogger.model.Blog;

import java.util.ArrayList;
import java.util.List;

public class FollowingBlogsActivity extends AppCompatActivity {
    private FollowingBlogsAdapter adapter;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_blogs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowingBlogsAdapter();
        recyclerView.setAdapter(adapter);

        List<Blog> blogs = IOUtils.blogsFollowing(this);
        adapter.setData(blogs);
        adapter.notifyDataSetChanged();
    }

    private class FollowingBlogsAdapter extends RecyclerView.Adapter<FollowingBlogsAdapter.ViewHolder> {
        private List<Blog> data = new ArrayList<>();

        public void setData(List<Blog> data) {
            this.data = data;
        }

        @Override
        public FollowingBlogsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(FollowingBlogsActivity.this).inflate(R.layout.list_item_following_blog, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FollowingBlogsAdapter.ViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView nameTV, urlTV;

            public ViewHolder(View itemView) {
                super(itemView);
                nameTV = (TextView) findViewById(R.id.blog_name);
                urlTV = (TextView) findViewById(R.id.blog_url);
            }

            public void bind(Blog data) {
                if (data.getName() == null) nameTV.setText(R.string.unknown_blog);
                else nameTV.setText(data.getName());
                if (data.getUrl() == null) urlTV.setVisibility(View.GONE);
                else {
                    urlTV.setVisibility(View.VISIBLE);
                    urlTV.setText(data.getUrl());
                }
            }
        }
    }
}