package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.util.Misc;
import illustratedEntities.helper.ImageHandler;
import illustratedEntities.helper.ImagePicker;
import illustratedEntities.helper.ImageTags;
import illustratedEntities.memory.ImageDataEntry;
import illustratedEntities.plugins.ModPlugin;

import java.util.ArrayList;
import java.util.List;

public final class StationChecks {
    private StationChecks() {
    }

    public static List<Step> stations() {
        List<Step> steps = new ArrayList<>();
        steps.add(StationChecks::jumpToStation);
        steps.add(StationChecks::stationCopiesPlanet);
        steps.add(StationChecks::changePlanetImage);
        steps.add(c -> {
            String expected = (String) c.get("stationPath");
            SectorEntityToken station = station(market(c));
            IllustratedChecks.closeDialog(c);
            c.hold(() -> expected.equals(IllustratedChecks.path(station)), 10f, "closing the market syncs the station image");
        });
        steps.add(StationChecks::pickerOnStation);
        return steps;
    }

    private static void jumpToStation(Ctx c) {
        SectorAPI sector = Global.getSector();
        MarketAPI target = null;
        for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
            SectorEntityToken station = station(market);
            if (station == null || !Misc.hasOrbitalStation(market) || !ImageHandler.hasImage(market.getPrimaryEntity())) continue;
            if (market.getContainingLocation() != sector.getCurrentLocation() && station.getCustomInteractionDialogImageVisual() == null) target = market;
        }
        c.expect(target != null, "market with a station and an image in another system found");
        if (target == null) return;

        c.put("stationMarket", target.getId());
        SectorEntityToken station = station(target);
        LocationAPI location = target.getContainingLocation();
        sector.doHyperspaceTransition(sector.getPlayerFleet(), sector.getPlayerFleet(), new JumpPointAPI.JumpDestination(target.getPrimaryEntity(), "live check"));
        c.until(() -> sector.getCurrentLocation() == location && ImageHandler.hasImage(station), 30f, "jump syncs the station image");
    }

    private static void stationCopiesPlanet(Ctx c) {
        MarketAPI market = market(c);
        SectorEntityToken station = station(market);
        c.expect(station.hasTag(ModPlugin.ORBITAL_HAS_INTERACTION_IMAGE), "station has the orbital tag");
        c.expect(IllustratedChecks.path(market.getPrimaryEntity()).equals(IllustratedChecks.path(station)), "station shows the planet image");
    }

    private static void changePlanetImage(Ctx c) {
        MarketAPI market = market(c);
        SectorEntityToken primary = market.getPrimaryEntity();
        ImageDataEntry other = null;
        for (ImageDataEntry entry : IllustratedChecks.memory().getDataMap().values()) {
            if (entry.planetId.isEmpty() && !entry.isUsed()) other = entry;
        }
        ImageHandler.removeImageFrom(primary);
        ImageHandler.setImage(primary, other, false);
        c.put("stationPath", other.getImagePath());
        c.expect(!other.getImagePath().equals(IllustratedChecks.path(station(market))), "station still shows the old image before the sync");
        IllustratedChecks.openDialog(c, primary);
    }

    private static void pickerOnStation(Ctx c) {
        ImagePicker picker = new ImagePicker(station(market(c)), false, ImageTags.MatchMode.BROAD, true);
        c.expect(!picker.isEmpty(), "picker finds an image for a station");
        if (picker.isEmpty()) return;
        ImageDataEntry entry = IllustratedChecks.memory().get(picker.pick());
        c.expect(entry.optionalTags.contains(ImageTags.STATION) || entry.requiredTags.contains(ImageTags.STATION), "station image has the station tag");
    }

    private static MarketAPI market(Ctx c) {
        return Global.getSector().getEconomy().getMarket((String) c.get("stationMarket"));
    }

    private static SectorEntityToken station(MarketAPI market) {
        for (SectorEntityToken t : market.getConnectedEntities()) {
            if (Entities.STATION_BUILT_FROM_INDUSTRY.equals(t.getCustomEntityType())) return t;
        }
        return null;
    }
}
