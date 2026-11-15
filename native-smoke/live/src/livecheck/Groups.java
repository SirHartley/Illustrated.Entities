package livecheck;

import java.util.ArrayList;
import java.util.List;

public final class Groups {
    private Groups() {
    }

    // comma separated groups run one after the other in one game, far faster than one boot per group
    public static List<Step> get(String group) {
        List<Step> all = new ArrayList<>();
        for (String one : group.split(",")) {
            switch (one) {
                case "images":
                    all.addAll(IllustratedChecks.images());
                    break;
                case "colony":
                    all.addAll(IllustratedChecks.colony());
                    break;
                case "panels":
                    all.addAll(PanelChecks.panels());
                    break;
                case "stations":
                    all.addAll(StationChecks.stations());
                    break;
                case "settings":
                    all.addAll(SettingChecks.settings());
                    break;
                case "text":
                    all.addAll(IllustratedChecks.text());
                    break;
                case "editor":
                    all.addAll(EditorChecks.editor());
                    break;
                case "clicks":
                    all.addAll(ClickChecks.clicks());
                    break;
                case "visual":
                    all.addAll(VisualChecks.visual());
                    break;
                case "perf":
                    all.addAll(PerfChecks.perf());
                    break;
                case "seed":
                    all.addAll(SeedChecks.seed());
                    break;
                case "upgrade":
                    all.addAll(UpgradeChecks.upgrade());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown group " + one);
            }
        }
        return all;
    }
}
