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
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PostAdapter.OnPostClickListener {
    private static final String BLOG_VISIBLE_PREFERENCE = "blog_visible_";

    private SwipeRefreshLayout srLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;

    private PostAdapter adapter;
    private Blogger blogger;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

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
                refresh();
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        adapter = new PostAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        refresh();
    }

    private void refresh() {
        if (!srLayout.isRefreshing()) {
            srLayout.post(new Runnable() {
                @Override
                public void run() {
                    srLayout.setRefreshing(true);
                }
            });
        }

        new AsyncTask<Void, Void, List<Post>>() {
            @Override
            protected List<Post> doInBackground(Void... params) {
                try {
                    if (IOUtils.checkNotConnected(MainActivity.this)) return new ArrayList<>();

                    SharedPreferences visibleBlogsPref = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor lastPostIds = getSharedPreferences(IOUtils.LAST_POST_ID_PREFERENCES, MODE_PRIVATE).edit();
                    List<Blog> blogs = IOUtils.blogsFollowing(MainActivity.this);
                    List<Post> list = new ArrayList<>();
                    for (Blog blog : blogs) {
                        if (visibleBlogsPref.getBoolean(BLOG_VISIBLE_PREFERENCE + blog.getId(), true)) {
                            try {
                                List<Post> posts = blogger.blogs().get(blog.getId()).setMaxPosts(10L).setKey(IOUtils.API_KEY)
                                        .execute().getPosts().getItems();
                                list.addAll(posts);
                                lastPostIds.putString(blog.getId(), posts.get(0).getId());
                            } catch (NullPointerException e) { /*ignored*/ }
                        }
                    }

                    lastPostIds.apply();

                    sortDates(list);
                    return list;

                    // Official Google Blog: 10861780
                    // Mike Louwman: 5563501798919888465
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            private void sortDates(List<Post> list) {
                for (int i = list.size() - 1; i > 1; i--) {
                    for (int j = 0; j < i; j++) {
                        if (list.get(j).getPublished().getValue() < list.get(j + 1).getPublished().getValue()) {
                            Post temp = list.get(j);
                            list.set(j, list.get(j + 1));
                            list.set(j + 1, temp);
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Post> posts) {
                adapter.setData(posts);
                adapter.notifyDataSetChanged();
                srLayout.setRefreshing(false);
            }
        }.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    @Override
    public void onClickPost(Post post) {
        PostActivity.openActivity(this, post.getBlog().getId(), post.getId(), post.getUrl());
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
                refresh();
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

        final SharedPreferences.Editor editor = pref.edit();
        new AlertDialog.Builder(this)
                .setTitle(R.string.visible_blogs)
                .setMultiChoiceItems(names, visible, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        editor.putBoolean(BLOG_VISIBLE_PREFERENCE + blogs.get(which).getId(), isChecked);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editor.apply();
                        refresh();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create().show();
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
