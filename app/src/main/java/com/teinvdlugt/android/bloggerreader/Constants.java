package com.teinvdlugt.android.bloggerreader;


public class Constants {
    // Settings
    public static final String USE_CUSTOM_TABS_PREFERENCE = "use_custom_tabs";
    public static final String USE_CUSTOM_TABS_ASKED_PREFERENCE = "use_custom_tabs_asked";
    public static final String NOTIFICATION_PREFERENCE_KEY = "notifications";

    // Other preferences
    public static final String BLOG_VISIBLE_PREFERENCE = "blog_visible_";
    public static final String LAST_POST_ID_PREF = "most_recent_post_";

    // API Key
    public static final String API_KEY = "AIzaSyAsG_pjWPPXYWq68igzilu77ss0qRP5yM8"; // TODO Move to protected file!

    // File names
    public static final String BLOGS_FOLLOWING_FILE_NAME = "blogs_following";

    // Chrome package names
    static final String STABLE = "com.android.chrome";
    static final String BETA = "com.chrome.beta";
    static final String DEV = "com.chrome.dev";
    static final String LOCAL = "com.google.android.apps.chrome";

    // Notification ID
    static final int NOTIFICATION_ID = 0;

    // Intent actions
    public static final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";
}
