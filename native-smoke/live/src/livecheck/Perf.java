package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.PositionAPI;
import illustratedEntities.memory.ImageDataEntry;
import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import patchlib.api.context.HookContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// timers from Timers and frame times from Runner. wall is real time per frame, work leaves out the wait inside
// Display.update and sync, so a frame cap or vsync does not hide a cost. run.sh copies the PERF lines to perf.csv
public final class Perf {
    private static final Logger log = Global.getLogger(Perf.class);
    private static final int TEXT_STRIP = 4;
    private static final int TEXT_GREEN = 120;

    static long displayNanos = 0;
    static long displayStart = 0;
    static int depth = 0;

    private static final Map<String, long[]> timers = new TreeMap<>();
    private static List<long[]> frames = null;
    private static long lastFrame = 0;

    // the MultilineTextField that advanced last, 2.0.c has none
    public static volatile Object field = null;

    // captures fail with the screen off, so read what a text box drew. [inside the box, the rows around it]
    static boolean readText = false;
    static final Map<Object, int[]> textPixels = new HashMap<>();

    private Perf() {
    }

    static void start(HookContext context) {
        context.setLocal("livecheck_t0", System.nanoTime());
    }

    static void stop(HookContext context, String key) {
        Long start = context.getLocal("livecheck_t0");
        if (start == null) return;
        long nanos = System.nanoTime() - start;
        long[] v = timers.computeIfAbsent(key, k -> new long[3]);
        v[0]++;
        v[1] += nanos;
        v[2] = Math.max(v[2], nanos);
    }

    static void frame() {
        long now = System.nanoTime(), wall = now - lastFrame, wait = displayNanos;
        lastFrame = now;
        displayNanos = 0;
        if (frames != null) frames.add(new long[]{wall, wall - wait});
    }

    static void sample(Ctx c, int count) {
        frames = new ArrayList<>();
        c.frames(count);
    }

    static void report(String section) {
        List<long[]> got = frames;
        frames = null;
        if (got == null || got.isEmpty()) return;
        line(section, "frames n=" + got.size() + stats("wall", got, 0) + stats("work", got, 1));
    }

    private static String stats(String name, List<long[]> got, int index) {
        List<long[]> sorted = new ArrayList<>(got);
        sorted.sort((a, b) -> Long.compare(a[index], b[index]));
        return " " + name + "_median=" + ms(sorted.get(sorted.size() / 2)[index])
                + " " + name + "_p95=" + ms(sorted.get(sorted.size() * 95 / 100)[index])
                + " " + name + "_max=" + ms(sorted.get(sorted.size() - 1)[index]);
    }

    static void dump(String section) {
        for (Map.Entry<String, long[]> e : timers.entrySet()) {
            long[] v = e.getValue();
            line(section, e.getKey() + " calls=" + v[0] + " total=" + ms(v[1]) + " mean=" + ms(v[1] / v[0]) + " max=" + ms(v[2]));
        }
        timers.clear();
    }

    static void clear() {
        timers.clear();
    }

    // loaded is what the texture cache holds, flagged is what the mod thinks it holds and can unload
    static void textures(String section, String when) {
        int loaded = 0, flagged = 0;
        for (ImageDataEntry entry : Ui.memory().getDataMap().values()) {
            if (Global.getSettings().getSprite(entry.getImagePath()).getTexWidth() > 0) loaded++;
            if (Boolean.TRUE.equals(entry.isLoaded)) flagged++;
        }
        line(section, "textures when=" + when + " loaded=" + loaded + " flagged=" + flagged);
    }

    static void readText(Object field, PositionAPI position) {
        float scale = Global.getSettings().getScreenScaleMult();
        int x = (int) (position.getX() * scale), y = (int) (position.getY() * scale);
        int w = (int) (position.getWidth() * scale), h = (int) (position.getHeight() * scale);
        int inside = bright(x + 2, y + 2, w - 8, h - 4);
        int around = bright(x, y - TEXT_STRIP - 1, w, TEXT_STRIP) + bright(x, y + h + 1, w, TEXT_STRIP);
        textPixels.put(field, new int[]{inside, around});
    }

    private static int bright(int x, int y, int w, int h) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 3);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(x, y, w, h, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, pixels);
        int count = 0;
        for (int i = 1; i < pixels.capacity(); i += 3) {
            if ((pixels.get(i) & 0xff) > TEXT_GREEN) count++;
        }
        return count;
    }

    static void line(String section, String text) {
        log.info("LIVE_CHECK note: PERF " + section + " | " + text);
    }

    private static String ms(long nanos) {
        return String.format("%.2f", nanos / 1000000.0);
    }
}
