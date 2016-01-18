package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;
import com.google.api.services.blogger.model.Post;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostActivity extends AppCompatActivity {
    public static final String BLOG_ID_EXTRA = "BLOG_ID";
    public static final String POST_ID_EXTRA = "POST_ID";
    public static final String POST_URL_EXTRA = "POST_URL";

    private String blogId, postId, postUrl;

    private Post post;
    private List<ContentPiece> postContent = new ArrayList<>();
    private Blog blog;
    private TextView titleTV, publishedTV, blogNameTV, authorTV;
    private LinearLayout contentContainer;
    private Blogger blogger;
    private DateFormat dateFormat;

    private AsyncTask<String, ProgressUpdateType, Void> task;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        blogId = getIntent().getStringExtra(BLOG_ID_EXTRA);
        postId = getIntent().getStringExtra(POST_ID_EXTRA);
        postUrl = getIntent().getStringExtra(POST_URL_EXTRA);
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        blogger = new Blogger.Builder(
                AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();

        titleTV = (TextView) findViewById(R.id.post_title);
        publishedTV = (TextView) findViewById(R.id.published_time);
        contentContainer = (LinearLayout) findViewById(R.id.content_container);
        blogNameTV = (TextView) findViewById(R.id.blogName_textView);
        authorTV = (TextView) findViewById(R.id.author_byline);

        refresh();
    }

    private void refresh() {
        if (task != null) task.cancel(true);
        task = new AsyncTask<String, ProgressUpdateType, Void>() {
            private String authorByline;

            @Override
            protected Void doInBackground(String... params) {
                try {
                    post = blogger.posts().get(params[0], params[1]).setKey(Constants.API_KEY).execute();
                    publishProgress(ProgressUpdateType.TITLE_PUBLISHED);

                    if (isCancelled()) return null;

                    postContent.clear();
                    StringBuilder content = new StringBuilder(post.getContent());

                    // Remove bad ending divs
                    removeBadEndingDivs(content);

                    // Author byline
                    authorByline = parseAuthorByline(content);
                    publishProgress(ProgressUpdateType.AUTHOR_BYLINE);

                    // Content text and images
                    parseTextAndImages(content);

                    publishProgress(ProgressUpdateType.CONTENT);

                    if (isCancelled()) return null;
                    // Retrieve blog name and link
                    String blogId = post.getBlog().getId();
                    blog = blogger.blogs().get(blogId).setKey(Constants.API_KEY).execute();
                    if (blog != null) publishProgress(ProgressUpdateType.BLOG_NAME);

                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            private void removeBadEndingDivs(StringBuilder content) {
                String[] badDivClasses = {"post-author-name", "post-author-title", "post-author-team", "post-quote"};
                for (String divClass : badDivClasses) {
                    int index = content.indexOf("<div class=\"" + divClass);
                    if (index == -1) continue;
                    int end = content.indexOf("</div>", index) + 6;
                    if (end == -1) continue;
                    content.delete(index, end);
                }
            }

            private String parseAuthorByline(StringBuilder content) {
                int index = content.indexOf("<span class=\"byline-author\"");
                if (index != -1) {
                    int end = content.indexOf("</span>", index) + 7;
                    String span = content.substring(index, end);
                    content.delete(index, end);
                    Element byline = Jsoup.parse(span).body().getElementsByClass("byline-author").first();
                    String text = byline.html();
                    if (text != null) return text;
                }
                return null;
            }

            private void parseTextAndImages(StringBuilder content) {
                int index;
                while ((index = content.indexOf("<img")) != -1) {
                    int end = content.indexOf("/>", index) + 2;
                    int width = 0, height = 0;
                    String imgUrl = null;
                    try {
                        String imgTag = content.substring(index, end);
                        Element img = Jsoup.parse(imgTag).body().getElementsByTag("img").first();
                        imgUrl = img.attr("src");
                        width = Integer.parseInt(img.attr("width"));
                        height = Integer.parseInt(img.attr("height"));
                    } catch (NumberFormatException | NullPointerException e) {
                        e.printStackTrace();
                        content.delete(index, end);
                    }
                    String textBeforeImg = content.substring(0, index);
                    postContent.add(new TextPiece(textBeforeImg));
                    if (imgUrl != null) postContent.add(new Image(imgUrl, width, height));
                    content.delete(0, end);
                }
                postContent.add(new TextPiece(content.toString()));
            }

            @Override
            protected void onProgressUpdate(ProgressUpdateType... type) {
                switch (type[0]) {
                    case TITLE_PUBLISHED:
                        titleTV.setText(post.getTitle());
                        setDate(post.getPublished().getValue());
                        break;
                    case CONTENT:
                        loadPostContents();
                        break;
                    case BLOG_NAME:
                        blogNameTV.setVisibility(View.VISIBLE);
                        blogNameTV.setText(blog.getName());
                        blogNameTV.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                BlogActivity.openBlogActivity(PostActivity.this, blog.getId());
                            }
                        });
                        break;
                    case AUTHOR_BYLINE:
                        if (authorByline == null) {
                            authorTV.setVisibility(View.GONE);
                            authorTV.setText("");
                        } else {
                            authorTV.setVisibility(View.VISIBLE);
                            authorTV.setText(Html.fromHtml(authorByline));
                        }
                        break;
                }
            }
        };
        task.execute(blogId, postId);
    }

    private void setDate(long millis) {
        publishedTV.setText(dateFormat.format(new Date(millis)));
    }

    private void loadPostContents() {
        contentContainer.removeAllViews();

        for (final ContentPiece piece : postContent) {
            if (piece instanceof TextPiece)
                contentContainer.addView(newContentTextView(piece.content));
            else if (piece instanceof Image) {
                final ImageView imageView = newContentImageView();
                contentContainer.addView(imageView);
                Picasso.with(this).load(piece.content).into(imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        imageView.getLayoutParams().height =
                                (int) ((float) imageView.getWidth() / ((Image) piece).width * ((Image) piece).height);
                        imageView.requestLayout();
                    }

                    public void onError() {
                        imageView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private TextView newContentTextView(String html) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(params);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(18);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setText(Html.fromHtml(html));

        int _16dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        params.leftMargin = params.rightMargin = _16dp;

        return tv;
    }

    private ImageView newContentImageView() {
        final ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageView.setLayoutParams(params);
        return imageView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.view_in_browser:
            case R.id.copy_url:
                String url = post != null && post.getUrl() != null ? post.getUrl() : postUrl;
                if (url != null && item.getItemId() == R.id.view_in_browser)
                    MainActivity.openURLInBrowser(this, url);
                else if (url != null && item.getItemId() == R.id.copy_url)
                    MainActivity.copyTextToClipboard(this, "Blogger url", url);
                else return false;
                return true;
        }
        return false;
    }

    public static void openPost(final CustomTabsActivity activity, final String blogId, final String postId, final String url) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!pref.getBoolean(Constants.USE_CUSTOM_TABS_ASKED_PREFERENCE, false)) { // TODO move preference vars to SettingsActivity or Constants.class
            // Ask whether to open post in browser or post viewer
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.use_custom_tabs_ask_dialog_title)
                    .setMessage(R.string.use_custom_tabs_ask_dialog_message)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pref.edit().putBoolean(Constants.USE_CUSTOM_TABS_ASKED_PREFERENCE, true)
                                    .putBoolean(Constants.USE_CUSTOM_TABS_PREFERENCE, true).apply();
                            activity.tabsHelper.openURL(activity, url);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pref.edit().putBoolean(Constants.USE_CUSTOM_TABS_ASKED_PREFERENCE, true)
                                    .putBoolean(Constants.USE_CUSTOM_TABS_PREFERENCE, false).apply();
                            PostActivity.openActivity(activity, blogId, postId, url);
                        }
                    })
                    .create().show();
        } else {
            if (pref.getBoolean(Constants.USE_CUSTOM_TABS_PREFERENCE, true)) {
                activity.tabsHelper.openURL(activity, url);
            } else {
                PostActivity.openActivity(activity, blogId, postId, url);
            }
        }
    }

    public static void openActivity(Context context, String blogId, String postId, String url) {
        Intent intent = new Intent(context, PostActivity.class);
        intent.putExtra(POST_ID_EXTRA, postId);
        intent.putExtra(BLOG_ID_EXTRA, blogId);
        intent.putExtra(POST_URL_EXTRA, url);
        context.startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        task.cancel(true);
    }

    private abstract class ContentPiece {
        final String content;

        public ContentPiece(String content) {
            this.content = content;
        }
    }

    private class TextPiece extends ContentPiece {
        public TextPiece(String content) {
            super(content);
        }
    }

    private class Image extends ContentPiece {
        final int width, height;

        public Image(String content, int width, int height) {
            super(content);
            this.width = width;
            this.height = height;
        }
    }

    private class AuthorByline extends ContentPiece {
        public AuthorByline(String content) {
            super(content);
        }
    }

    private enum ProgressUpdateType {TITLE_PUBLISHED, CONTENT, BLOG_NAME, AUTHOR_BYLINE}
}
