package com.teinvdlugt.android.bloggerreader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FollowingBlogsActivity extends AppCompatActivity {
    public static final String ADD_BLOG_EXTRA = "add_blog";

    private FollowingBlogsAdapter adapter;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_blogs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.follow_first_blog_button).getBackground().setColorFilter(
                IOUtils.getColor(this, R.color.colorAccent), PorterDuff.Mode.MULTIPLY);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowingBlogsAdapter();
        recyclerView.setAdapter(adapter);

        refresh();

        if (getIntent().getBooleanExtra(ADD_BLOG_EXTRA, false))
            onClickAddBlog(null);
    }

    private void refresh() {
        List<Blog> blogs = IOUtils.blogsFollowing(this);

        if (blogs.isEmpty()) {
            findViewById(R.id.not_yet_following_blogs_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.not_yet_following_blogs_layout).setVisibility(View.GONE);
            adapter.setData(blogs);
            adapter.notifyDataSetChanged();
        }
    }

    public void onClickAddBlog(View view) {
        final EditText et = new EditText(this);
        et.setHint(R.string.blog_url);
        TextInputLayout til = new TextInputLayout(this);
        int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        til.setPadding(_16dp, _16dp, _16dp, 0);
        til.addView(et);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.add_blog_dialog_title)
                .setView(til)
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        processURL(et.getText().toString());
                    }
                }).setNegativeButton(R.string.cancel, null).create();

        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /*ignored*/ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(et.length() != 0);
            }

            public void afterTextChanged(Editable s) { /*ignored*/ }
        });

        dialog.show();
    }

    private void processURL(final String url) {
        ProgressBar pBar = new ProgressBar(this);
        TextView msg = new TextView(this);

        // Progress bar layout params
        int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams pBarParams = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pBarParams.gravity = Gravity.CENTER_VERTICAL;
        pBarParams.setMargins(_16dp, _16dp, _16dp, _16dp);
        pBar.setLayoutParams(pBarParams);

        // TextView layout params
        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewParams.gravity = Gravity.CENTER_VERTICAL;
        msg.setText(R.string.loading_blog);
        msg.setLayoutParams(textViewParams);

        // Container
        LinearLayout ll = new LinearLayout(this);
        ll.addView(pBar);
        ll.addView(msg);

        final AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(ll).create();
        progressDialog.show();

        new AsyncTask<Void, Void, Blog>() {
            @Override
            protected Blog doInBackground(Void... params) {
                try {
                    String url2 = url;
                    if (!(url2.startsWith("http://") || url2.startsWith("https://")))
                        url2 = "http://" + url2;

                    Blogger blogger = IOUtils.createBloggerInstance();
                    return blogger.blogs().getByUrl(url2).setKey(Constants.API_KEY).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Blog blog) {
                progressDialog.dismiss();
                if (blog == null) showBlogNotFoundDialog(url);
                else showAddBlogDialog(blog);
            }
        }.execute();
    }

    private void showAddBlogDialog(final Blog blog) {
        String msg = getString(R.string.do_you_want_to_follow_this_blog, "<b>" + blog.getName() + "</b>\n", blog.getUrl());
        new AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(msg))
                .setPositiveButton(R.string.follow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (IOUtils.saveBlog(FollowingBlogsActivity.this, blog)) {
                            Snackbar.make(findViewById(R.id.recyclerView),
                                    getString(R.string.following_blog, blog.getName()), Snackbar.LENGTH_LONG).show();
                            List<Blog> blogs = IOUtils.blogsFollowing(FollowingBlogsActivity.this);
                            adapter.setData(blogs);
                            adapter.notifyItemInserted(adapter.getItemCount() - 1);
                        } else {
                            Snackbar.make(findViewById(R.id.recyclerView), R.string.following_blog_error, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    private void showBlogNotFoundDialog(String url) {
        new AlertDialog.Builder(this)
                .setTitle("Blog not found")
                .setMessage(getString(R.string.blog_not_found, url))
                .create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_following_blogs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.add_blog_action:
                onClickAddBlog(null);
            default:
                return true;
        }
    }

    private void deleteBlog(final Blog blog) {
        new AlertDialog.Builder(this)
                .setTitle("Unfollow blog")
                .setMessage(Html.fromHtml(getString(R.string.unfollow_blog_confirmation_message, blog.getName())))
                .setPositiveButton(R.string.unfollow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = adapter.data.indexOf(blog);
                        if (position != -1 && adapter.data.remove(blog)) {
                            IOUtils.overwriteBlogs(FollowingBlogsActivity.this, adapter.data);
                            adapter.notifyItemRemoved(position);
                        }
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    public void onClickFollowFirstBlog(View view) {
        onClickAddBlog(view);
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
            private Blog blog;

            public ViewHolder(View itemView) {
                super(itemView);
                nameTV = (TextView) itemView.findViewById(R.id.blog_name);
                urlTV = (TextView) itemView.findViewById(R.id.blog_url);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BlogActivity.openBlogActivity(FollowingBlogsActivity.this, blog.getName(), blog.getId());
                    }
                });
                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        deleteBlog(blog);
                        return false;
                    }
                });
            }

            public void bind(Blog data) {
                this.blog = data;

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