package com.teinvdlugt.android.bloggerreader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Receiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = preferences.getBoolean(AlarmUtils.NOTIFICATION_PREFERENCE_KEY, true);

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            // Phone just booted, reset alarm
            if (notifications) AlarmUtils.setOrCancelAlarm(context, true);
        }

        if (!notifications) {
            // This receiver shouldn't be called (except on boot),
            // Alarm has to be cancelled
            AlarmUtils.setOrCancelAlarm(context, false);
            return;
        }

        if (IOUtils.checkNotConnected(context)) return;

        try {
            List<Blog> blogsWithNewPosts = new ArrayList<>();

            List<Blog> following = IOUtils.blogsFollowing(context);
            Map<String, String> lastPostIds = IOUtils.getLastPostIds(context);

            Blogger blogger = IOUtils.createBloggerInstance();
            for (Blog blog : following) {
                String newPostId = blogger.blogs().get(blog.getId()).setMaxPosts(1L)
                        .setKey(IOUtils.API_KEY).execute().getPosts().getItems().get(0).getId();
                String oldPostId = lastPostIds.get(blog.getId());

                if (newPostId != null && !newPostId.equals(oldPostId)) {
                    // New post available!
                    blogsWithNewPosts.add(blog);
                }
            }

            if (!blogsWithNewPosts.isEmpty())
                issueNotification(context, names(blogsWithNewPosts));

        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void issueNotification(Context context, List<String> blogNames) {
        String title = context.getString(R.string.notification_title);
        StringBuilder message = new StringBuilder(context.getString(R.string.notification_message));
        for (int i = 0; i < blogNames.size(); i++) {
            message.append(blogNames.get(i));
            if (i == blogNames.size() - 1)
                message.append(".");
            else if (i != 1)
                message.append(", ");
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message.toString())
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_add_white_36dp)
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message.toString())) // Make notification expandable
                .setAutoCancel(true)
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private static List<String> names(List<Blog> blogs) {
        List<String> names = new ArrayList<>();
        for (Blog blog : blogs) {
            names.add(blog.getName());
        }
        return names;
    }
}