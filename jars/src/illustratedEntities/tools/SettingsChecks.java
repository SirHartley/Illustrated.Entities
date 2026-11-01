package illustratedEntities.tools;

import illustratedEntities.helper.Setting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SettingsChecks {

    private static int checks;

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of("data/config/LunaSettings.csv"));
        check(lines.get(0).startsWith("fieldID,fieldName,fieldType,defaultValue,secondaryValue,fieldDescription,minValue,maxValue"),
                "luna header");
        Map<String, String[]> rows = new HashMap<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.replace(",", "").isBlank()) continue;
            String[] cells = line.split(",", -1);
            check(cells.length == 9, "nine luna columns: " + cells[0]);
            check(rows.put(cells[0], cells) == null, "unique id " + cells[0]);
        }
        for (Setting setting : Setting.values()) {
            String[] row = rows.get(setting.id);
            check(row != null, "registered " + setting);
            check(!row[5].isBlank(), "help text " + setting);
            if (setting.fallback instanceof Integer fallback) {
                check(row[2].equals("Int"), "int type " + setting);
                check(Integer.parseInt(row[3]) == fallback, "default " + setting);
                check(Integer.parseInt(row[6]) == setting.min, "minimum " + setting);
                check(Integer.parseInt(row[7]) == setting.max, "maximum " + setting);
                check(setting.sanitize(Double.NaN).equals(fallback), "reject nan " + setting);
                check(setting.sanitize(-1e6).equals(setting.min), "clamp minimum " + setting);
                check(setting.sanitize(1e6).equals(setting.max), "clamp maximum " + setting);
                check(setting.sanitize(3.0).equals(3), "double becomes int " + setting);
            } else {
                check(row[2].equals("Boolean"), "boolean type " + setting);
                check(Boolean.parseBoolean(row[3].toLowerCase()) == (Boolean) setting.fallback, "default " + setting);
                check(setting.sanitize(!(Boolean) setting.fallback).equals(!(Boolean) setting.fallback), "keep value " + setting);
            }
            check(setting.sanitize(null).equals(setting.fallback), "missing value " + setting);
            check(setting.sanitize("x").equals(setting.fallback), "invalid value " + setting);
            check(Setting.byId(setting.id) == setting, "lookup " + setting);
        }
        for (Map.Entry<String, String[]> row : rows.entrySet()) {
            if (row.getValue()[2].equals("Header")) continue;
            check(Setting.byId(row.getKey()) != null, "csv row has a Setting: " + row.getKey());
        }
        System.out.println("Settings checks passed: " + checks);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private SettingsChecks() {
    }
}
