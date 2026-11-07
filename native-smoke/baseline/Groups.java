package livecheck;

import java.util.ArrayList;
import java.util.List;

// run.sh swaps this in when it tests a release zip. these groups compile against the 2.0.c jar, the others need 2.0.d
public final class Groups {
    private Groups() {
    }

    public static List<Step> get(String group) {
        List<Step> all = new ArrayList<>();
        for (String one : group.split(",")) {
            switch (one) {
                case "perf":
                    all.addAll(PerfChecks.perf());
                    break;
                case "seed":
                    all.addAll(SeedChecks.seed());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown group " + one);
            }
        }
        return all;
    }
}
