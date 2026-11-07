package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.ui.PositionAPI;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.MultilineTextField;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.TextHandler;
import illustratedEntities.memory.TextDataEntry;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

// real mouse and keyboard. each step logs what it wants, clicks.sh does it with gamectl, c.hold waits for the result.
// gamectl has no wheel, drag or modifier keys, so the scroll wheel and dragging the thumb stay untested
public final class ClickChecks {
    private static final float WAIT = 20f;

    private ClickChecks() {
    }

    public static List<Step> clicks() {
        List<Step> steps = new ArrayList<>();
        steps.add(ClickChecks::open);
        steps.add(c -> button(c, "change_interaction_visual", () -> Ui.hasButton("showFilter"), "click opens the selector"));
        steps.add(ClickChecks::applyImage);
        steps.add(c -> button(c, "desc", () -> Ui.hasButton("confirm"), "click opens the editor"));
        // Perf.field still holds the last editor until the new one advances
        steps.add(c -> c.frames(30));
        steps.add(ClickChecks::clickBox);
        steps.add(ClickChecks::typeText);
        steps.add(ClickChecks::enterAndBackspace);
        steps.add(c -> button(c, "confirm", () -> saved(c, "live click\nsecond lin"), "click on apply saves the typed text"));
        steps.add(ClickChecks::scrollTrack);
        steps.add(c -> button(c, "delete", () -> !Ui.hasButton("confirm"), "click opens the delete prompt"));
        steps.add(c -> button(c, "delete", () -> TextHandler.getDataForEntity(Ui.entity(c, "clickColony")) == null, "click on delete removes the description"));
        steps.add(c -> c.frames(30));
        steps.add(c -> button(c, "return", () -> Ui.hasButton("desc"), "click on return leaves the editor"));
        steps.add(Ui::closeDialog);
        return steps;
    }

    private static void open(Ctx c) {
        PlanetAPI planet = Ui.newColony(c, "clickColony");
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        Ui.openDialog(c, planet);
    }

    private static void applyImage(Ctx c) {
        SectorEntityToken t = Ui.entity(c, "clickColony");
        int current = ImageHandler.getImageId(t);
        String pick = null;
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : Ui.buttons()) {
            if (pick == null && entry.id.endsWith("_set") && !entry.id.equals(current + "_set")) pick = entry.id;
        }
        c.expect(pick != null, "selector lists other images");
        if (pick == null) return;
        int picked = Integer.parseInt(pick.substring(0, pick.indexOf('_')));
        button(c, pick, () -> ImageHandler.getImageId(t) == picked && t.getMemoryWithoutUpdate().getBoolean(ModPlugin.PLAYER_PICK), "click on apply sets the image");
    }

    private static void clickBox(Ctx c) throws Throwable {
        MultilineTextField f = EditorChecks.field(0);
        PositionAPI p = EditorChecks.position(f);
        click(c, p.getCenterX(), p.getCenterY());
        c.hold(() -> focused(f), WAIT, "click focuses the text box");
    }

    private static void typeText(Ctx c) throws Throwable {
        MultilineTextField f = EditorChecks.field(0);
        c.input("type live click");
        c.hold(() -> "live click".equals(f.getText()), WAIT, "real keys type into the box");
    }

    private static void enterAndBackspace(Ctx c) throws Throwable {
        MultilineTextField f = EditorChecks.field(0);
        c.input("key ENTER");
        c.input("type second line");
        c.input("key BACK");
        c.hold(() -> "live click\nsecond lin".equals(f.getText()), WAIT, "real enter and backspace work");
    }

    private static void scrollTrack(Ctx c) throws Throwable {
        MultilineTextField f = EditorChecks.field(0);
        EditorChecks.focus(f);
        EditorChecks.insert(f, EditorChecks.longText().substring(0, 4000));
        lookupToTop(f);
        PositionAPI p = EditorChecks.position(f);
        click(c, p.getX() + p.getWidth() - 7f, p.getY() + 30f);
        c.hold(() -> scroll(f) > 0f, WAIT, "click on the scroll track moves the text");
    }

    private static void lookupToTop(MultilineTextField f) {
        f.processInput(new ArrayList<>(List.of(new KeyEvent(org.lwjgl.input.Keyboard.KEY_HOME, (char) 0, true, false))));
    }

    private static void button(Ctx c, String id, BooleanSupplier done, String check) {
        InteractionDialogCustomPanelPlugin.ButtonEntry entry = Ui.button(id);
        c.expect(entry != null, "button " + id + " shown");
        if (entry == null) return;
        PositionAPI p = entry.button.getPosition();
        click(c, p.getCenterX(), p.getCenterY());
        c.hold(done, WAIT, check);
    }

    private static void click(Ctx c, float x, float y) {
        float scale = Global.getSettings().getScreenScaleMult();
        c.input("click " + Math.round(x * scale) + " " + Math.round(Global.getSettings().getScreenHeightPixels() - y * scale));
    }

    private static boolean saved(Ctx c, String text) {
        TextDataEntry data = TextHandler.getDataForEntity(Ui.entity(c, "clickColony"));
        return data != null && text.equals(data.getText(1));
    }

    private static boolean focused(MultilineTextField f) {
        try {
            return EditorChecks.focused(f);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static float scroll(MultilineTextField f) {
        try {
            return EditorChecks.scroll(f);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }
}
