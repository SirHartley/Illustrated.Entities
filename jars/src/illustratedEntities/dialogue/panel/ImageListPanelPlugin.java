package illustratedEntities.dialogue.panel;

import com.fs.starfarer.api.ui.PositionAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ImageListPanelPlugin extends FramedCustomPanelPlugin {

    // about 12 ms per image, so a few per frame keep the list from stalling the game
    private static final int LOADS_PER_FRAME = 2;

    private final List<ImageTilePlugin> queue = new ArrayList<>();

    public ImageListPanelPlugin(float sideRatio, Color color, boolean square) {
        super(sideRatio, color, square);
    }

    // the scroller renders every row and only clips them, so the tiles ask whether they are inside the list
    boolean shows(PositionAPI tile) {
        return pos != null && tile.getY() < pos.getY() + pos.getHeight() && tile.getY() + tile.getHeight() > pos.getY();
    }

    void queue(ImageTilePlugin tile) {
        if (tile.queued) return;
        tile.queued = true;
        queue.add(tile);
    }

    // tiles queue in render order, top first. seen is set again each frame a tile is still on screen
    @Override
    public void advance(float amount) {
        int loads = 0;
        Iterator<ImageTilePlugin> iterator = queue.iterator();
        while (iterator.hasNext()) {
            ImageTilePlugin tile = iterator.next();
            if (!tile.isLoaded() && tile.seen && loads < LOADS_PER_FRAME) {
                tile.load();
                loads++;
            }
            tile.seen = false;
            if (tile.isLoaded()) {
                tile.queued = false;
                iterator.remove();
            }
        }
    }
}
