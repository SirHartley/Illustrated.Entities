package illustratedEntities.loading;

import com.fs.starfarer.api.Global;
import illustratedEntities.helper.Settings;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.plugins.ModPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Importer {

    public static Map<Integer, ImageDataEntry> loadImageData(){
        Map<Integer, ImageDataEntry> dataMap = new HashMap<>();

        try {
            JSONArray config = Global.getSettings().getMergedSpreadsheetDataForMod("id", Settings.CSV_PATH, ModPlugin.MOD_ID);
            for (int i = 0; i < config.length(); i++) {
                JSONObject row = config.getJSONObject(i);
                int id = row.getInt("id");
                String planetId = row.getString("planet_id").replaceAll("\\s", "");
                String faction = row.getString("faction").replaceAll("\\s", "");
                int weight = row.getString("weight").isEmpty() ? 0 : row.getInt("weight");

                List<String> requiredTags = new ArrayList<>();
                List<String> requiredExcludedTags = new ArrayList<>();
                List<String> optionalTags = new ArrayList<>();
                List<String> optionalExcludedTags = new ArrayList<>();

                for (String s : row.getString("tags_required").split("\\s+")){
                    s = s.replaceAll("\\s", "");
                    if (s.length() < 3) continue;

                    if (s.startsWith("!")) requiredExcludedTags.add(s.substring(1));
                    else requiredTags.add(s);
                }

                for (String s : row.getString("tags_optional").split("\\s+")){
                    s = s.replaceAll("\\s", "");
                    if (s.length() < 3) continue;

                    if (s.startsWith("!")) optionalExcludedTags.add(s.substring(1));
                    else optionalTags.add(s);
                }

                ImageDataEntry imageEntry = new ImageDataEntry(id, weight, requiredTags, requiredExcludedTags, optionalTags, optionalExcludedTags, faction, planetId);
                dataMap.put(id, imageEntry);
            }
        } catch (IOException | JSONException ex) {
            ModPlugin.log.error("Could not find Illustrated.Entites image_data.csv, or something is wrong with the data format.", ex);
        }

       return dataMap;
    }

    public static void unloadImage(String path){
        ModPlugin.log.info("unloading image " + path);
        Global.getSettings().unloadTexture(path);
    }

    public static void loadImage(String path) {

        try {
            ModPlugin.log.info("loading image " + path);
            Global.getSettings().loadTexture(path);
        } catch (IOException e){
            ModPlugin.log.error("Could not find image " + path, e);
        }
    }
}
