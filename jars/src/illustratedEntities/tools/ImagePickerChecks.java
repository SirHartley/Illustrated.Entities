package illustratedEntities.tools;

import illustratedEntities.helper.ImagePicker;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.memory.ImageDataEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static illustratedEntities.helper.ImageTags.MatchMode.*;

public final class ImagePickerChecks {

    private static int checks;

    public static void main(String[] args) throws Exception {
        Set<String> planet = Set.of("bar", "hot", "int", "index2");
        Integer index = ImagePicker.getTargetIndex(planet);
        check(index == 2, "target index");
        check(ImagePicker.getTargetIndex(Set.of("index-1")) == 0 && ImagePicker.getTargetIndex(Set.of("index7")) == 4, "index clamp");

        ImageDataEntry generic = entry(0, List.of(), List.of(), List.of(), List.of(), "", "A");
        ImageDataEntry excluding = entry(0, List.of(), List.of("dvl", "wtr", "jun", "cld"), List.of(), List.of("pol", "atm"), "", "A");
        ImageDataEntry matching = entry(0, List.of("bar", "hot"), List.of(), List.of("int"), List.of(), "", "A");
        check(score(generic, planet) == 0, "generic scores zero");
        check(score(excluding, planet) == 0, "exclusions add no score");
        check(score(matching, planet) == 3, "matched tags score");
        check(score(entry(0, List.of(), List.of(), List.of(), List.of("int"), "", "A"), planet) == -1, "present optional exclusion costs");
        check(score(entry(0, List.of(), List.of("int"), List.of(), List.of(), "", "A"), planet) == null, "required exclusion rejects");
        check(score(entry(0, List.of("wtr"), List.of(), List.of(), List.of(), "", "A"), planet) == null, "missing required rejects");

        check(ImagePicker.pickWeight(-3) == ImagePicker.pickWeight(0) && ImagePicker.pickWeight(0) > 0, "negative scores stay pickable and never above zero");
        check(ImagePicker.pickWeight(3) > ImagePicker.pickWeight(2), "better match weighs more");

        ImageDataEntry exact = entry(0, List.of(), List.of(), List.of(), List.of(), "", "2");
        ImageDataEntry near = entry(0, List.of(), List.of(), List.of(), List.of(), "", "3");
        ImageDataEntry far = entry(0, List.of(), List.of(), List.of(), List.of(), "", "4");
        check(score(exact, planet, EXACT) == 1 && score(near, planet, EXACT) == null, "exact population");
        check(score(near, planet, BROAD) == 0 && score(far, planet, BROAD) == null, "broad population");
        check(score(exact, planet, BROAD) > score(near, planet, BROAD), "broad prefers exact size");
        check(score(far, planet, ANY) == 0, "any ignores population");

        Set<String> station = Set.of("stn", "int", "index2");
        check(score(generic, station) == null, "stations need stn");
        check(score(entry(0, List.of(), List.of(), List.of("stn"), List.of(), "", "A"), station) != null, "stn optional");
        check(score(entry(0, List.of("stn"), List.of(), List.of(), List.of(), "", "A"), station) != null, "stn required");
        check(score(matching, Set.of("gas", "bar", "hot", "index2")) == null, "gas giants need gas");

        ImageDataEntry pirate = entry(0, List.of(), List.of(), List.of(), List.of(), "pirates", "A");
        check(ImagePicker.score(pirate, planet, index, "hegemony", BROAD) == null, "faction limit");
        check(ImagePicker.score(pirate, planet, index, "pirates", BROAD) == 2, "faction bonus");
        check(ImagePicker.score(pirate, planet, index, null, BROAD) == 0, "no faction shows all");
        check(score(entry(4, List.of(), List.of(), List.of(), List.of(), "", "A"), planet) == 4, "weight adds");

        csv();
        System.out.println("Image picker checks passed: " + checks);
    }

    private static void csv() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("data/config/image_data.csv"));
        check(lines.get(0).equals("id,tags_required,tags_optional,population,faction,weight,planet_id,note"), "image_data header");
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split(",", -1);
            check(cells.length == 8, "eight columns: " + cells[0]);
            check(Files.isRegularFile(Path.of("graphics/illustrated_entities/images/" + cells[0] + ".png")), "image file " + cells[0]);
            if (!cells[3].isEmpty() && !cells[3].equals("A")) Integer.parseInt(cells[3]);
            if (!cells[5].isEmpty()) Integer.parseInt(cells[5]);
        }
    }

    private static Integer score(ImageDataEntry entry, Set<String> tags) {
        return score(entry, tags, BROAD);
    }

    private static Integer score(ImageDataEntry entry, Set<String> tags, ImageTags.MatchMode mode) {
        return ImagePicker.score(entry, tags, ImagePicker.getTargetIndex(tags), null, mode);
    }

    private static ImageDataEntry entry(int weight, List<String> required, List<String> requiredExcluded,
                                        List<String> optional, List<String> optionalExcluded, String faction, String index) {
        return new ImageDataEntry(1, weight, required, requiredExcluded, optional, optionalExcluded, faction, "", index);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private ImagePickerChecks() {
    }
}
