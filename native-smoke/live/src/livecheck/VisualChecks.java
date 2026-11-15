package livecheck;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.dialogue.panel.VisualCustomPanel;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

// the market image against the one vanilla draws for the same visual, and what is left on screen after each return
public final class VisualChecks {
    private static final float GLOW = 30f;

    private VisualChecks() {
    }

    public static List<Step> visual() {
        List<Step> steps = new ArrayList<>();
        steps.add(VisualChecks::open);
        steps.add(c -> grab(c, "mod"));
        steps.add(c -> {
            Grab.save("mod");
            visuals(c, "dock", 1);
            c.shot("visual-mod");
        });

        steps.add(c -> Ui.press(c, "change_interaction_visual"));
        steps.add(c -> {
            Grab.request("selector", region(c)[0], region(c)[1], region(c)[2], region(c)[3]);
            c.frames(2);
        });
        steps.add(c -> {
            Grab.save("selector");
            click(c, "return");
            c.frames(6);
        });
        steps.add(c -> grab(c, "return-fade"));
        steps.add(c -> {
            Grab.save("return-fade");
            visuals(c, "selector return fading", -1);
            c.frames(60);
        });
        steps.add(c -> grab(c, "return"));
        steps.add(c -> {
            Grab.save("return");
            visuals(c, "selector return", 1);
            same(c, "mod", "return", "selector return draws the dock image");
        });

        steps.add(c -> Ui.press(c, "desc"));
        steps.add(c -> {
            Ui.press(c, "return");
            c.frames(60);
        });
        steps.add(c -> grab(c, "editor-return"));
        steps.add(c -> {
            Grab.save("editor-return");
            visuals(c, "editor return", 1);
            same(c, "mod", "editor-return", "editor return draws the dock image");
        });

        steps.add(c -> {
            dialog(c).getPlugin().optionSelected("Visit the bar", "marketVisitBar");
            c.frames(60);
        });
        steps.add(c -> {
            visuals(c, "bar", -1);
            dialog(c).getPlugin().optionSelected("Leave the bar", "barLeave");
            c.frames(6);
        });
        steps.add(c -> grab(c, "bar-fade"));
        steps.add(c -> {
            Grab.save("bar-fade");
            visuals(c, "bar return fading", -1);
            c.frames(60);
        });
        steps.add(c -> grab(c, "bar-return"));
        steps.add(c -> {
            Grab.save("bar-return");
            // vanilla's leaveBar shows its own visual, so only log what is left
            visuals(c, "bar return", 1);
        });

        steps.add(c -> {
            SectorEntityToken t = dialog(c).getInteractionTarget();
            dialog(c).getVisualPanel().showImageVisual(t.getCustomInteractionDialogImageVisual());
            c.frames(60);
        });
        steps.add(c -> grab(c, "vanilla"));
        steps.add(c -> {
            Grab.save("vanilla");
            visuals(c, "vanilla", 1);
            same(c, "mod", "vanilla", "market image matches the vanilla image");
            c.shot("visual-vanilla");
        });

        steps.add(c -> {
            dialog(c).getVisualPanel().showCore(CoreUITabId.CARGO, dialog(c).getInteractionTarget(), CoreUITradeMode.OPEN, null);
            c.frames(60);
        });
        steps.add(c -> {
            visuals(c, "market core ui", 0);
            dialog(c).getVisualPanel().closeCoreUI();
            c.frames(30);
        });
        steps.add(Ui::closeDialog);

        steps.add(VisualChecks::surveyCore);
        steps.add(c -> {
            visuals(c, "survey core ui", 0);
            found(c);
            c.frames(60);
        });
        steps.add(c -> grab(c, "founded"));
        steps.add(c -> {
            Grab.save("founded");
            visuals(c, "core ui after founding", 0);
            c.shot("visual-founded");
            dialog(c).getVisualPanel().closeCoreUI();
            c.frames(60);
        });
        steps.add(c -> {
            visuals(c, "dialog after founding", -1);
            c.shot("visual-founded-dialog");
            dialog(c).getVisualPanel().showCore(CoreUITabId.CARGO, dialog(c).getInteractionTarget(), CoreUITradeMode.OPEN, null);
            c.frames(60);
        });
        steps.add(c -> grab(c, "founded-core"));
        steps.add(c -> {
            Grab.save("founded-core");
            visuals(c, "core ui again after founding", 0);
            c.shot("visual-founded-core");
            dialog(c).getVisualPanel().closeCoreUI();
            c.frames(30);
        });
        steps.add(Ui::closeDialog);
        return steps;
    }

    // what PlanetSurveyPanel does: the survey core ui is open while the colony is made and the dialog re-inits
    private static void surveyCore(Ctx c) {
        PlanetAPI planet = Colony.planets(1).get(0);
        c.put("foundColony", planet.getId());
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        Ui.openDialog(c, planet);
        dialog(c).getVisualPanel().showCore(CoreUITabId.CARGO, planet, CoreUITradeMode.OPEN, null);
        c.frames(60);
    }

    private static void found(Ctx c) {
        PlanetAPI planet = (PlanetAPI) Ui.entity(c, "foundColony");
        Colony.make(planet, 3);
        ListenerUtil.reportPlayerColonizedPlanet(planet);
        new RuleBasedInteractionDialogPluginImpl("OutpostEstablished").init(dialog(c));
    }

    // a texture the unloader dropped, like a planet docked with for the first time since the load
    private static void open(Ctx c) {
        PlanetAPI planet = Ui.newColony(c, "visualColony");
        Ui.image(planet).unload();
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        Ui.openDialog(c, planet);
        PositionAPI p = VisualCustomPanel.getPanel().getPosition();
        // vanilla adds the image visual and the custom panel with the same rightOfTop, so they share the top left
        float top = p.getY() + p.getHeight();
        c.put("region", new float[]{p.getX() - GLOW, top - 300f - GLOW, 480f + 2 * GLOW, 300f + 2 * GLOW});
        c.note("custom panel x=" + p.getX() + " y=" + p.getY() + " w=" + p.getWidth() + " h=" + p.getHeight());
    }

    private static float[] region(Ctx c) {
        return (float[]) c.get("region");
    }

    private static void grab(Ctx c, String name) {
        float[] r = region(c);
        Grab.request(name, r[0], r[1], r[2], r[3]);
        c.frames(2);
    }

    // the bottom GLOW rows hold the mod's button row, vanilla has none
    private static void same(Ctx c, String a, String b, String id) {
        float[] d = Grab.diff(a, b, GLOW);
        c.expect(d != null && d[1] < 0.01f, id + " (" + (d == null ? "no grab" : "mean diff " + d[0] + ", changed " + d[1]) + ")");
    }

    private static void click(Ctx c, String id) {
        InteractionDialogCustomPanelPlugin.ButtonEntry entry = Ui.button(id);
        c.expect(entry != null, "button " + id + " shown");
        if (entry != null) entry.button.setChecked(!entry.button.isChecked());
    }

    private static InteractionDialogAPI dialog(Ctx c) {
        return Global.getSector().getCampaignUI().getCurrentInteractionDialog();
    }

    // every child of the dialog right of the text, the visual panel area. expected < 0 only logs
    @SuppressWarnings("unchecked")
    private static void visuals(Ctx c, String when, int expected) throws Throwable {
        InteractionDialogAPI dialog = dialog(c);
        List<Object> children = (List<Object>) MethodHandles.publicLookup()
                .findVirtual(dialog.getClass(), "getChildrenCopy", MethodType.methodType(List.class))
                .invoke(dialog);
        float left = region(c)[0] + GLOW;
        int shown = 0;
        StringBuilder text = new StringBuilder();
        for (Object child : children) {
            if (!(child instanceof UIComponentAPI)) continue;
            UIComponentAPI ui = (UIComponentAPI) child;
            PositionAPI p = ui.getPosition();
            if (p.getX() < left - 1f || p.getWidth() <= 0f) continue;
            String kind = child instanceof CustomPanelAPI ? "custom panel " + plugin((CustomPanelAPI) child) : child.getClass().getSimpleName();
            Fader fader = (Fader) MethodHandles.publicLookup()
                    .findVirtual(child.getClass(), "getFader", MethodType.methodType(Fader.class))
                    .invoke(child);
            float alpha = ui.getOpacity() * fader.getBrightness();
            text.append(" | ").append(kind).append(" x=").append(p.getX()).append(" y=").append(p.getY())
                    .append(" w=").append(p.getWidth()).append(" h=").append(p.getHeight()).append(" alpha=").append(alpha);
            if (alpha > 0f) shown++;
        }
        c.note("visuals " + when + text);
        if (expected >= 0) c.expect(shown == expected, when + ": " + expected + " visual shown (" + shown + ")");
    }

    private static String plugin(CustomPanelAPI panel) {
        CustomUIPanelPlugin plugin = panel.getPlugin();
        return plugin == null ? "none" : plugin.getClass().getSimpleName();
    }
}
