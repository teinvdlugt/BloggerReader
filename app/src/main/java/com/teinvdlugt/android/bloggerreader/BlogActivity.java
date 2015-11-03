package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.io.IOException;

public class BlogActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener,
        PostAdapter.HeaderUpdateListener {
    public static final String BLOG_ID = "blog_id";

    private ViewHolder holder;
    private Blog blog;
    private String blogId;
    private PostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        blogId = getIntent().getStringExtra(BLOG_ID);
        if (blogId == null) return;

        setContentView(R.layout.activity_blog);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PostAdapter(this, this);
        adapter.setHeader(this);
        recyclerView.setAdapter(adapter);

        refresh();
    }

    private void refresh() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Blogger blogger = new Blogger.Builder(
                        AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();

                try {
                    blog = blogger.blogs().get(blogId)
                            .setMaxPosts(100L)
                            .setKey(MainActivity.API_KEY).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                bindViewHolder(holder);
                if (blog.getPosts() != null && blog.getPosts().getItems() != null) {
                    adapter.setData(blog.getPosts());
                    adapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }

    @Override
    public void onClickPost(Post post) {
        PostActivity.openActivity(this, post.getBlog().getId(), post.getId());
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
            if (blog.getName() != null) holder.nameTV.setText(blog.getName());
            if (blog.getDescription() != null) holder.descTV.setText(blog.getDescription());
        }
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

    public static void openBlogActivity(Context context, String blogId) {
        Intent intent = new Intent(context, BlogActivity.class);
        intent.putExtra(BLOG_ID, blogId);
        context.startActivity(intent);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTV, descTV;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTV = (TextView) itemView.findViewById(R.id.name);
            descTV = (TextView) itemView.findViewById(R.id.description);
        }
    }
}
