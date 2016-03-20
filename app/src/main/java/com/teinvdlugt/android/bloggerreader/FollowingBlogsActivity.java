package com.teinvdlugt.android.bloggerreader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FollowingBlogsActivity extends AppCompatActivity implements FollowingBlogsAdapter.CallBacks {
    public static final String ADD_BLOG_EXTRA = "add_blog";

    private FollowingBlogsAdapter adapter;
    private RecyclerView recyclerView;
    private Snackbar notFollowingBlogsSnackbar;

    private ArrayList<String> deletedBlogs = new ArrayList<>();
    private ArrayList<String> addedBlogs = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_blogs);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
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
            notFollowingBlogsSnackbar = Snackbar.make(recyclerView, R.string.not_yet_following_blogs, Snackbar.LENGTH_INDEFINITE);
            notFollowingBlogsSnackbar.setAction(R.string.follow_first_blog, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickFollowFirstBlog(v);
                }
            });
            notFollowingBlogsSnackbar.show();
        } else {
            if (notFollowingBlogsSnackbar != null) notFollowingBlogsSnackbar.dismiss();
            adapter.setData(this, blogs, this);
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

    private boolean progressDialogDismissed = false;

    private void processURL(final String url) {
        progressDialogDismissed = false;

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading_blog));
        dialog.setIndeterminate(true);
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                progressDialogDismissed = true;
            }
        });
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        new AsyncTask<Void, Void, Blog>() {
            private String url2;
            private String urlWithScheme;

            @Override
            protected Blog doInBackground(Void... params) {
                try {
                    url2 = url;

                    if (!url2.contains(".blogspot."))
                        url2 += ".blogspot.com";
                    urlWithScheme = url2;
                    if (!(url2.startsWith("http://") || url2.startsWith("https://")))
                        urlWithScheme = "http://" + urlWithScheme;

                    Blogger blogger = IOUtils.createBloggerInstance();
                    return blogger.blogs().getByUrl(urlWithScheme).setKey(Constants.API_KEY).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Blog blog) {
                if (progressDialogDismissed) return;

                dialog.dismiss();
                if (blog == null) showBlogNotFoundDialog(url2);
                else {
                    List<Blog> followingBlogs = IOUtils.blogsFollowing(FollowingBlogsActivity.this);
                    List<String> ids = new ArrayList<>();
                    for (Blog followingBlog : followingBlogs)
                        ids.add(followingBlog.getId());

                    if (ids.contains(blog.getId()))
                        showAlreadyFollowingBlogDialog(blog.getName());
                    else showAddBlogDialog(blog);
                }
            }
        }.execute();
    }

    private void showAddBlogDialog(final Blog blog) {
        String msg = getString(R.string.do_you_want_to_follow_this_blog)
                + "<br>" + getString(R.string.name_colon, "<b>" + blog.getName() + "</b>")
                + "<br>" + getString(R.string.url_colon, blog.getUrl());
        new AlertDialog.Builder(this)
                .setMessage(Html.fromHtml(msg))
                .setPositiveButton(R.string.follow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (IOUtils.saveBlog(FollowingBlogsActivity.this, blog)) {
                            Snackbar.make(findViewById(R.id.recyclerView),
                                    getString(R.string.following_blog, blog.getName()), Snackbar.LENGTH_LONG).show();
                            refresh();

                            if (!deletedBlogs.remove(blog.getId()))
                                addedBlogs.add(blog.getId());
                        } else {
                            Snackbar.make(findViewById(R.id.recyclerView), R.string.following_blog_error, Snackbar.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, null)
                .create().show();
    }

    private void showAlreadyFollowingBlogDialog(String name) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.already_following_blog_dialog_title)
                .setMessage(Html.fromHtml(getString(R.string.already_following_blog_dialog_message,
                        "<b>" + name + "</b>")))
                .setPositiveButton(R.string.ok, null)
                .create().show();
    }

    private void showBlogNotFoundDialog(String url) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.blog_not_found_dialog_title)
                .setMessage(getString(R.string.blog_not_found, url))
                .setPositiveButton(R.string.ok, null)
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

    @Override
    public void finish() {
        setResultExtras();
        super.finish();
    }

    private void setResultExtras() {
        // Intent to put data to for MainActivity
        Intent intent = new Intent();
        if (addedBlogs != null) intent.putExtra(MainActivity.ADDED_BLOGS_EXTRA, addedBlogs);
        if (addedBlogs != null) intent.putExtra(MainActivity.DELETED_BLOGS_EXTRA, deletedBlogs);
        setResult(RESULT_OK, intent);
    }

    private void deleteBlog(final Blog blog) {
        new AlertDialog.Builder(this)
                .setTitle("Unfollow blog")
                .setMessage(Html.fromHtml(getString(R.string.unfollow_blog_confirmation_message, blog.getName())))
                .setPositiveButton(R.string.unfollow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = adapter.getData().indexOf(blog);
                        if (position != -1 && adapter.getData().remove(blog)) {
                            IOUtils.overwriteBlogs(FollowingBlogsActivity.this, adapter.getData());
                            adapter.notifyItemRemoved(position);
                        }

                        if (!addedBlogs.remove(blog.getId()))
                            deletedBlogs.add(blog.getId());
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

    @Override
    public void onLongClickBlog(Blog blog) {
        deleteBlog(blog);
    }

    @Override
    public void onClickFollowNewBlog() {
        onClickAddBlog(null);
    }
}

class FollowingBlogsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_BLOG = 0;
    private static final int VIEW_TYPE_NEW_BLOG = 1;
    private List<Blog> data = new ArrayList<>();
    private Context context;
    private CallBacks callBacks;

    interface CallBacks {
        void onLongClickBlog(Blog blog);
        void onClickFollowNewBlog();
    }

    public void setData(Context context, List<Blog> data, CallBacks callBacks) {
        this.data = data;
        this.context = context;
        this.callBacks = callBacks;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_BLOG:
                View view = LayoutInflater.from(context).inflate(R.layout.list_item_following_blog, parent, false);
                return new ViewHolder(view);
            case VIEW_TYPE_NEW_BLOG:
                View newBlogView = LayoutInflater.from(context).inflate(R.layout.list_item_follow_new_blog, parent, false);
                return new NewBlogViewHolder(newBlogView);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof NewBlogViewHolder) {
            ((NewBlogViewHolder) holder).root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callBacks != null)
                        callBacks.onClickFollowNewBlog();
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return position == data.size() ? VIEW_TYPE_NEW_BLOG : VIEW_TYPE_BLOG;
    }

    @Override
    public int getItemCount() {
        return data.size() + 1;
    }

    public List<Blog> getData() {
        return data;
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
                    BlogActivity.openBlogActivity(context, blog.getName(), blog.getId());
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (callBacks != null)
                        callBacks.onLongClickBlog(blog);
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

    class NewBlogViewHolder extends RecyclerView.ViewHolder {
        View root;

        public NewBlogViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
        }
    }
}
