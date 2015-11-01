package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
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
    private static final String BLOG_ID = "BLOD_ID";
    private static final String POST_ID = "POST_ID";

    private String blogId, postId;

    private Post post;
    private List<ContentPiece> postContent = new ArrayList<>();
    private TextView title, published;
    private LinearLayout contentContainer;
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
        contentContainer = (LinearLayout) findViewById(R.id.content_container);

        refresh();
    }

    private void refresh() {
        new AsyncTask<String, Void, Post>() {
            @Override
            protected Post doInBackground(String... params) {
                try {
                    Post post = blogger.posts().get(params[0], params[1]).setKey(MainActivity.API_KEY).execute();

                    String content = post.getContent();
                    content = "<p>" + content + "</p>";
                    content = content.replaceAll("<div class=\"separator\"", "</p><div class=\"separator\"");
                    content = content.replaceAll("/></a></div>", "/></a></div><p>");

                    postContent.clear();
                    Element body = Jsoup.parse(content).body();
                    for (Element element : body.children()) {
                        if ("p".equals(element.tagName()))
                            postContent.add(new TextPiece(element.html()));
                        else if ("div".equals(element.tagName())) {
                            try {
                                Element img = element.getElementsByTag("img").first();
                                postContent.add(new Image(img.attr("src"),
                                        Integer.parseInt(img.attr("width")), Integer.parseInt(img.attr("height"))));
                            } catch (NullPointerException | NumberFormatException ignored) {
                            }
                        }
                    }

                    return post;
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
            contentContainer.removeAllViews();
            published.setText("");
            title.setText("Error.");
            return;
        }

        title.setText(post.getTitle());
        published.setText(dateFormat.format(new Date(post.getPublished().getValue())));
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
}
