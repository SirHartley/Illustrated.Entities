package illustratedEntities.helper;

import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.openal.AL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageTags {

    public static final String
            DERELICT = "der",
            RUINS = "rns",
            BARREN = "bar",
            DESERT = "dst",
            SNOW = "snw",
            TUNDRA = "tun",
            ARID = "ard",
            WATER = "wtr",
            JUNGLE = "jun",
            TERRAN = "ter",
            LAVA = "lav",
            ALIEN = "aln",
            GAS = "gas", //Always primary
            INTERIOR = "int",
            HABITABLE = "hab",
            MINE = "mne",
            STATION = "stn", //Always optional
            IRRADIATED = "irr",
            TOXIC = "tox",
            HOT = "hot",
            COLD = "cld",
            POLLUTED = "pol",
            DEVELOPED = "dvl",
            ATMOSPHERE = "atm",
            RED = "red",
            SALT = "slt",
            INDEX = "index";

    public static final List<Pair<String, String>> tagNameList = new ArrayList<Pair<String, String>>(){{
        add( new Pair<>(DERELICT, "derelict"));
        add( new Pair<>(BARREN, "barren"));
        add( new Pair<>(DESERT, "desert"));
        add( new Pair<>(SNOW, "snow"));
        add( new Pair<>(TUNDRA, "tundra"));
        add( new Pair<>(ARID, "arid"));
        add( new Pair<>(WATER, "water"));
        add( new Pair<>(JUNGLE, "jungle"));
        add( new Pair<>(TERRAN, "terran"));
        add( new Pair<>(LAVA, "lava"));
        add( new Pair<>(ALIEN, "strange"));
        add( new Pair<>(GAS, "gas"));
        add( new Pair<>(INTERIOR, "hostile"));
        add( new Pair<>(HABITABLE, "habitable"));
        add( new Pair<>(MINE, "mining"));
        add( new Pair<>(STATION, "station"));
        add( new Pair<>(IRRADIATED, "irradiated"));
        add( new Pair<>(TOXIC, "toxic"));
        add( new Pair<>(HOT, "hot"));
        add( new Pair<>(COLD, "cold"));
        add( new Pair<>(POLLUTED, "polluted"));
        add( new Pair<>(DEVELOPED, "civilized"));
        add( new Pair<>(ATMOSPHERE, "atmosph."));
    }};

    public static final Map<String, String[]> typeTagMap = new HashMap<String, String[]>(){{
        put("giant", new String[]{GAS});
        put("lava", new String[]{LAVA, HOT});
        put("frozen", new String[]{SNOW});
        put("barren", new String[]{BARREN});
        put("toxic", new String[]{TOXIC, INTERIOR});
        put("jungle", new String[]{JUNGLE});
        put("terran", new String[]{TERRAN});
        put("desert", new String[]{DESERT});
        put("arid", new String[]{ARID});
        put("cryovolcanic", new String[]{SNOW});
        put("rocky_metallic", new String[]{BARREN});
        put("rocky_unstable", new String[]{BARREN});
        put("rocky_ice", new String[]{SNOW, BARREN});
        put("water", new String[]{WATER});
        put("irradiated", new String[]{IRRADIATED});
        put("tundra", new String[]{TUNDRA});

        //us
        put("US_volcanic", new String[]{LAVA, HOT});
        put("US_ice", new String[]{SNOW});
        put("blue", new String[]{IRRADIATED});
        put("green", new String[]{ALIEN});
        put("acid", new String[]{TOXIC});
        put("azure", new String[]{IRRADIATED, HOT});
        put("burnt", new String[]{BARREN});
        put("dust", new String[]{ARID, DESERT});
        put("red", new String[]{DESERT, ALIEN});
        put("purple", new String[]{IRRADIATED});
        put("lifeless", new String[]{BARREN});
        put("alkali", new String[]{IRRADIATED});
        put("auric", new String[]{BARREN});
        put("continent", new String[]{WATER, TERRAN});
        put("magnetic", new String[]{BARREN});
        put("artificial", new String[]{INTERIOR, STATION});
        put("storm", new String[]{ARID, INTERIOR});
    }};

    public static final Map<String, String[]> kaleidoscopeNameMap = new HashMap<String, String[]>(){{
        put("Salty Arid", new String[]{SALT, ARID});
        put("Rocky Arid", new String[]{BARREN, ARID});
        put("Barren-Sulfuric", new String[]{TOXIC, BARREN});
        put("Rock Tundra", new String[]{TUNDRA, BARREN});
        put("Basalt Desert", new String[]{BARREN, DESERT});
        put("Salt Desert", new String[]{SALT, DESERT});
        put("Brine Desert", new String[]{SALT, DESERT});
        put("Crimson Jungle", new String[]{RED, JUNGLE});
        put("Fungal-Eccentric", new String[]{ALIEN});
        put("Red Terran", new String[]{RED, TERRAN});
        put("Fungal", new String[]{ALIEN});
    }};

    public static final Map<String, String[]> specificConditionTagMap = new HashMap<String, String[]>(){{
        //put("cold", new String[]{COLD});
        //put("very_cold", new String[]{COLD});
        //put("hot", new String[]{HOT}); // TODO: 09/11/2022 check if removing barren from here didn't make image selection too narrow
        //put("very_hot", new String[]{HOT});
        put("no_atmosphere", new String[]{INTERIOR});
        put("thin_atmosphere", new String[]{INTERIOR});
        put("mild_climate", new String[]{HABITABLE});
        //put("extreme_weather", new String[]{INTERIOR});
        put("irradiated", new String[]{IRRADIATED, ALIEN, INTERIOR});
    }};

    public static final Map<String, String[]> generalConditionTagMap = new HashMap<String, String[]>(){{
        put("hot", new String[]{HOT});
        put("cold", new String[]{COLD});
        put("habitable", new String[]{HABITABLE});
        put("toxic", new String[]{TOXIC, ALIEN});
        put("water", new String[]{WATER});
        put("farm", new String[]{HABITABLE});
        put("pollution", new String[]{POLLUTED, INTERIOR});

        //us
        put("storm", new String[]{DESERT});
    }};

    public static enum MatchMode{
        EXACT,
        BROAD,
        ANY
    }
}
