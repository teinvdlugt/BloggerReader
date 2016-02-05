package com.teinvdlugt.android.bloggerreader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.Blog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = preferences.getBoolean(Constants.NOTIFICATION_PREFERENCE_KEY, true);

        if (Constants.BOOT_COMPLETED_ACTION.equals(intent.getAction())) {
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

        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                try {
                    Set<Blog> blogsWithNewPosts = new HashSet<>();
                    List<Blog> following = IOUtils.blogsFollowing(context);

                    SharedPreferences pref = context.getSharedPreferences(Constants.LAST_POST_ID_PREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor prefEditor = pref.edit();

                    Blogger blogger = IOUtils.createBloggerInstance();
                    for (Blog blog : following) {
                        try {
                            String newPostId = blogger.blogs().get(blog.getId()).setMaxPosts(1L)
                                    .setKey(Constants.API_KEY).execute().getPosts().getItems().get(0).getId();
                            String oldPostId = pref.getString(blog.getId(), null);

                            if (newPostId != null && oldPostId != null && !newPostId.equals(oldPostId)) {
                                // New post available!
                                blogsWithNewPosts.add(blog);

                                // Update most_recent_posts sharedPreferences
                                prefEditor.putString(blog.getId(), newPostId);
                            }
                        } catch (NullPointerException | IOException | IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }

                    }

                    prefEditor.apply();

                    if (!blogsWithNewPosts.isEmpty())
                        return names(blogsWithNewPosts);
                    return null;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<String> blogNames) {
                if (blogNames == null) return;
                issueNotification(context, blogNames);
            }
        }.execute();
    }

    public static void issueNotification(Context context, List<String> blogNames) {
        String title = context.getString(R.string.notification_title);
        StringBuilder message = new StringBuilder(context.getString(R.string.notification_message));
        for (int i = 0; i < blogNames.size(); i++) {
            message.append(blogNames.get(i));
            if (i == blogNames.size() - 1)
                message.append(".");
            else if (i == blogNames.size() - 2)
                message.append(" and ");
            else
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
        notificationManager.notify(Constants.NOTIFICATION_ID, notification);
    }

    private static List<String> names(Collection<Blog> blogs) {
        List<String> names = new ArrayList<>();
        for (Blog blog : blogs) {
            names.add(blog.getName());
        }
        return names;
    }
}
