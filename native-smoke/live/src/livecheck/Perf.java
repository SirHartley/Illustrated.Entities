package livecheck;

import com.fs.starfarer.api.Global;
import illustratedEntities.memory.ImageDataEntry;
import org.apache.log4j.Logger;
import patchlib.api.context.HookContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// timers from Timers and frame times from Runner. wall is real time per frame, work leaves out the wait inside
// Display.update and sync, so a frame cap or vsync does not hide a cost. run.sh copies the PERF lines to perf.csv
public final class Perf {
    private static final Logger log = Global.getLogger(Perf.class);

    static long displayNanos = 0;
    static long displayStart = 0;
    static int depth = 0;

    private static final Map<String, long[]> timers = new TreeMap<>();
    private static List<long[]> frames = null;
    private static long lastFrame = 0;

    // the MultilineTextField that advanced last, 2.0.c has none
    public static volatile Object field = null;

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

    static void line(String section, String text) {
        log.info("LIVE_CHECK note: PERF " + section + " | " + text);
    }

    private static String ms(long nanos) {
        return String.format("%.2f", nanos / 1000000.0);
    }
}
