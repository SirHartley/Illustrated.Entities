package illustratedEntities.helper;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.econ.ResourceDepositsCondition;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.memory.ImageDataMemory;
import illustratedEntities.plugins.ModPlugin;

import java.util.*;

public class ImagePicker {

    private WeightedRandomPicker<Integer> picker;
    private Set<String> tags;
    private String faction;

    public ImagePicker(SectorEntityToken t, boolean avoidDuplicates, boolean respectSeed) {
        Random r = respectSeed ? new Random(t.getId().hashCode()) : new Random();
        this.picker = new WeightedRandomPicker<>();
        this.tags = t != null ? getTags(t) : null;

        if (Settings.RESPECT_FACTION && t != null) {
            if (t.getMarket() != null && t.getMarket().getFaction() != null)
                this.faction = t.getMarket().getFactionId();
            else if (t.getFaction() != null) t.getFaction().getId();
            else faction = null;
        }

        ModPlugin.log.info("Generating Picker for " + (t != null ? t.getName() : null) + ", [" + (tags != null ? tags.toString() : null) + "]");

        Map<Integer, Integer> baseChanceMap =generateChoices(tags, faction, avoidDuplicates);

        for (Map.Entry<Integer, Integer> entry : baseChanceMap.entrySet()) {
            //we use chance = total num * weight ^ 2 so a result with more tag matches is more likely to get picked against the mass of low matches
            picker.add(entry.getKey(), (float) (baseChanceMap.size() * Math.pow(entry.getValue(), 2)));
        }

        //failsafe - if all images are in use, we allow duplicates even if it shouldn't be allowed.
        if (avoidDuplicates && picker.isEmpty()) {
            generateChoices(tags, faction, false);
        }
    }

    public boolean isEmpty() {
        return picker.isEmpty();
    }

    public int pick() {
        return picker.pick();
    }

    public static Map<Integer, Integer> generateChoices(Set<String> tags, String faction, boolean avoidDuplicates) {
        Map<Integer, Integer> baseChanceMap = new HashMap<>();

        int used = 0, faction_mismatch = 0, station = 0, gas = 0, tag_mismatch = 0, passed = 0;

        for (ImageDataEntry entry : ImageDataMemory.getInstance().getDataMap().values()) {
            if (avoidDuplicates && entry.isUsed()) {
                used++;
                continue;
            }
            if (!entry.faction.isEmpty() && faction != null && !faction.equals(entry.faction)) {
                faction_mismatch++;
                continue;
            }

            //special cases (stations require station tag, gas giants require gas giant tag...)
            if (tags.contains(ImageTags.STATION) && !entry.optionalTags.contains(ImageTags.STATION)) {
                station++;
                continue; //stations only get station images
            }

            if (tags.contains(ImageTags.GAS) && !entry.requiredTags.contains(ImageTags.GAS)) {
                gas++;
                continue;
            }

            int score = entry.weight;

            //normal cases - the image is valid if it has all required and no forbidden tags
            if (tags.containsAll(entry.requiredTags) && Collections.disjoint(tags, entry.requiredExcludedTags)) score += entry.requiredTags.size() + entry.requiredExcludedTags.size();
            else {
               tag_mismatch++;
                continue;
            }

            for (String s : entry.optionalTags) if (tags.contains(s)) score++;
            for (String s : entry.optionalExcludedTags) if (!tags.contains(s)) score++;

            passed++;

            baseChanceMap.put(entry.id, score);
        }

        if (Global.getSettings().isDevMode()) ModPlugin.log.info(
                "Passed: " + passed + "\n" +
                        "Tag Mismatch: " + tag_mismatch + "\n" +
                        "Used: " + used + "\n" +
                        "Faction Mismatch: " + faction_mismatch + "\n" +
                        "Needs Station: " + station + "\n" +
                        "Needs Gas: " + gas + "\n");

        return baseChanceMap;
    }

    public static Set<String> getTags(SectorEntityToken t) {
        Set<String> applicableTags = new HashSet<>();
        MarketAPI m = t.getMarket();

        //base planet properties
        if (t instanceof PlanetAPI) {
            String type = ((PlanetAPI) t).getTypeId();

            for (Map.Entry<String, String[]> e : ImageTags.typeTagMap.entrySet()) {
                if (type.contains(e.getKey())) applicableTags.addAll(Arrays.asList(e.getValue()));
            }

            if (m != null
                    && !m.isPlanetConditionMarketOnly()
                    && m.getSize() > 4)
                applicableTags.add(ImageTags.CITY);
        }

        if (m != null) {
            if(!(t instanceof PlanetAPI) && !t.getCustomEntityType().equals(Entities.STATION_BUILT_FROM_INDUSTRY)) {
                applicableTags.add(ImageTags.STATION);
                applicableTags.add(ImageTags.INTERIOR);
            }

            if (!m.isPlanetConditionMarketOnly() && m.getHazardValue() > 175f) applicableTags.add(ImageTags.INTERIOR);

            int largestPlanet = 0;
            for (MarketAPI market : Misc.getFactionMarkets(m.getFactionId())) {
                if (market.getSize() > largestPlanet) largestPlanet = market.getSize();
            }

            if (m.isPlayerOwned()) applicableTags.add(ImageTags.DEVELOPED);

            if (!m.isFreePort()
                    && m.getSize() > 3
                    && !m.isHidden()
                    && largestPlanet > 5
                    && !Factions.PIRATES.equals(m.getFactionId())
                    && !Factions.LUDDIC_PATH.equals(m.getFactionId())
                    && !Factions.INDEPENDENT.equals(m.getFactionId())) applicableTags.add(ImageTags.DEVELOPED);

            //condition-dependent
            for (MarketConditionAPI condition : m.getConditions()) {
                String id = condition.getSpec().getId();

                //specific
                for (Map.Entry<String, String[]> e : ImageTags.specificConditionTagMap.entrySet()) {
                    if (id.equals(e.getKey())) applicableTags.addAll(Arrays.asList(e.getValue()));
                }

                //unspecific
                for (Map.Entry<String, String[]> e : ImageTags.generalConditionTagMap.entrySet()) {
                    if (id.contains(e.getKey())) applicableTags.addAll(Arrays.asList(e.getValue()));
                }

                //if ore mod > 0 means ORE_ABUNDANT+
                if (condition.getId().contains("ore")) {
                    int mod = ResourceDepositsCondition.MODIFIER.get(condition.getId()) != null ? ResourceDepositsCondition.MODIFIER.get(condition.getId()) : 0;
                    if (mod > 0) applicableTags.add(ImageTags.MINE);
                }
            }

            //if planet has no or toxic atmo, it does not have an atmo, otherwise, add atmo tag
            if (Collections.disjoint(m.getConditions(), Arrays.asList(Conditions.NO_ATMOSPHERE, Conditions.TOXIC_ATMOSPHERE))){
                applicableTags.add(ImageTags.ATMOSPHERE);
            }

            if (Misc.hasRuins(m)){
                applicableTags.add(ImageTags.RUINS);
            }

            if (m.getTags().contains(Tags.NO_MARKET_INFO)) {
                applicableTags.clear();
                applicableTags.add(ImageTags.DERELICT);
            }
        }

        if (applicableTags.isEmpty()) applicableTags.add(ImageTags.DERELICT);

        return applicableTags;
    }

    public List<Integer> getChoices(){
        return picker.getItems();
    }
}
