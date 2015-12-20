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
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;

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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PostAdapter.OnPostClickListener {
    private static final String BLOG_VISIBLE_PREFERENCE = "blog_visible_";
    private static final String USE_CUSTOM_TABS_PREFERENCE = "use_custom_tabs";
    private static final String USE_CUSTOM_TABS_ASKED_PREFERENCE = "use_custom_tabs_asked";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout srLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private PostAdapter adapter;
    private Blogger blogger;
    private Map<String, Boolean> blogMap = new HashMap<>();
    private CustomTabsHelper tabsHelper;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        tabsHelper = new CustomTabsHelper();
        blogger = IOUtils.createBloggerInstance();

        // Find views
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
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
        adapter = new PostAdapter(this, this);
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

        List data = adapter.getData();
        if (totalRefresh && data != null && data.size() > 0) {
            data.clear();
            adapter.notifyDataSetChanged();
        }

        new AsyncTask<Void, Post, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if (totalRefresh) {
                    List<Blog> blogs = IOUtils.blogsFollowing(MainActivity.this);
                    SharedPreferences visibleBlogsPref = getPreferences(MODE_PRIVATE);
                    for (Iterator<Blog> it = blogs.iterator(); it.hasNext(); ) {
                        if (!visibleBlogsPref.getBoolean(BLOG_VISIBLE_PREFERENCE + it.next().getId(), true)) {
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

                    SharedPreferences.Editor lastPostIds = getSharedPreferences(IOUtils.LAST_POST_ID_PREFERENCES, MODE_PRIVATE).edit();

                    for (String blogId : blogMap.keySet()) {
                        if (!blogMap.get(blogId)) {
                            List<Post> posts = blogger.blogs().get(blogId).setMaxPosts(10L).setKey(IOUtils.API_KEY)
                                    .execute().getPosts().getItems();
                            if (posts.size() > 0)
                                lastPostIds.putString(blogId, posts.get(0).getId());

                            // Convert to native array
                            Post[] postArray = new Post[posts.size()];
                            for (int i = 0; i < postArray.length; i++) {
                                postArray[i] = posts.get(i);
                            }
                            publishProgress(postArray);

                            blogMap.put(blogId, true);
                        }
                    }

                    lastPostIds.apply();

                    /*List<Blog> blogs = IOUtils.blogsFollowing(MainActivity.this);
                    List<Post> list = new ArrayList<>();
                    for (Blog blog : blogs) {
                        try {
                            List<Post> posts = blogger.blogs().get(blog.getId()).setMaxPosts(10L).setKey(IOUtils.API_KEY)
                                    .execute().getPosts().getItems();
                            list.addAll(posts);
                            lastPostIds.putString(blog.getId(), posts.get(0).getId());
                        } catch (NullPointerException e) { *//*ignored*//* }
                    }*/

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

            /*private void sortDates(List<Post> list) {
                for (int i = list.size() - 1; i > 1; i--) {
                    for (int j = 0; j < i; j++) {
                        if (list.get(j).getPublished().getValue() < list.get(j + 1).getPublished().getValue()) {
                            Post temp = list.get(j);
                            list.set(j, list.get(j + 1));
                            list.set(j + 1, temp);
                        }
                    }
                }
            }*/

            @Override
            protected void onPostExecute(Void aVoid) {
                // adapter.setData(posts);
                // adapter.notifyDataSetChanged();
                srLayout.setRefreshing(false);

                // TODO: 20-12-2015 If no posts are displayed, display "Follow blogs" text

                if (adapter.getData() != null) {
                    for (int i = 0; i < 8 && i < adapter.getData().size(); i++) {
                        tabsHelper.mayLaunchUrl(adapter.getData().get(i).getUrl());
                    }
                }
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
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!pref.getBoolean(USE_CUSTOM_TABS_ASKED_PREFERENCE, false)) {
            // Ask whether to open post in browser or post viewer
            new AlertDialog.Builder(this)
                    .setTitle(R.string.use_custom_tabs_ask_dialog_title)
                    .setMessage(getString(R.string.use_custom_tabs_ask_dialog_message))
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pref.edit().putBoolean(USE_CUSTOM_TABS_ASKED_PREFERENCE, true)
                                    .putBoolean(USE_CUSTOM_TABS_PREFERENCE, true).apply();
                            tabsHelper.openURL(MainActivity.this, post.getUrl());
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pref.edit().putBoolean(USE_CUSTOM_TABS_ASKED_PREFERENCE, true)
                                    .putBoolean(USE_CUSTOM_TABS_PREFERENCE, false).apply();
                            PostActivity.openActivity(MainActivity.this, post.getBlog().getId(), post.getId(), post.getUrl());
                        }
                    })
                    .create().show();
        } else {
            if (pref.getBoolean(USE_CUSTOM_TABS_PREFERENCE, true)) {
                tabsHelper.openURL(this, post.getUrl());
            } else {
                PostActivity.openActivity(this, post.getBlog().getId(), post.getId(), post.getUrl());
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                navigationView.setCheckedItem(R.id.home);
                break;
            case R.id.follow_blog:
                Intent intent = new Intent(this, FollowingBlogsActivity.class);
                startActivity(intent);
                break;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            /*case R.id.notification_test:
                List<String> blogNames = new ArrayList<>();
                blogNames.add("Official Google Blog");
                blogNames.add("Research Blog");
                blogNames.add("Als docent...");
                Receiver.issueNotification(this, blogNames);
                break;*/
        }
        return false;
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
            visible[i] = pref.getBoolean(BLOG_VISIBLE_PREFERENCE + blogs.get(i).getId(), true);
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
                        editor.putBoolean(BLOG_VISIBLE_PREFERENCE + id, isChecked);
                        if (isChecked) {
                            blogMap.put(id, false);
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

    @Override
    protected void onStart() {
        super.onStart();
        tabsHelper.bindService(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        tabsHelper.unbindService(this);
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
}
