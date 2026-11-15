package livecheck;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

// reads the finished frame from the back buffer just before Display.update swaps it, captures fail with the screen off
public final class Grab {
    private static final Logger log = Global.getLogger(Grab.class);

    private static int[] ask = null;
    private static String askName = null;
    static final Map<String, int[]> regions = new HashMap<>();
    static final Map<String, byte[]> pixels = new HashMap<>();

    private Grab() {
    }

    // ui units, bottom left origin like PositionAPI
    static void request(String name, float x, float y, float w, float h) {
        float scale = Global.getSettings().getScreenScaleMult();
        ask = new int[]{(int) (x * scale), (int) (y * scale), (int) (w * scale), (int) (h * scale)};
        askName = name;
    }

    static boolean done(String name) {
        return pixels.containsKey(name);
    }

    private static void frame() {
        if (ask == null) return;
        int[] r = ask;
        ByteBuffer buffer = BufferUtils.createByteBuffer(r[2] * r[3] * 3);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(r[0], r[1], r[2], r[3], GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
        byte[] out = new byte[buffer.capacity()];
        buffer.get(out);
        regions.put(askName, r);
        pixels.put(askName, out);
        ask = null;
    }

    // base64 rgb rows bottom up, the only file a script may write
    static void save(String name) {
        int[] r = regions.get(name);
        byte[] p = pixels.get(name);
        if (r == null || p == null) return;
        try {
            Global.getSettings().writeTextFileToCommon("live_check_grab_" + name, r[2] + " " + r[3] + "\n" + Base64.getEncoder().encodeToString(p));
        } catch (Exception e) {
            log.error("LIVE_CHECK grab " + name + " not saved", e);
        }
    }

    // [mean channel difference, share of pixels that differ by more than 24 in any channel], bottom rows left out
    static float[] diff(String a, String b, float skipBottom) {
        byte[] pa = pixels.get(a), pb = pixels.get(b);
        int[] r = regions.get(a);
        if (pa == null || pb == null || pa.length != pb.length) return null;
        int start = (int) (skipBottom * Global.getSettings().getScreenScaleMult()) * r[2] * 3;
        long sum = 0;
        int changed = 0;
        for (int i = start; i < pa.length; i += 3) {
            int max = 0;
            for (int k = 0; k < 3; k++) {
                int d = Math.abs((pa[i + k] & 0xff) - (pb[i + k] & 0xff));
                sum += d;
                max = Math.max(max, d);
            }
            if (max > 24) changed++;
        }
        return new float[]{sum / (float) (pa.length - start), changed * 3f / (pa.length - start)};
    }

    @Patch(target = @ClassMatch(typeName = "org.lwjgl.opengl.Display"))
    public static final class Hook {
        @Before(target = @MethodMatch(methodName = "update"))
        public static void before(BeforeContext context) { frame(); }
    }
}
