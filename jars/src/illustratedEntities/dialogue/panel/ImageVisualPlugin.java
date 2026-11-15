package illustratedEntities.dialogue.panel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

// draws an InteractionDialogImageVisual the way VisualPanelAPI.showImageVisual does, glow border included
public class ImageVisualPlugin extends NoFrameCustomPanelPlugin {

    private static final float GLOW_WIDTH = 25f;
    private static final float GLOW_ALPHA = 0.35f;
    private static final float GLOW_TEX_END = 0.99f;
    private static final int CORNER_SEGMENTS = 3;

    private final SpriteAPI sprite;
    private final SpriteAPI glow;
    private final Color glowColor;
    private final float texX;
    private final float texY;
    private final float texW;
    private final float texH;

    private PositionAPI pos;

    public ImageVisualPlugin(InteractionDialogImageVisual visual) {
        sprite = Global.getSettings().getSprite(visual.getSpriteName());
        glow = Global.getSettings().getSprite("planets", "atmosphere");
        glowColor = Global.getSettings().getColor("buttonText");

        float width = sprite.getWidth();
        float height = sprite.getHeight();
        float subW = visual.getSubImageWidth();
        float subH = visual.getSubImageHeight();
        float x = visual.getSubImageX();
        float y = visual.getSubImageY();

        if (visual.isShowRandomSubImage()) {
            if (width == 480f && height == 300f) {
                subW = 480f;
                subH = 300f;
            }
            x = Math.max(0f, (float) Math.random() * (width - subW));
            y = Math.max(0f, (float) Math.random() * (height - subH));
        }

        texX = x / width;
        texY = y / height;
        texW = subW / width;
        texH = subH / height;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        pos = position;
    }

    @Override
    public void render(float alphaMult) {
        if (pos == null) return;
        float w = pos.getWidth();
        float h = pos.getHeight();

        GL11.glPushMatrix();
        GL11.glTranslatef(pos.getX(), pos.getY(), 0f);
        renderGlow(w, h, alphaMult);

        sprite.setAlphaMult(alphaMult);
        GL11.glScalef(w / (texW * sprite.getWidth()), h / (texH * sprite.getHeight()), 1f);
        GL11.glTranslatef(-texX * sprite.getWidth(), -texY * sprite.getHeight(), 0f);
        sprite.renderRegion(0f, 0f, texX, texY, texW, texH);
        GL11.glPopMatrix();
    }

    private void renderGlow(float w, float h, float alphaMult) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        glow.bindTexture();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glColor4ub((byte) glowColor.getRed(), (byte) glowColor.getGreen(), (byte) glowColor.getBlue(), (byte) (glowColor.getAlpha() * GLOW_ALPHA * alphaMult));

        renderCorner(0f, 0f, 180f);
        renderCorner(0f, h, 90f);
        renderCorner(w, h, 0f);
        renderCorner(w, 0f, 270f);

        float g = GLOW_WIDTH;
        GL11.glBegin(GL11.GL_QUADS);
        edge(0f, 0f, -g, 0f, -g, h, 0f, h);
        edge(w, 0f, w + g, 0f, w + g, h, w, h);
        edge(0f, h, 0f, h + g, w, h + g, w, h);
        edge(0f, 0f, 0f, -g, w, -g, w, 0f);
        GL11.glEnd();
    }

    private void edge(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x1, y1);
        GL11.glTexCoord2f(0f, GLOW_TEX_END);
        GL11.glVertex2f(x2, y2);
        GL11.glTexCoord2f(0f, GLOW_TEX_END);
        GL11.glVertex2f(x3, y3);
        GL11.glTexCoord2f(0f, 0f);
        GL11.glVertex2f(x4, y4);
    }

    private void renderCorner(float x, float y, float startAngle) {
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double angle = Math.toRadians(startAngle + 90f * i / CORNER_SEGMENTS);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            GL11.glTexCoord2f(0f, 0f);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(0f, GLOW_TEX_END);
            GL11.glVertex2f(x + cos * GLOW_WIDTH, y + sin * GLOW_WIDTH);
        }
        GL11.glEnd();
    }
}
