package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Pair;
import illustratedEntities.dialogue.panel.InteractionDialogCustomPanelPlugin;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.memory.ImageDataEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// same steps on 2.0.c and 2.0.d, so it only uses what both builds have. compare two runs with compare.txt and perf.csv
public final class PerfChecks {
    private static final String MODE = "$illent_matchMode";
    private static final String DUPES = "$illent_dupes";
    private static final String TAGS = "$illent_tagList";
    private static final int FRAMES = 300;
    private static final int RELOADS = 3;
    private static final int JUMPS = 5;
    private static final int OPENS = 5;

    private PerfChecks() {
    }

    public static List<Step> perf() {
        List<Step> steps = new ArrayList<>();
        steps.add(c -> {
            Perf.clear();
            Perf.sample(c, FRAMES);
        });
        steps.add(c -> Perf.report("campaign"));

        for (int i = 0; i < RELOADS; i++) steps.add(c -> c.reload());
        steps.add(c -> Perf.dump("game load"));

        for (int i = 0; i < JUMPS; i++) {
            int n = i;
            steps.add(c -> jump(c, n));
            // a jump started during the last one's fade is dropped
            steps.add(c -> c.days(1f));
        }
        steps.add(c -> Perf.dump("jump"));

        steps.add(PerfChecks::colony);
        for (int i = 0; i < OPENS; i++) {
            steps.add(c -> Ui.openDialog(c, Ui.entity(c, "perfColony")));
            steps.add(Ui::closeDialog);
        }
        steps.add(c -> Perf.dump("market open"));

        steps.add(c -> Ui.openDialog(c, Ui.entity(c, "perfColony")));
        steps.add(c -> {
            Perf.textures("selector default", "before");
            Ui.press(c, "change_interaction_visual");
        });
        steps.add(c -> selectorOpen(c, "selector default"));
        steps.add(c -> selectorDone("selector default"));

        steps.add(c -> {
            anyMatch();
            Ui.press(c, "showFilter");
        });
        steps.add(c -> selectorOpen(c, "selector any"));
        steps.add(c -> selectorDone("selector any"));

        steps.add(c -> {
            anyMatch();
            allTags();
            Ui.press(c, "showFilter");
        });
        steps.add(c -> selectorOpen(c, "selector all tags"));
        steps.add(c -> selectorDone("selector all tags"));

        steps.add(c -> {
            Ui.closeDialog(c);
            c.frames(30);
        });
        steps.add(c -> {
            Perf.textures("economy tick", "before");
            c.days(4f);
        });
        steps.add(c -> {
            Perf.textures("economy tick", "after");
            Perf.dump("economy tick");
        });

        steps.add(c -> Ui.openDialog(c, Ui.entity(c, "perfColony")));
        steps.add(c -> {
            anyMatch();
            Ui.press(c, "change_interaction_visual");
        });
        steps.add(c -> {
            Ui.closeDialog(c);
            c.frames(30);
        });
        steps.add(c -> {
            Perf.textures("reload", "before");
            c.reload();
        });
        steps.add(c -> {
            Perf.textures("reload", "after");
            c.days(4f);
        });
        steps.add(c -> Perf.textures("reload", "after tick"));

        steps.add(c -> Ui.openDialog(c, Ui.entity(c, "perfColony")));
        steps.add(c -> Ui.press(c, "desc"));
        steps.add(c -> Perf.sample(c, FRAMES));
        steps.add(c -> {
            Perf.report("editor empty");
            Perf.dump("editor empty");
            Ui.closeDialog(c);
            c.frames(30);
        });

        steps.add(c -> c.days(10f));
        steps.add(c -> Perf.dump("ten days"));
        return steps;
    }

    private static void jump(Ctx c, int n) {
        SectorAPI sector = Global.getSector();
        List<SectorEntityToken> targets = new ArrayList<>();
        Set<LocationAPI> seen = new HashSet<>();
        for (ImageDataEntry entry : Ui.presets()) {
            SectorEntityToken t = sector.getEntityById(entry.planetId);
            LocationAPI location = t.getContainingLocation();
            if (ImageHandler.hasImage(t) && location != sector.getCurrentLocation() && seen.add(location)) targets.add(t);
        }
        SectorEntityToken target = targets.get(n % targets.size());
        LocationAPI location = target.getContainingLocation();
        sector.doHyperspaceTransition(sector.getPlayerFleet(), sector.getPlayerFleet(), new JumpPointAPI.JumpDestination(target, "live check"));
        c.until(() -> sector.getCurrentLocation() == location, 30f, "perf jump " + n + " reaches " + target.getId());
    }

    private static void colony(Ctx c) {
        PlanetAPI planet = Ui.newColony(c, "perfColony");
        Colony.jump(planet.getContainingLocation(), planet.getLocation().x, planet.getLocation().y + 300f);
        c.frames(30);
    }

    private static void selectorOpen(Ctx c, String section) {
        int listed = 0;
        for (InteractionDialogCustomPanelPlugin.ButtonEntry entry : Ui.buttons()) {
            if (entry.id.endsWith("_set")) listed++;
        }
        Perf.line(section, "listed images=" + listed);
        Perf.textures(section, "open");
        Perf.sample(c, FRAMES);
    }

    private static void selectorDone(String section) {
        Perf.report(section);
        Perf.dump(section);
    }

    private static void anyMatch() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.set(MODE, ImageTags.MatchMode.ANY, 0f);
        memory.set(DUPES, true, 0f);
    }

    // what a player gets by clicking every tag button in the filter. stn and gas stay off, either one hides all other images
    private static void allTags() {
        Set<String> tags = new HashSet<>();
        for (Pair<String, String> tag : ImageTags.tagNameList) tags.add(tag.one);
        tags.remove("stn");
        tags.remove("gas");
        Global.getSector().getMemoryWithoutUpdate().set(TAGS, tags, 0f);
    }
}
