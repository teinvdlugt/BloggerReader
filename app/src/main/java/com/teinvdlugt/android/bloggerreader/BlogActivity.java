package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.io.IOException;

public class BlogActivity extends CustomTabsActivity implements PostAdapter.OnPostClickListener,
        PostAdapter.HeaderUpdateListener, PostAdapter.LoadNextBatchListener {
    public static final String BLOG_NAME_EXTRA = "blog_name";
    public static final String BLOG_ID_EXTRA = "blog_id";

    private ViewHolder holder;
    private Blog blog;
    private String blogId;
    private PostAdapter adapter;
    private boolean followingBlogDetermined;
    private boolean followingBlog;
    private Blogger blogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        blogId = getIntent().getStringExtra(BLOG_ID_EXTRA);
        String blogName = getIntent().getStringExtra(BLOG_NAME_EXTRA);
        if (blogId == null || blogName == null)
            throw new IllegalArgumentException("The intent extras blog_name and blog_id were not passed");
        setTitle(blogName);

        setContentView(R.layout.activity_blog);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, this, this);
        adapter.setHeader(this);
        recyclerView.setAdapter(adapter);

        refresh();
    }

    @Override
    public void loadNextBatch() {
        // TODO: 23-1-2016 Load next batch
    }

    private enum ProgressUpdateType {BLOG, FOLLOWING_BLOG}

    private void refresh() {
        new AsyncTask<Void, ProgressUpdateType, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (blogger == null) blogger = IOUtils.createBloggerInstance();

                try {
                    blog = blogger.blogs().get(blogId)
                            .setMaxPosts(50L)
                            .setKey(Constants.API_KEY).execute();
                    publishProgress(ProgressUpdateType.BLOG);

                    followingBlog = IOUtils.blogFollowed(BlogActivity.this, blog.getId());
                    publishProgress(ProgressUpdateType.FOLLOWING_BLOG);

                    PreferenceManager.getDefaultSharedPreferences(BlogActivity.this)
                            .edit()
                            .putString(Constants.LAST_POST_ID_PREF + blog.getId(), blog.getPosts().getItems().get(0).getId())
                            .apply();
                } catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(ProgressUpdateType... type) {
                switch (type[0]) {
                    case BLOG:
                        bindViewHolder(holder);
                        break;
                    case FOLLOWING_BLOG:
                        followingBlogDetermined = true;
                        if (holder != null && holder.followButton != null) {
                            holder.followButton.setVisibility(View.VISIBLE);
                            if (followingBlog) setButtonTextFollowing();
                            else setButtonTextFollow();
                        }
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                bindViewHolder(holder);
                if (blog.getPosts() != null && blog.getPosts().getItems() != null) {
                    adapter.setData(blog.getPosts().getItems());
                    adapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }

    private void onClickFollowButton() {
        if (blog == null || !followingBlogDetermined) return;

        if (followingBlog) {
            // Unfollow blog
            IOUtils.unfollowBlog(this, blog.getId());
            followingBlog = false;
            setButtonTextFollow();
        } else {
            // Follow blog
            IOUtils.saveBlog(this, blog);
            followingBlog = true;
            setButtonTextFollowing();
        }
    }

    @Override
    public void onClickPost(Post post) {
        PostActivity.openPost(this, post.getBlog().getId(), post.getId(), post.getUrl());
    }

    @Override
    public RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        View view = getLayoutInflater().inflate(R.layout.blog_header_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void bindViewHolder(RecyclerView.ViewHolder holder1) {
        if (holder1 == null) return;
        holder = (ViewHolder) holder1;

        if (blog != null) {
            if (blog.getDescription() != null) holder.descTV.setText(blog.getDescription());
            if (followingBlogDetermined) {
                holder.followButton.setVisibility(View.VISIBLE);
                if (followingBlog) setButtonTextFollowing();
                else setButtonTextFollow();
            }
        }
    }

    private void setButtonTextFollow() {
        holder.followButton.setText(R.string.follow);
        holder.followButton.getBackground().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
        holder.followButton.setTextColor(Color.BLACK);
    }

    private void setButtonTextFollowing() {
        holder.followButton.setText(R.string.following);
        holder.followButton.getBackground().setColorFilter(
                IOUtils.getColor(this, R.color.colorAccent), PorterDuff.Mode.MULTIPLY);
        holder.followButton.setTextColor(Color.WHITE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    public static void openBlogActivity(Context context, String blogName, String blogId) {
        Intent intent = new Intent(context, BlogActivity.class);
        intent.putExtra(BLOG_NAME_EXTRA, blogName);
        intent.putExtra(BLOG_ID_EXTRA, blogId);
        context.startActivity(intent);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView descTV;
        private Button followButton;

        public ViewHolder(View itemView) {
            super(itemView);
            descTV = (TextView) itemView.findViewById(R.id.description);
            followButton = (Button) itemView.findViewById(R.id.follow_button);

            followButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickFollowButton();
                }
            });
        }
    }
}
