package illustratedEntities.dialogue.panel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.memory.ImageDataEntry;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;

public class ImageTilePlugin implements CustomUIPanelPlugin {

    private static final int DOTS = 10;
    private static final float RADIUS = 12f;
    private static final float DOT_SIZE = 3f;
    private static final float TURN_TIME = 1f;

    private final ImageDataEntry entry;
    private final ImageListPanelPlugin loader;

    private PositionAPI position;
    private float spin = 0f;
    boolean seen = false;
    boolean queued = false;

    public ImageTilePlugin(ImageDataEntry entry, ImageListPanelPlugin loader) {
        this.entry = entry;
        this.loader = loader;
    }

    public boolean isLoaded() {
        return Boolean.TRUE.equals(entry.isLoaded);
    }

    public void load() {
        entry.load();
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void render(float alphaMult) {
        if (position == null || !loader.shows(position)) return;

        float x = position.getX();
        float y = position.getY();
        float w = position.getWidth();
        float h = position.getHeight();

        if (isLoaded()) {
            SpriteAPI sprite = Global.getSettings().getSprite(entry.getImagePath());
            sprite.setSize(w, h);
            sprite.setAlphaMult(alphaMult);
            sprite.render(x, y);
            return;
        }

        seen = true;
        loader.queue(this);

        quad(x, y, w, h, Color.BLACK, alphaMult);
        int head = (int) (spin * DOTS);
        for (int i = 0; i < DOTS; i++) {
            double angle = Math.PI / 2 - i * 2 * Math.PI / DOTS;
            float fade = 1f - ((head - i + DOTS) % DOTS) / (float) DOTS;
            float dx = (float) Math.cos(angle) * RADIUS;
            float dy = (float) Math.sin(angle) * RADIUS;
            quad(x + w / 2f + dx - DOT_SIZE / 2f, y + h / 2f + dy - DOT_SIZE / 2f, DOT_SIZE, DOT_SIZE, Misc.getBasePlayerColor(), alphaMult * fade);
        }
    }

    @Override
    public void advance(float amount) {
        spin = (spin + amount / TURN_TIME) % 1f;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }

    private static void quad(float x, float y, float w, float h, Color color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) Math.round(color.getAlpha() * Math.max(0f, Math.min(1f, alphaMult))));
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x + w, y);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}
