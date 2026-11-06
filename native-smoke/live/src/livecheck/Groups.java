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
                case "text":
                    all.addAll(IllustratedChecks.text());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown group " + one);
            }
        }
        return all;
    }
}
