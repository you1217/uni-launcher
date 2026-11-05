package xyz.i4o.uni.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import org.json.JSONObject;

/**
 * Native GlobalDockService stub during Phase 1 native migration.
 */
public final class GlobalDockService extends Service {
    public static void ensureStarted(Context c) {}
    public static JSONObject snapshot(Context c) { return new JSONObject(); }
    public static boolean handleCommand(Context c, JSONObject r) { return false; }
    public static void resumeAfterCameraLaunchFailure() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

