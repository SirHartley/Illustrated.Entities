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
import org.lazywizard.lazylib.MathUtils;

import java.util.*;

public class ImagePicker {

    private WeightedRandomPicker<Integer> picker;
    private Set<String> tags;
    private String faction;
    private Random r;

    public ImagePicker(SectorEntityToken t, boolean avoidDuplicates, ImageTags.MatchMode mode, boolean respectSeed) {
        this.r = respectSeed && t != null ? new Random(t.getId().hashCode()) : new Random();
        this.picker = new WeightedRandomPicker<>();
        this.tags = t != null ? getTags(t) : null;

        if (t != null) {
            if (t.getMarket() != null && t.getMarket().getFaction() != null) faction = t.getMarket().getFactionId();
            else if (t.getFaction() != null) faction = t.getFaction().getId();
            else faction = null;
        }

        ModPlugin.log.info("Generating Picker for " + (t != null ? t.getName() : null) + ", [" + (tags != null ? tags.toString() : null) + "]");

        Map<Integer, Integer> choices = generateChoices(tags, faction, avoidDuplicates, mode);
        if (choices.isEmpty() && mode == ImageTags.MatchMode.EXACT) choices = generateChoices(tags, faction, avoidDuplicates, ImageTags.MatchMode.BROAD);
        if (choices.isEmpty() && avoidDuplicates) choices = generateChoices(tags, faction, false, ImageTags.MatchMode.ANY);

        for (Map.Entry<Integer, Integer> entry : choices.entrySet()) picker.add(entry.getKey(), pickWeight(entry.getValue()));
    }

    public boolean isEmpty() {
        return picker.isEmpty();
    }

    public int pick() {
        return picker.pick(r);
    }

    public static Map<Integer, Integer> generateChoices(Set<String> tags, String faction, boolean avoidDuplicates, ImageTags.MatchMode mode) {
        Map<Integer, Integer> choices = new HashMap<>();
        Integer targetIndex = getTargetIndex(tags);
        int used = 0, rejected = 0;

        for (ImageDataEntry entry : ImageDataMemory.getInstance().getDataMap().values()) {
            if (avoidDuplicates && entry.isUsed()) {
                used++;
                continue;
            }

            Integer score = score(entry, tags, targetIndex, faction, mode);
            if (score == null) rejected++;
            else choices.put(entry.id, score);
        }

        if (Global.getSettings().isDevMode()) ModPlugin.log.info("Checked: " + ImageDataMemory.getInstance().getDataMap().size()
                + ", passed: " + choices.size() + ", used: " + used + ", rejected: " + rejected);

        return choices;
    }

    public static Integer score(ImageDataEntry entry, Set<String> tags, Integer targetIndex, String faction, ImageTags.MatchMode mode) {
        if (!entry.faction.isEmpty() && faction != null && !faction.equals(entry.faction)) return null;
        if (tags.contains(ImageTags.STATION) && !entry.optionalTags.contains(ImageTags.STATION) && !entry.requiredTags.contains(ImageTags.STATION)) return null;
        if (tags.contains(ImageTags.GAS) && !entry.requiredTags.contains(ImageTags.GAS)) return null;
        if (!tags.containsAll(entry.requiredTags) || !Collections.disjoint(tags, entry.requiredExcludedTags)) return null;

        int score = entry.weight + entry.requiredTags.size();

        if (targetIndex != null && mode != ImageTags.MatchMode.ANY && !entry.index.equals("A")) {
            int distance = Math.abs(Integer.parseInt(entry.index) - targetIndex);
            if (distance > (mode == ImageTags.MatchMode.BROAD ? 1 : 0)) return null;
            if (distance == 0) score++;
        }

        for (String s : entry.optionalTags) if (tags.contains(s)) score++;
        for (String s : entry.optionalExcludedTags) if (tags.contains(s)) score--;
        if (faction != null && faction.equals(entry.faction)) score += 2;

        return score;
    }

    // +1 keeps score 0 images pickable, the square favours better matches
    public static float pickWeight(int score) {
        return (float) Math.pow(1 + Math.max(0, score), 2);
    }

    public static Integer getTargetIndex(Set<String> tags) {
        for (String s : tags) {
            if (!s.startsWith(ImageTags.INDEX)) continue;
            String index = s.substring(ImageTags.INDEX.length());
            return index.equals("A") ? null : MathUtils.clamp(Integer.parseInt(index), 0, 4);
        }
        return null;
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
        }

        if (m != null) {
            if (!(t instanceof PlanetAPI) && !Entities.STATION_BUILT_FROM_INDUSTRY.equals(t.getCustomEntityType())) {
                applicableTags.add(ImageTags.STATION);
                applicableTags.add(ImageTags.INTERIOR);
            }

            if (!m.isPlanetConditionMarketOnly() && m.getHazardValue() > 1.75f) applicableTags.add(ImageTags.INTERIOR);

            int largestPlanet = 0;
            for (MarketAPI market : Misc.getFactionMarkets(m.getFactionId())) {
                if (market.getSize() > largestPlanet) largestPlanet = market.getSize();
            }

            if (m.isPlayerOwned()) applicableTags.add(ImageTags.DEVELOPED);

            if (!m.isFreePort()
                    && !m.isHidden()
                    && largestPlanet > 5
                    && !Factions.PIRATES.equals(m.getFactionId())
                    && !Factions.LUDDIC_PATH.equals(m.getFactionId())
                    && !Factions.INDEPENDENT.equals(m.getFactionId())) applicableTags.add(ImageTags.DEVELOPED);

            //we remove this during iteration if needed
            applicableTags.add(ImageTags.ATMOSPHERE);

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

                if (condition.getId().equals(Conditions.NO_ATMOSPHERE) || condition.getId().equals(Conditions.TOXIC_ATMOSPHERE)) applicableTags.remove(ImageTags.ATMOSPHERE);
            }

            if (Misc.hasRuins(m)) {
                applicableTags.add(ImageTags.RUINS);
            }

            if (m.getTags().contains(Tags.NO_MARKET_INFO)) {
                applicableTags.clear();
                applicableTags.add(ImageTags.DERELICT);
            }

            applicableTags.add(ImageTags.INDEX + getDevelopmentIndexForMarket(m));
        }

        if (applicableTags.isEmpty()) {
            applicableTags.add(ImageTags.DERELICT);
            applicableTags.add(ImageTags.INDEX + 0);
        }

        return applicableTags;
    }

    public List<Integer> getChoices() {
        return picker.getItems();
    }

    public static int getDevelopmentIndexForMarket(MarketAPI m) {
        int level = 0;

        if (m != null && !m.isPlanetConditionMarketOnly()) {
            level += m.getSize() - 2;
        }

        return level;
    }
}
