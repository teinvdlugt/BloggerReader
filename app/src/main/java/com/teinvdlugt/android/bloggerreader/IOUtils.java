package com.teinvdlugt.android.bloggerreader;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {
    public static final String API_KEY = "AIzaSyAsG_pjWPPXYWq68igzilu77ss0qRP5yM8";

    private IOUtils() {
    }

    public static final String BLOGS_FOLLOWING_FILE_NAME = "blogs_following";

    public static List<Blog> blogsFollowing(Context context) {
        // See overwriteBlogs(Context context, List<Blog> blogs) method for
        // info about file structure
        try {
            FileInputStream fis = context.openFileInput(BLOGS_FOLLOWING_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader buff = new BufferedReader(isr);

            List<Blog> result = new ArrayList<>();
            String line;
            while ((line = buff.readLine()) != null) {
                String[] info = line.split(",");
                Blog blog = new Blog();
                blog.setId(info[0]);
                blog.setName(info[1]);
                blog.setUrl(info[2]);
                result.add(blog);
            }

            isr.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static boolean saveBlog(Context context, Blog blog) {
        // TODO: 15-11-2015 Use Context.MODE_APPEND
        List<Blog> saved = blogsFollowing(context);
        saved.add(blog);
        return overwriteBlogs(context, saved);
    }

    public static boolean overwriteBlogs(Context context, List<Blog> blogs) {
        // File should have the following structure:
        // blog1Id,blog1Name,blog1Url\n
        // blog2Id,blog2Name,blog2Url\n
        // etc. etc.
        try {
            FileOutputStream fos = context.openFileOutput(BLOGS_FOLLOWING_FILE_NAME, Context.MODE_PRIVATE);

            StringBuilder sb = new StringBuilder();
            for (Blog blog : blogs) {
                sb.append(blog.getId()).append(",")
                        .append(blog.getName()).append(",")
                        .append(blog.getUrl()).append("\n");
            }

            fos.write(sb.toString().getBytes());
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Blogger createBloggerInstance() {
        return new Blogger.Builder(
                AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), null).build();
    }

    public static boolean checkNotConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return networkInfo == null || !networkInfo.isConnected();
    }
}
