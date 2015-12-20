package com.teinvdlugt.android.bloggerreader;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

public class CustomTabsHelper {
    private CustomTabsClient client;
    private CustomTabsSession session;
    private CustomTabsServiceConnection connection;

    public void openURL(Activity activity, String url) {
        String packageName = getChromePackageName();

        if (packageName == null) {
            // No Chrome Custom Tabs support; fall back to normal Intent
            Intent classicIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(classicIntent);
        } else {
            CustomTabsIntent intent = new CustomTabsIntent.Builder()
                    .setToolbarColor(activity.getResources().getColor(R.color.colorPrimary))
                    .build();

            intent.intent.setPackage(packageName);
            intent.launchUrl(activity, Uri.parse(url));
        }
    }

    public void bindService(Activity activity) {
        if (client != null) return;

        String packageName = getChromePackageName();
        if (packageName == null) return;

        connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                CustomTabsHelper.this.client = client;
                client.warmup(0L);
                // Initialize a session as soon as possible.
                getSession();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                client = null;
            }
        };
        CustomTabsClient.bindCustomTabsService(activity, packageName, connection);
    }

    public void unbindService(Activity activity) {
        if (connection == null) return;
        activity.unbindService(connection);
        client = null;
        session = null;
    }

    public CustomTabsSession getSession() {
        if (client == null) {
            session = null;
        } else if (session == null) {
            session = client.newSession(null);
        }
        return session;
    }

    public void mayLaunchUrl(String url) {
        if (client == null) return;

        CustomTabsSession session = getSession();
        if (session == null) return;

        session.mayLaunchUrl(Uri.parse(url), null, null);
    }

    public static String getChromePackageName() {
        return "com.chrome.dev";
    }
}
