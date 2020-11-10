package com.izooto.shortcutbadger;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

import com.izooto.shortcutbadger.impl.AdwHomeBadger;
import com.izooto.shortcutbadger.impl.ApexHomeBadger;
import com.izooto.shortcutbadger.impl.AsusHomeBadger;
import com.izooto.shortcutbadger.impl.DefaultBadger;
import com.izooto.shortcutbadger.impl.EverythingMeHomeBadger;
import com.izooto.shortcutbadger.impl.HuaweiHomeBadger;
import com.izooto.shortcutbadger.impl.NewHtcHomeBadger;
import com.izooto.shortcutbadger.impl.NovaHomeBadger;
import com.izooto.shortcutbadger.impl.OPPOHomeBader;
import com.izooto.shortcutbadger.impl.SamsungHomeBadger;
import com.izooto.shortcutbadger.impl.SonyHomeBadger;
import com.izooto.shortcutbadger.impl.VivoHomeBadger;
import com.izooto.shortcutbadger.impl.YandexLauncherBadger;
import com.izooto.shortcutbadger.impl.ZTEHomeBadger;
import com.izooto.shortcutbadger.impl.ZukHomeBadger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

public class ShortcutBadger {
    private static final String LOG_TAG = "ShortcutBadger";
    private static final int SUPPORTED_CHECK_ATTEMPTS = 3;

    private static final List<Class<? extends Badger>> BADGERS = new LinkedList<Class<? extends Badger>>();

    private volatile static Boolean sIsBadgeCounterSupported;
    private final static Object sCounterSupportedLock = new Object();

    static {
        BADGERS.add(AdwHomeBadger.class);
        BADGERS.add(ApexHomeBadger.class);
        BADGERS.add(DefaultBadger.class);
        BADGERS.add(NewHtcHomeBadger.class);
        BADGERS.add(NovaHomeBadger.class);
        BADGERS.add(SonyHomeBadger.class);
        BADGERS.add(AsusHomeBadger.class);
        BADGERS.add(HuaweiHomeBadger.class);
        BADGERS.add(OPPOHomeBader.class);
        BADGERS.add(SamsungHomeBadger.class);
        BADGERS.add(ZukHomeBadger.class);
        BADGERS.add(VivoHomeBadger.class);
        BADGERS.add(ZTEHomeBadger.class);
        BADGERS.add(EverythingMeHomeBadger.class);
        BADGERS.add(YandexLauncherBadger.class);
    }

    private static Badger sShortcutBadger;
    private static ComponentName sComponentName;

    /**
     * Tries to update the notification count
     *
     * @param context    Caller context
     * @param badgeCount Desired badge count
     * @return true in case of success, false otherwise
     */
    public static boolean applyCount(Context context, int badgeCount) {
        try {
            applyCountOrThrow(context, badgeCount);
            return true;
        } catch (Exception e) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, "Unable to execute badge", e);
            }
            return false;
        }
    }

    /**
     * Tries to update the notification count, throw a {@link ShortcutBadgerException} if it fails
     *
     * @param context    Caller context
     * @param badgeCount Desired badge count
     */
    public static void applyCountOrThrow(Context context, int badgeCount)  {
        if (sShortcutBadger == null) {
            boolean launcherReady = initBadger(context);

            if (!launcherReady)
                Log.e("ShortcutBadger"," default launcher available");
               // throw new ShortcutBadgerException("No default launcher available");
        }

        try {
            sShortcutBadger.executeBadge(context, sComponentName, badgeCount);
        } catch (Exception e) {
            Log.e("Handle Exception","ShortcutBadger");
           // throw new ShortcutBadgerException("Unable to execute badge", e);
        }
    }

    /**
     * Tries to remove the notification count
     *
     * @param context Caller context
     * @return true in case of success, false otherwise
     */
    public static boolean removeCount(Context context) {
        return applyCount(context, 0);
    }

    /**
     * Tries to remove the notification count, throw a {@link ShortcutBadgerException} if it fails
     *
     * @param context Caller context
     */
    public static void removeCountOrThrow(Context context) throws ShortcutBadgerException {
        applyCountOrThrow(context, 0);
    }

    /**
     * Whether this platform launcher supports shortcut badges. Doing this check causes the side
     * effect of resetting the counter if it's supported, so this method should be followed by
     * a call that actually sets the counter to the desired value, if the counter is supported.
     */
    public static boolean isBadgeCounterSupported(Context context) {
        // Checking outside synchronized block to avoid synchronization in the common case (flag
        // already set), and improve perf.
        if (sIsBadgeCounterSupported == null) {
            synchronized (sCounterSupportedLock) {
                // Checking again inside synch block to avoid setting the flag twice.
                if (sIsBadgeCounterSupported == null) {
                    String lastErrorMessage = null;
                    for (int i = 0; i < SUPPORTED_CHECK_ATTEMPTS; i++) {
                        try {
                            Log.i(LOG_TAG, "Checking if platform supports badge counters, attempt "
                                    + String.format("%d/%d.", i + 1, SUPPORTED_CHECK_ATTEMPTS));
                            if (initBadger(context)) {
                                sShortcutBadger.executeBadge(context, sComponentName, 0);
                                sIsBadgeCounterSupported = true;
                                Log.i(LOG_TAG, "Badge counter is supported in this platform.");
                                break;
                            } else {
                                lastErrorMessage = "Failed to initialize the badge counter.";
                            }
                        } catch (Exception e) {
                            // Keep retrying as long as we can. No need to dump the stack trace here
                            // because this error will be the norm, not exception, for unsupported
                            // platforms. So we just save the last error message to display later.
                            lastErrorMessage = e.getMessage();
                        }
                    }

                    if (sIsBadgeCounterSupported == null) {
                        Log.w(LOG_TAG, "Badge counter seems not supported for this platform: "
                                + lastErrorMessage);
                        sIsBadgeCounterSupported = false;
                    }
                }
            }
        }
        return sIsBadgeCounterSupported;
    }

    /**
     * @param context      Caller context
     * @param notification
     * @param badgeCount
     */
    public static void applyNotification(Context context, Notification notification, int badgeCount) {
        if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
            try {
                Field field = notification.getClass().getDeclaredField("extraNotification");
                Object extraNotification = field.get(notification);
                Method method = extraNotification.getClass().getDeclaredMethod("setMessageCount", int.class);
                method.invoke(extraNotification, badgeCount);
            } catch (Exception e) {
                if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                    Log.d(LOG_TAG, "Unable to execute badge", e);
                }
            }
        }
    }

    // Initialize Badger if a launcher is availalble (eg. set as default on the device)
    // Returns true if a launcher is available, in this case, the Badger will be set and sShortcutBadger will be non null.
    private static boolean initBadger(Context context) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent == null) {
            Log.e(LOG_TAG, "Unable to find launch intent for package " + context.getPackageName());
            return false;
        }

        sComponentName = launchIntent.getComponent();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null || resolveInfo.activityInfo.name.toLowerCase().contains("resolver"))
            return false;

        String currentHomePackage = resolveInfo.activityInfo.packageName;

        for (Class<? extends Badger> badger : BADGERS) {
            Badger shortcutBadger = null;
            try {
                shortcutBadger = badger.newInstance();
            } catch (Exception ignored) {
            }
            if (shortcutBadger != null && shortcutBadger.getSupportLaunchers().contains(currentHomePackage)) {
                sShortcutBadger = shortcutBadger;
                break;
            }
        }

        if (sShortcutBadger == null) {
            if (Build.MANUFACTURER.equalsIgnoreCase("ZUK"))
                sShortcutBadger = new ZukHomeBadger();
            else if (Build.MANUFACTURER.equalsIgnoreCase("OPPO"))
                sShortcutBadger = new OPPOHomeBader();
            else if (Build.MANUFACTURER.equalsIgnoreCase("VIVO"))
                sShortcutBadger = new VivoHomeBadger();
            else
                sShortcutBadger = new DefaultBadger();
        }

        return true;
    }

    // Avoid anybody to instantiate this class
    private ShortcutBadger() {

    }
}
