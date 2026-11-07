package illustratedEntities.dialogue.panel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.opengl.GL11;

// draws an InteractionDialogImageVisual the way VisualPanelAPI.showImageVisual does
public class ImageVisualPlugin extends NoFrameCustomPanelPlugin {

    private final SpriteAPI sprite;
    private final float texX;
    private final float texY;
    private final float texW;
    private final float texH;

    private PositionAPI pos;

    public ImageVisualPlugin(InteractionDialogImageVisual visual) {
        sprite = Global.getSettings().getSprite(visual.getSpriteName());

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
        sprite.setAlphaMult(alphaMult);
        GL11.glScalef(w / (texW * sprite.getWidth()), h / (texH * sprite.getHeight()), 1f);
        GL11.glTranslatef(-texX * sprite.getWidth(), -texY * sprite.getHeight(), 0f);
        sprite.renderRegion(0f, 0f, texX, texY, texW, texH);
        GL11.glPopMatrix();
    }
}
