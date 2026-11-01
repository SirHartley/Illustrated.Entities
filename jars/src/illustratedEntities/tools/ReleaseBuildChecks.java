package illustratedEntities.tools;

public final class ReleaseBuildChecks {

    private static int checks;

    private ReleaseBuildChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkNotes();
        checkRepoChangelog();
        checkStamping();
        checkFiles();
        System.out.println("Release build: " + checks + " checks passed");
    }

    private static void checkNotes() {
        String changelog = "header\n\n1.1.a - UNRELEASED\nAdded - New thing\n\n1.0.a\nFixed - Old thing\n";
        ReleaseBuild.Notes preview = ReleaseBuild.notes(changelog, null);
        require(preview.version().equals("1.1.a") && preview.unreleased(), "preview reads newest section");
        require(!preview.text().contains("Old thing"), "notes stop at the next section");
        fails(() -> ReleaseBuild.notes(changelog, "1.1.a"), "tag rejects UNRELEASED");
        fails(() -> ReleaseBuild.notes(changelog, "1.0.a"), "tag must match newest section");
        fails(() -> ReleaseBuild.notes(changelog, "v1.1.a"), "tag has no v prefix");
        String released = changelog.replace(" - UNRELEASED", "");
        require(ReleaseBuild.notes(released, "1.1.a").version().equals("1.1.a"), "tag accepts released section");
        fails(() -> ReleaseBuild.notes("1.0.a\nNothing here\n", null), "section needs a change line");
        fails(() -> ReleaseBuild.notes("1.0.a\nAdded - x\n1.0.a\nAdded - y\n", null), "duplicate versions");
    }

    private static void checkStamping() {
        String file = "{\"modVersion\":{\"major\":1,\"minor\":0,\"patch\":\"a\"},\"directDownloadURL\":\"x\"}";
        String stamped = ReleaseBuild.stampVersion(file, "2.3.c");
        require(stamped.contains("\"major\":2") && stamped.contains("\"minor\":3")
                && stamped.contains("\"patch\":\"c\""), "version file stamped");
        require(stamped.contains("/download/2.3.c/" + ReleaseBuild.FOLDER + "2.3.c.zip"), "download url stamped");
        require(ReleaseBuild.stampInfo("{\"version\": \"0.1\"}", "2.3.c").contains("\"2.3.c\""), "mod_info stamped");
        fails(() -> ReleaseBuild.stampInfo("{}", "2.3.c"), "missing field rejected");
    }

    private static void checkRepoChangelog() throws Exception {
        String changelog = java.nio.file.Files.readString(java.nio.file.Path.of("Changelog"));
        require(ReleaseBuild.notes(changelog, null).version().matches(ReleaseBuild.VERSION), "repo changelog parses");
    }

    private static void checkFiles() {
        require(ReleaseBuild.releaseFile("Changelog"), "changelog shipped");
        require(ReleaseBuild.releaseFile("NOTICE"), "notice shipped");
        require(ReleaseBuild.releaseFile("data/campaign/rules.csv"), "data shipped");
        require(ReleaseBuild.releaseFile("graphics/" + ReleaseBuild.ASSET_DIR + "/images/1.png"), "graphics shipped");
        require(ReleaseBuild.releaseFile("IllustratedEntities_icon.png"), "luna icon shipped");
        require(!ReleaseBuild.releaseFile("graphics/" + ReleaseBuild.ASSET_DIR + "/images/note.txt"), "notes skipped");
        require(!ReleaseBuild.releaseFile("docs/ARCHITECTURE.md"), "docs skipped");
        require(!ReleaseBuild.releaseFile("CLAUDE.md"), "agent files skipped");
        require(!ReleaseBuild.releaseFile("jars/src/" + ReleaseBuild.MOD + "/ModPlugin.java"), "source skipped");
        require(!ReleaseBuild.releaseFile("graphics/unused/old.png"), "unused art skipped");
        require(!ReleaseBuild.releaseFile("graphics/" + ReleaseBuild.ASSET_DIR + "/art.psd"), "source art skipped");
        require(!ReleaseBuild.releaseFile("data/.idea/x.json"), "hidden dirs skipped");
        require(!ReleaseBuild.runtimeClass(ReleaseBuild.MOD + "/tools/ReleaseBuild.class"), "tools left out of jar");
        require(ReleaseBuild.runtimeClass(ReleaseBuild.PLUGIN), "runtime classes kept");
    }

    private static void fails(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            checks++;
            return;
        }
        throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
