package illustratedEntities.helper;

import com.fs.starfarer.api.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageTags {
    
    public static final String
            DERELICT = "drl",
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
            GAS = "gas",
            CITY = "cty",
            INTERIOR = "int",
            HABITABLE = "hab",
            MINE = "mne",
            STATION = "sta",
            IRRADIATED = "irr",
            TOXIC = "tox",
            HOT = "hot",
            COLD = "cld",
            POLLUTED = "pol",
            DEVELOPED = "dvl";

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
        add( new Pair<>(CITY, "city"));
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
    }};

    public static final Map<String, String[]> typeTagMap = new HashMap<String, String[]>(){{
        put("giant", new String[]{GAS});
        put("lava", new String[]{LAVA, HOT});
        put("frozen", new String[]{SNOW, TUNDRA});
        put("barren", new String[]{BARREN});
        put("toxic", new String[]{TOXIC});
        put("jungle", new String[]{JUNGLE});
        put("terran", new String[]{TERRAN});
        put("desert", new String[]{DESERT, HOT});
        put("arid", new String[]{ARID, HOT});
        put("cryovolcanic", new String[]{SNOW});
        put("rocky_metallic", new String[]{BARREN});
        put("rocky_unstable", new String[]{BARREN});
        put("rocky_ice", new String[]{SNOW, TUNDRA});
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
        put("burnt", new String[]{BARREN, HOT});
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

    public static final Map<String, String[]> specificConditionTagMap = new HashMap<String, String[]>(){{
        //put("cold", new String[]{COLD});
        //put("very_cold", new String[]{COLD});
        //put("hot", new String[]{HOT}); // TODO: 09/11/2022 check if removing barren from here didn't make image selection too narrow
        //put("very_hot", new String[]{HOT});
        put("no_atmosphere", new String[]{INTERIOR});
        put("mild_climate", new String[]{HABITABLE, CITY});
        put("extreme_weather", new String[]{INTERIOR});
        put("irradiated", new String[]{IRRADIATED, ALIEN, INTERIOR});
    }};

    public static final Map<String, String[]> generalConditionTagMap = new HashMap<String, String[]>(){{
        put("hot", new String[]{HOT});
        put("cold", new String[]{COLD});
        put("habitable", new String[]{HABITABLE});
        put("toxic", new String[]{TOXIC, ALIEN});
        put("water", new String[]{WATER});
        put("farm", new String[]{HABITABLE});
        put("pollution", new String[]{POLLUTED});

        //us
        put("storm", new String[]{DESERT});
        put("floating", new String[]{CITY});
    }};
}
