package com.stirante.RuneChanger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.model.Rune;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Internal class for generating enums and resources
 */
public class DataUpdater {

    private static final String RUNE_ENUM_PREFIX = "package com.stirante.RuneChanger.model;\n" +
            "\n" +
            "import javax.imageio.ImageIO;\n" +
            "import java.awt.image.BufferedImage;\n" +
            "import java.io.IOException;\n" +
            "\n" +
            "public enum Rune {\n";
    private static final String RUNE_ENUM_POSTFIX = "\n" +
            "\n" +
            "\n" +
            "    private final int id;\n" +
            "    private final Style style;\n" +
            "    private final int slot;\n" +
            "    private final String name;\n" +
            "    private BufferedImage image;\n" +
            "\n" +
            "    Rune(int id, int styleId, int slot, String name) {\n" +
            "        this.id = id;\n" +
            "        style = Style.getById(styleId);\n" +
            "        this.slot = slot;\n" +
            "        this.name = name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune id\n" +
            "     *\n" +
            "     * @return rune id\n" +
            "     */\n" +
            "    public int getId() {\n" +
            "        return id;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune name\n" +
            "     *\n" +
            "     * @return rune name\n" +
            "     */\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get style this rune belongs to\n" +
            "     *\n" +
            "     * @return style\n" +
            "     */\n" +
            "    public Style getStyle() {\n" +
            "        return style;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get slot where this rune can be placed\n" +
            "     *\n" +
            "     * @return slot\n" +
            "     */\n" +
            "    public int getSlot() {\n" +
            "        return slot;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune image. Works only for keystones\n" +
            "     *\n" +
            "     * @return image\n" +
            "     */\n" +
            "    public BufferedImage getImage() {\n" +
            "        if (getSlot() != 0) return null;\n" +
            "        if (image == null) {\n" +
            "            try {\n" +
            "                image = ImageIO.read(getClass().getResourceAsStream(\"/runes/\" + getId() + \".png\"));\n" +
            "            } catch (IOException e) {\n" +
            "                e.printStackTrace();\n" +
            "            }\n" +
            "        }\n" +
            "        return image;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get rune by name\n" +
            "     *\n" +
            "     * @param name rune name\n" +
            "     * @return rune\n" +
            "     */\n" +
            "    public static Rune getByName(String name) {\n" +
            "        for (Rune rune : values()) {\n" +
            "            if (rune.name.equalsIgnoreCase(name) || rune.name.replaceAll(\"'\", \"’\").equalsIgnoreCase(name)) return rune;\n" +
            "        }\n" +
            "        System.out.println(name + \" not found\");\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public String toString() {\n" +
            "        return name() + \"(\" + name + \")\";\n" +
            "    }\n" +
            "}\n";

    private static final String CHAMPION_ENUM_PREFIX = "package com.stirante.RuneChanger.model;\n" +
            "\n" +
            "public enum Champion {\n";
    private static final String CHAMPION_ENUM_POSTFIX = "\n" +
            "\n" +
            "    private final int id;\n" +
            "    private final String internalName;\n" +
            "    private final String name;\n" +
            "    private final String alias;\n" +
            "\n" +
            "    Champion(int id, String internalName, String name, String alias) {\n" +
            "        this.id = id;\n" +
            "        this.internalName = internalName;\n" +
            "        this.name = name;\n" +
            "        this.alias = alias;\n" +
            "    }\n" +
            "\n" +
            "    Champion(int id, String internalName, String name) {\n" +
            "        this(id, internalName, name, name);\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion id\n" +
            "     *\n" +
            "     * @return champion id\n" +
            "     */\n" +
            "    public int getId() {\n" +
            "        return id;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get riot internal champion name\n" +
            "     *\n" +
            "     * @return internal champion name\n" +
            "     */\n" +
            "    public String getInternalName() {\n" +
            "        return internalName;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion name\n" +
            "     *\n" +
            "     * @return champion name\n" +
            "     */\n" +
            "    public String getName() {\n" +
            "        return name;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get alternative champion name\n" +
            "     *\n" +
            "     * @return alias\n" +
            "     */\n" +
            "    public String getAlias() {\n" +
            "        return alias;\n" +
            "    }\n" +
            "\n" +
            "    /**\n" +
            "     * Get champion by id\n" +
            "     *\n" +
            "     * @param id id\n" +
            "     * @return champion\n" +
            "     */\n" +
            "    public static Champion getById(int id) {\n" +
            "        for (Champion champion : values()) {\n" +
            "            if (champion.id == id) return champion;\n" +
            "        }\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "}\n";

    private static InputStream getEndpoint(String endpoint) throws IOException {
        System.out.println(endpoint);
        URL url = new URL(endpoint);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        return urlConnection.getInputStream();
    }

    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().create();
        String patch = getLatestPatch(gson);
        generateChampions(gson, patch);
        generateRunes(gson, patch);
        downloadImages();
    }

    private static String getLatestPatch(Gson gson) throws IOException {
        InputStream in = getEndpoint("https://ddragon.leagueoflegends.com/api/versions.json");
        String[] strings = gson.fromJson(new InputStreamReader(in), String[].class);
        return strings[0];
    }

    private static void generateChampions(Gson gson, String patch) throws IOException {
        InputStream in = getEndpoint("http://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/champion.json");
        StringBuilder sb = new StringBuilder();
        ChampionList champions = gson.fromJson(new InputStreamReader(in), ChampionList.class);
        in.close();
        List<Champion> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));
        for (int i = 0; i < values.size(); i++) {
            Champion champion = values.get(i);
            sb.append("    ")
                    .append(champion.id.toUpperCase())
                    .append("(")
                    .append(champion.key)
                    .append(", \"")
                    .append(champion.id)
                    .append("\", \"")
                    .append(champion.name)
                    .append("\", \"")
                    .append(champion.name.replaceAll(" ", ""))
                    .append("\")");
            if (i == values.size() - 1) {
                sb.append(";\n");
            }
            else {
                sb.append(",\n");
            }
        }
        try {
            FileWriter writer = new FileWriter(new File("src/main/java/com/stirante/RuneChanger/model/Champion.java"));
            writer.write(CHAMPION_ENUM_PREFIX + "    //Generated on " +
                    SimpleDateFormat.getDateTimeInstance().format(new Date()) + "\n" + sb.toString() +
                    CHAMPION_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateRunes(Gson gson, String patch) throws IOException {
        InputStream in =
                getEndpoint("http://ddragon.leagueoflegends.com/cdn/" + patch + "/data/en_US/runesReforged.json");
        StringBuilder sb = new StringBuilder();
        ReforgedRunePathDto[] runes = gson.fromJson(new InputStreamReader(in), ReforgedRunePathDto[].class);
        in.close();
        List<ReforgedRuneDto> runes1 = new ArrayList<>();
        for (ReforgedRunePathDto rune : runes) {
            List<ReforgedRuneSlotDto> slots = rune.slots;
            for (int i = 0; i < slots.size(); i++) {
                ReforgedRuneSlotDto slot = slots.get(i);
                for (ReforgedRuneDto runeDto : slot.runes) {
                    runeDto.slot = i;
                    runeDto.runePathId = rune.id;
                }
                runes1.addAll(slot.runes);
            }
        }
        runes1.sort(Comparator.comparingInt(o -> o.id));
        for (int i = 0; i < runes1.size(); i++) {
            ReforgedRuneDto rune = runes1.get(i);
            sb.append("    RUNE_")
                    .append(rune.id)
                    .append("(")
                    .append(rune.id)
                    .append(", ")
                    .append(rune.runePathId)
                    .append(", ")
                    .append(rune.slot)
                    .append(", \"")
                    .append(rune.name)
                    .append("\")");
            if (i == runes1.size() - 1) {
                sb.append(";\n");
            }
            else {
                sb.append(",\n");
            }
        }
        try {
            FileWriter writer = new FileWriter(new File("src/main/java/com/stirante/RuneChanger/model/Rune.java"));
            writer.write(RUNE_ENUM_PREFIX + "    //Generated on " +
                    SimpleDateFormat.getDateTimeInstance().format(new Date()) + "\n" + sb.toString() +
                    RUNE_ENUM_POSTFIX);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadImages() {
        HashMap<Rune, String> replacements = new HashMap<>();
        replacements.put(Rune.RUNE_8439, "veteranaftershock");
        for (Rune rune : Rune.values()) {
            if (rune.getSlot() == 0) {
                String internalName = rune.getName().toLowerCase().replaceAll(" ", "");
                if (replacements.containsKey(rune)) {
                    internalName = replacements.get(rune);
                }
                try {
                    String url =
                            "https://raw.communitydragon.org/latest/plugins/rcp-be-lol-game-data/global/default/v1/perk-images/styles/";
                    URL input = new URL(url + rune.getStyle().getName().toLowerCase() + "/" + internalName + "/" +
                            internalName + (rune == Rune.RUNE_8008 ? "temp" : "") + ".png");
                    HttpURLConnection conn = (HttpURLConnection) input.openConnection();
                    conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                    BufferedImage read = ImageIO.read(conn.getInputStream());
                    conn.getInputStream().close();
                    ImageIO.write(read, "png", new File("src/main/resources/runes/" + rune.getId() + ".png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class ReforgedRuneDto {
        public String runePathName;
        public int runePathId;
        public String name;
        public int id;
        public String key;
        public String shortDesc;
        public String longDesc;
        public String icon;
        public volatile int slot = -1;
    }

    public static class ReforgedRuneSlotDto {
        public List<ReforgedRuneDto> runes;
    }

    public static class ReforgedRunePathDto {
        public String icon;
        public int id;
        public String key;
        public String name;
        List<ReforgedRuneSlotDto> slots;
    }

    public class Champion {
        public String version;
        public String id;
        public String key;
        public String name;
        public String title;
        public String blurb;
        public Info info;
        public Image image;
        public List<String> tags = null;
        public String partype;
        public Stats stats;
    }

    public class ChampionList {
        public String type;
        public String format;
        public String version;
        public HashMap<String, Champion> data;
    }

    public class Image {
        public String full;
        public String sprite;
        public String group;
        public Integer x;
        public Integer y;
        public Integer w;
        public Integer h;
    }

    public class Info {
        public Double attack;
        public Double defense;
        public Double magic;
        public Double difficulty;
    }

    public class Stats {
        public Double hp;
        public Double hpperlevel;
        public Double mp;
        public Double mpperlevel;
        public Double movespeed;
        public Double armor;
        public Double armorperlevel;
        public Double spellblock;
        public Double spellblockperlevel;
        public Double attackrange;
        public Double hpregen;
        public Double hpregenperlevel;
        public Double mpregen;
        public Double mpregenperlevel;
        public Double crit;
        public Double critperlevel;
        public Double attackdamage;
        public Double attackdamageperlevel;
        public Double attackspeedperlevel;
        public Double attackspeed;
    }

}
