package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Post;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class PostActivity extends AppCompatActivity {
    private static final String BLOG_ID = "BLOD_ID";
    private static final String POST_ID = "POST_ID";

    private String blogId, postId;

    private Post post;
    private TextView title, published, content;
    private Blogger blogger;
    private DateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        blogId = getIntent().getStringExtra(BLOG_ID);
        postId = getIntent().getStringExtra(POST_ID);
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        blogger = new Blogger.Builder(
                AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();

        title = (TextView) findViewById(R.id.post_title);
        published = (TextView) findViewById(R.id.published_time);
        content = (TextView) findViewById(R.id.content);
        content.setMovementMethod(new LinkMovementMethod());

        loadPostContents();
        refresh();
    }

    private void refresh() {
        new AsyncTask<String, Void, Post>() {
            @Override
            protected Post doInBackground(String... params) {
                try {
                    return blogger.posts().get(params[0], params[1]).setKey(MainActivity.API_KEY).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Post post) {
                PostActivity.this.post = post;
                loadPostContents();
            }
        }.execute(blogId, postId);
    }

    private void loadPostContents() {
        if (post == null) {
            content.setText("");
            published.setText("");
            title.setText("Error.");
            return;
        }

        title.setText(post.getTitle());
        published.setText(dateFormat.format(new Date(post.getPublished().getValue())));
        content.setText(post.getContent() == null ? "" : Html.fromHtml(post.getContent()));
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

    public static void openActivity(Context context, String blogId, String postId) {
        Intent intent = new Intent(context, PostActivity.class);
        intent.putExtra(POST_ID, postId);
        intent.putExtra(BLOG_ID, blogId);
        context.startActivity(intent);
    }
}
