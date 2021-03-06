package com.teinvdlugt.android.bloggerreader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends CustomTabsActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PostAdapter.OnPostClickListener, PostAdapter.LoadNextBatchListener {
    private static final int FOLLOWING_BLOGS_ACTIVITY_REQUEST_CODE = 1;
    public static final String ADDED_BLOGS_EXTRA = "added_blogs";
    public static final String DELETED_BLOGS_EXTRA = "deleted_blogs";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout srLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;
    private Snackbar notFollowingBlogsSnackbar;
    private final int batchSize = 20; // Per blog
    private int numOfBatches = 0;

    private PostAdapter adapter;
    private Blogger blogger;
    private Map<String, Boolean> blogMap = new HashMap<>();

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        // TODO Integrate Google Analytics

        blogger = IOUtils.createBloggerInstance();

        // Find views
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        srLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        navigationView = (NavigationView) findViewById(R.id.navigationView);

        // Drawer layout and stuff
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        navigationView.setNavigationItemSelectedListener(this);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.xs_open_drawer, R.string.xs_close_drawer);

        // Swipe-refresh
        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh(true);
            }
        });

        // RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        adapter = new PostAdapter(this, this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        // Refresh
        refresh(true);
    }

    private void refresh(final boolean totalRefresh) {
        if (!srLayout.isRefreshing()) {
            srLayout.post(new Runnable() {
                @Override
                public void run() {
                    srLayout.setRefreshing(true);
                }
            });
        }

        numOfBatches = 1;

        List data = adapter.getData();
        if (totalRefresh && data != null && data.size() > 0) {
            data.clear();
            adapter.notifyDataSetChanged();
        }

        final List<Blog> blogs = IOUtils.blogsFollowing(MainActivity.this);
        if (blogs.isEmpty()) {
            notFollowingBlogsSnackbar = Snackbar.make(recyclerView, R.string.not_yet_following_blogs, Snackbar.LENGTH_INDEFINITE);
            notFollowingBlogsSnackbar.setAction(R.string.follow_first_blog, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickFollowFirstBlog(v);
                }
            });
            notFollowingBlogsSnackbar.show();
            srLayout.post(new Runnable() {
                @Override
                public void run() {
                    srLayout.setRefreshing(false);
                }
            });
            return;
        } else if (notFollowingBlogsSnackbar != null) {
            notFollowingBlogsSnackbar.dismiss();
        }

        // TODO: 5-2-2016 Use AsyncTaskLoader to maintain data after configuration change

        new AsyncTask<Void, Post, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (totalRefresh) {
                    SharedPreferences visibleBlogsPref = getPreferences(MODE_PRIVATE);
                    for (Iterator<Blog> it = blogs.iterator(); it.hasNext(); ) {
                        if (!visibleBlogsPref.getBoolean(Constants.BLOG_VISIBLE_PREFERENCE + it.next().getId(), true)) {
                            it.remove();
                        }
                    }
                    blogMap.clear();
                    for (Blog blog : blogs) {
                        blogMap.put(blog.getId(), false);
                    }
                }

                try {
                    if (IOUtils.checkNotConnected(MainActivity.this)) return null;

                    SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit();

                    for (String blogId : blogMap.keySet()) {
                        if (!blogMap.get(blogId)) {
                            List<Post> posts = blogger.blogs().get(blogId).setMaxPosts((long) batchSize).setKey(Constants.API_KEY)
                                    .execute().getPosts().getItems();
                            if (posts.size() > 0)
                                prefEditor.putString(Constants.LAST_POST_ID_PREF + blogId, posts.get(0).getId());

                            // Convert to native array
                            Post[] postArray = new Post[posts.size()];
                            for (int i = 0; i < postArray.length; i++) {
                                postArray[i] = posts.get(i);
                            }
                            publishProgress(postArray);

                            blogMap.put(blogId, true);
                        }
                    }

                    prefEditor.apply();

                    return null;

                    // Official Google Blog: 10861780
                    // Mike Louwman: 5563501798919888465
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onProgressUpdate(Post... posts) {
                List<Post> data = adapter.getData();
                List<Integer> positions = new ArrayList<>();

                if (data == null || data.isEmpty()) {
                    data = new ArrayList<>();
                    Collections.addAll(data, posts);
                    adapter.setData(data);
                    adapter.notifyItemRangeInserted(0, data.size());
                    return;
                }

                int d = 0; // adapter data index
                for (int p = 0; p < posts.length; p++) {
                    for (; d < data.size(); d++) {
                        if (data.get(d).getPublished().getValue() < posts[p].getPublished().getValue()) {
                            data.add(d, posts[p]);
                            positions.add(d);
                            break;
                        }
                    }

                    if (d == data.size()) {
                        // Add all remaining posts to end of adapter data
                        for (; p < posts.length; p++) {
                            data.add(posts[p]);
                            positions.add(data.size() - 1);
                        }
                        break;
                    }
                }

                for (int i : positions) {
                    adapter.notifyItemInserted(i);
                }

                recyclerView.smoothScrollToPosition(0);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                srLayout.setRefreshing(false);

                if (adapter.getData() != null && PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                        .getBoolean(Constants.USE_CUSTOM_TABS_PREFERENCE, true)) {
                    for (int i = 0; i < 8 && i < adapter.getData().size(); i++) {
                        tabsHelper.mayLaunchUrl(adapter.getData().get(i).getUrl());
                    }
                }
            }
        }.execute();
    }

    @Override
    public void loadNextBatch() {
        numOfBatches++;
        new AsyncTask<Void, Void, List<Post>>() {
            @Override
            protected List<Post> doInBackground(Void... params) {
                try {
                    if (IOUtils.checkNotConnected(MainActivity.this)) return null;

                    List<Post> batch = new ArrayList<>();

                    for (String blogId : blogMap.keySet()) {
                        List<Post> posts = blogger.blogs().get(blogId).setMaxPosts(numOfBatches * (long) batchSize)
                                .setKey(Constants.API_KEY).execute().getPosts().getItems();
                        batch.addAll(posts.subList((numOfBatches - 1) * batchSize, posts.size()));

                        // TODO: 23-1-2016 Sort posts
                    }

                    return batch;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Post> posts) {
                super.onPostExecute(posts);
                adapter.nextBatchLoaded(posts, true); // TODO: 23-1-2016 Look if more posts are coming
            }
        }.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onClickPost(final Post post) {
        PostActivity.openPost(this, post.getBlog().getId(), post.getId(), post.getUrl());
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        drawerLayout.closeDrawer(navigationView);
        switch (item.getItemId()) {
            case R.id.home:
                navigationView.setCheckedItem(R.id.home);
                break;
            case R.id.follow_blog:
                Intent intent = new Intent(this, FollowingBlogsActivity.class);
                startActivityForResult(intent, FOLLOWING_BLOGS_ACTIVITY_REQUEST_CODE);
                break;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLLOWING_BLOGS_ACTIVITY_REQUEST_CODE
                && resultCode == RESULT_OK
                && data != null) {
            List<String> deletedBlogs = data.getStringArrayListExtra(DELETED_BLOGS_EXTRA);
            List<String> addedBlogs = data.getStringArrayListExtra(ADDED_BLOGS_EXTRA);

            if (deletedBlogs != null && !deletedBlogs.isEmpty())
                removeBlogs(deletedBlogs);
            if (addedBlogs != null && !addedBlogs.isEmpty()) {
                for (String blogId : addedBlogs)
                    blogMap.put(blogId, false);
                refresh(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh(true);
                return true;
            case R.id.blog_visibility:
                showBlogVisibilityDialog();
        }
        return false;
    }

    private void showBlogVisibilityDialog() {
        final List<Blog> blogs = IOUtils.blogsFollowing(this);
        CharSequence[] names = new CharSequence[blogs.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = Html.fromHtml(blogs.get(i).getName());
        }

        boolean[] visible = new boolean[blogs.size()];
        SharedPreferences pref = getPreferences(MODE_PRIVATE); // Local to activity
        for (int i = 0; i < visible.length; i++) {
            visible[i] = pref.getBoolean(Constants.BLOG_VISIBLE_PREFERENCE + blogs.get(i).getId(), true);
        }

        final HashMap<String, Boolean> blogMapBackup = new HashMap<>(blogMap);
        final List<String> removeBlogs = new ArrayList<>();

        final SharedPreferences.Editor editor = pref.edit();
        new AlertDialog.Builder(this)
                .setTitle(R.string.visible_blogs)
                .setMultiChoiceItems(names, visible, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        String id = blogs.get(which).getId();
                        editor.putBoolean(Constants.BLOG_VISIBLE_PREFERENCE + id, isChecked);
                        if (isChecked) {
                            if (blogMapBackup.get(id) != null) {
                                blogMap.put(id, blogMapBackup.get(id));
                            } else {
                                blogMap.put(id, false);
                            }

                            removeBlogs.remove(id);
                        } else {
                            blogMap.remove(id);
                            removeBlogs.add(id);
                        }
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.apply();
                        removeBlogs(removeBlogs);
                        refresh(false);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        blogMap = blogMapBackup;
                    }
                })
                .create().show();
    }

    private void removeBlogs(List<String> ids) {
        List<Post> data = adapter.getData();
        if (data == null || data.size() == 0) return;

        for (int i = 0; i < data.size(); i++) {
            if (ids.contains(data.get(i).getBlog().getId())) {
                data.remove(i);
                adapter.notifyItemRemoved(i);
                i--;
            }
        }
    }

    public static boolean openURLInBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        boolean safe = activities.size() > 0;
        if (safe) context.startActivity(intent);
        return safe;
    }

    public static void copyTextToClipboard(Context context, String label, String text) {
        if (Build.VERSION.SDK_INT >= 11) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);
        }
    }

    public void onClickFollowFirstBlog(View view) {
        Intent intent = new Intent(this, FollowingBlogsActivity.class);
        intent.putExtra(FollowingBlogsActivity.ADD_BLOG_EXTRA, true);
        startActivity(intent);
    }
}
