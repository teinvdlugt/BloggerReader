package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {
    public static final String API_KEY = "AIzaSyAsG_pjWPPXYWq68igzilu77ss0qRP5yM8";

    private PostAdapter adapter;
    private Blogger blogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        blogger = new Blogger.Builder(
                AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        adapter = new PostAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        new AsyncTask<Void, Void, Blog.Posts>() {
            @Override
            protected Blog.Posts doInBackground(Void... params) {
                try {
                    if (checkNotConnected()) return new Blog.Posts();

                    Blog googleblog = blogger.blogs().get("10861780").setMaxPosts(50l).setKey(API_KEY).execute();
                    return googleblog.getPosts();

                    // Official Google Blog: 10861780
                    // Mike Louwman: 5563501798919888465
                } catch (IOException e) {
                    e.printStackTrace();
                    return new Blog.Posts();
                }
            }

            @Override
            protected void onPostExecute(Blog.Posts posts) {
                adapter.setData(posts);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public void onClickPost(Post post) {
        PostActivity.openActivity(this, post.getBlog().getId(), post.getId());
    }

    private boolean checkNotConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return networkInfo == null || !networkInfo.isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
