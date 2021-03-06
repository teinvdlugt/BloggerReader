package com.teinvdlugt.android.bloggerreader;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsService;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CustomTabsHelper {
    private CustomTabsClient client;
    private CustomTabsSession session;
    private CustomTabsServiceConnection connection;

    public void openURL(Activity activity, String url) {
        String packageName = getChromePackageName(activity);

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

        String packageName = getChromePackageName(activity);
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

    private static String chromePackageName;

    public static String getChromePackageName(Context context) {
        if (chromePackageName != null) return chromePackageName;

        PackageManager pm = context.getPackageManager();

        // Get default VIEW intent handler.
        Intent activityIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"));
        ResolveInfo defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0);
        String defaultViewHandlerPackageName = null;
        if (defaultViewHandlerInfo != null) {
            defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName;
        }

        // Get all apps that can handle VIEW intents.
        List<ResolveInfo> resolvedActivityList = pm.queryIntentActivities(activityIntent, 0);
        List<String> packagesSupportingCustomTabs = new ArrayList<>();
        for (ResolveInfo info : resolvedActivityList) {
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
            serviceIntent.setPackage(info.activityInfo.packageName);
            if (pm.resolveService(serviceIntent, 0) != null) {
                packagesSupportingCustomTabs.add(info.activityInfo.packageName);
            }
        }

        // Now packagesSupportingCustomTabs contains all apps that can handle both VIEW intents
        // and service calls.
        if (packagesSupportingCustomTabs.isEmpty()) {
            chromePackageName = null;
        } else if (packagesSupportingCustomTabs.size() == 1) {
            chromePackageName = packagesSupportingCustomTabs.get(0);
        } else if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
                && !hasSpecializedHandlerIntents(context, activityIntent)
                && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)) {
            chromePackageName = defaultViewHandlerPackageName;
        } else if (packagesSupportingCustomTabs.contains(Constants.STABLE)) {
            chromePackageName = Constants.STABLE;
        } else if (packagesSupportingCustomTabs.contains(Constants.BETA)) {
            chromePackageName = Constants.BETA;
        } else if (packagesSupportingCustomTabs.contains(Constants.DEV)) {
            chromePackageName = Constants.DEV;
        } else if (packagesSupportingCustomTabs.contains(Constants.LOCAL)) {
            chromePackageName = Constants.LOCAL;
        }
        return chromePackageName;
    }

    private static boolean hasSpecializedHandlerIntents(Context context, Intent intent) {
        try {
            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> handlers = pm.queryIntentActivities(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER);
            if (handlers == null || handlers.size() == 0) {
                return false;
            }
            for (ResolveInfo resolveInfo : handlers) {
                IntentFilter filter = resolveInfo.filter;
                if (filter == null) continue;
                if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue;
                if (resolveInfo.activityInfo == null) continue;
                return true;
            }
        } catch (RuntimeException e) {
            Log.e("TAG", "Runtime exception while getting specialized handlers");
        }
        return false;
    }
}
