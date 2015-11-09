package com.teinvdlugt.android.bloggerreader;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class IOUtils {
    private IOUtils() {
    }

    public static final String BLOGS_FOLLOWING_FILE_NAME = "blogs_following";

    public static String[] blogsFollowing(Context context) {
        try {
            FileInputStream fis = context.openFileInput(BLOGS_FOLLOWING_FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader buff = new BufferedReader(isr);

            StringBuilder file = new StringBuilder();
            String line;
            while ((line = buff.readLine()) != null)
                file.append(line).append("\n");

            return file.toString().split(",");
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0];
        }
    }
}
