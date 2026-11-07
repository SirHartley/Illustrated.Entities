package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;

import java.util.Map;

public final class Ctx {
    private static final Logger log = Global.getLogger(Ctx.class);
    public static int passed = 0;
    public static int failed = 0;

    float waitDays = 0f;
    long waitStart = 0L;
    int waitFrames = 0;
    boolean reload = false;
    public boolean keepDialog = false;
    java.util.function.BooleanSupplier hold = null;
    String holdId = null;
    long holdEnd = 0L;
    java.util.function.BooleanSupplier until = null;
    Runnable onFrame = null;
    String untilId = null;
    float untilMax = 0f;
    long untilStart = 0L;

    public void expect(boolean condition, String id) {
        if (condition) {
            passed++;
            log.info("LIVE_CHECK ok: " + id);
        } else {
            failed++;
            log.info("LIVE_CHECK FAIL: " + id);
        }
    }

    public void near(float actual, float expected, float tolerance, String id) {
        expect(Math.abs(actual - expected) <= tolerance, id + " (got " + actual + ", want " + expected + ")");
    }

    private static int inputs = 0;

    // clicks.sh reads these lines and does them with gamectl
    public void input(String what) {
        log.info("LIVE_CHECK input " + (++inputs) + " " + what);
    }

    public void note(String text) {
        log.info("LIVE_CHECK note: " + text);
    }

    public void days(float days) {
        waitDays = days;
        waitStart = Global.getSector().getClock().getTimestamp();
    }

    public void shot(String name) {
        log.info("LIVE_CHECK shot " + name);
        waitFrames = 500;
    }

    public void frames(int frames) {
        waitFrames = frames;
    }

    public void until(java.util.function.BooleanSupplier condition, float maxDays, String id, Runnable frame) {
        until = condition;
        untilMax = maxDays;
        untilId = id;
        onFrame = frame;
        untilStart = Global.getSector().getClock().getTimestamp();
    }

    public void until(java.util.function.BooleanSupplier condition, float maxDays, String id) {
        until(condition, maxDays, id, null);
    }

    // real seconds, works while paused and with a dialog open
    public void hold(java.util.function.BooleanSupplier condition, float seconds, String id) {
        hold = condition;
        holdId = id;
        holdEnd = System.currentTimeMillis() + (long) (seconds * 1000f);
    }

    public void reload() {
        reload = true;
    }

    public Map<String, Object> data() {
        return Global.getSector().getPersistentData();
    }

    public void put(String key, Object value) {
        data().put("live_check_" + key, value);
    }

    public Object get(String key) {
        return data().get("live_check_" + key);
    }

    public MarketAPI market(String key) {
        return Global.getSector().getEconomy().getMarket((String) get(key));
    }
}
