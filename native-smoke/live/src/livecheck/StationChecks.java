package livecheck;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
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
import java.util.Map;

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
            Ui.closeDialog(c);
            c.hold(() -> expected.equals(Ui.path(station)), 10f, "closing the market syncs the station image");
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
        c.expect(Ui.path(market.getPrimaryEntity()).equals(Ui.path(station)), "station shows the planet image");
    }

    private static void changePlanetImage(Ctx c) {
        MarketAPI market = market(c);
        SectorEntityToken primary = market.getPrimaryEntity();
        ImageDataEntry other = null;
        for (ImageDataEntry entry : Ui.memory().getDataMap().values()) {
            if (entry.planetId.isEmpty() && !entry.isUsed()) other = entry;
        }
        ImageHandler.removeImageFrom(primary);
        ImageHandler.setImage(primary, other, false);
        c.put("stationPath", other.getImagePath());
        c.expect(!other.getImagePath().equals(Ui.path(station(market))), "station still shows the old image before the sync");
        Ui.openDialog(c, primary);
    }

    // a built orbital station copies its planet, only a market on a station entity gets the stn tag
    private static void pickerOnStation(Ctx c) {
        int stations = 0, empty = 0, wrong = 0, required = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            SectorEntityToken t = market.getPrimaryEntity();
            if (t instanceof PlanetAPI) continue;
            stations++;
            Map<Integer, Integer> choices = ImagePicker.generateChoices(ImagePicker.getTags(t), market.getFactionId(), false, ImageTags.MatchMode.BROAD);
            if (choices.isEmpty()) empty++;
            for (int id : choices.keySet()) {
                ImageDataEntry entry = Ui.memory().get(id);
                if (entry.requiredTags.contains(ImageTags.STATION)) required++;
                else if (!entry.optionalTags.contains(ImageTags.STATION)) wrong++;
            }
        }
        c.expect(stations > 0, "markets on stations found (" + stations + ")");
        c.expect(empty == 0, "picker finds an image for every station market (" + empty + " empty)");
        c.expect(wrong == 0, "station images all carry the station tag (" + wrong + " wrong)");
        c.expect(required > 0, "images with a required station tag show up on stations (" + required + ")");
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
