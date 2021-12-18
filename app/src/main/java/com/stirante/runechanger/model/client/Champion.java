package com.stirante.runechanger.model.client;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.stirante.eventbus.EventBus;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.sourcestore.SourceStore;
import com.stirante.runechanger.sourcestore.impl.ChampionGGSource;
import com.stirante.runechanger.util.*;
import generated.Position;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Champion {

    public static final String IMAGES_READY_EVENT = "IMAGES_READY_EVENT";

    private static final Logger log = LoggerFactory.getLogger(Champion.class);
    private static final List<Champion> values = new ArrayList<>(256);
    private static final AtomicBoolean IMAGES_READY = new AtomicBoolean(false);
    private static final File portraitsDir = new File(PathUtils.getAssetsDir(), "champions");
    private static final LoadingCache<Champion, java.awt.Image> portraitCache = Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .removalListener((key, value, removalCause) -> {
                if (value != null) {
                    ((java.awt.Image) value).flush();
                }
            })
            .build(key -> {
                BufferedImage img;
                try {
                    img = ImageIO.read(new File(portraitsDir, key.getId() + ".jpg"));
                } catch (Exception e) {
                    return null;
                }
                if (img == null) {
                    return null;
                }
                BufferedImage scaledImage = SwingUtils.getScaledImage(70, 70, img);
                img.flush();
                return scaledImage;
            });

    static {
        portraitsDir.mkdirs();
    }

    private final int id;
    private String internalName;
    private String name;
    private String alias;
    private String url;
    private String pickQuote = "";
    private Position position;

    private Champion(int id, String internalName, String name, String alias, String url) {
        this.id = id;
        this.internalName = internalName;
        this.name = name;
        this.alias = alias;
        this.url = url;
    }

    /**
     * Return all champions
     *
     * @return unmodifiable list of all champions
     */
    public static List<Champion> values() {
        return List.copyOf(values);
    }

    /**
     * Get champion by id
     *
     * @param id id
     * @return champion
     */
    public static Champion getById(int id) {
        for (Champion champion : values) {
            if (champion.id == id) {
                return champion;
            }
        }
        return null;
    }

    /**
     * Get champion by name
     *
     * @param name name
     * @return champion
     */
    public static Champion getByName(String name) {
        return getByName(name, false);
    }

    /**
     * Get champion by name
     *
     * @param name             name
     * @param additionalChecks if set to true, there will be additional checks
     * @return champion
     */
    public static Champion getByName(String name, boolean additionalChecks) {
        for (Champion champion : values) {
            if (champion.name.equalsIgnoreCase(name) || champion.alias.equalsIgnoreCase(name) ||
                    champion.internalName.equalsIgnoreCase(name) ||
                    (additionalChecks && champion.name.replaceAll("[^a-zA-Z]", "")
                            .equalsIgnoreCase(name.replaceAll("[^a-zA-Z]", "")))) {
                return champion;
            }
        }
        return null;
    }

    /**
     * Initializes all champions in game
     */
    public static void init() throws IOException {
        PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.CHAMPIONS_INIT_START);
        File cache = new File(portraitsDir, "champions.json");
        if (cache.exists()) {
            try {
                offlineInit(cache);
            } catch (Exception e) {
                //Cache failed, try online init
                log.error("Exception occurred while performing offline init of champions", e);
                onlineInit(cache);
                return;
            }
            //Do online init anyways, but async, so it won't hold back GUI
            RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                try {
                    onlineInit(cache);
                } catch (IOException e) {
                    log.error("Exception occurred while performing online init of champions", e);
                }
            });
        }
        else {
            //We don't have any cache for champions, so we get all champions on the main thread since champion list is required for GUI to work
            onlineInit(cache);
        }
    }

    private static void offlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        try (FileReader fileReader = new FileReader(cache)) {
            Champion[] champions = gson.fromJson(fileReader, Champion[].class);
            synchronized (Champion.values) {
                Champion.values.clear();
                Champion.values.addAll(Arrays.asList(champions));
            }
        }
    }

    private static void onlineInit(File cache) throws IOException {
        Gson gson = new Gson();
        ChampionList champions;
        try (InputStream in = getUrl("http://ddragon.leagueoflegends.com/cdn/" + Patch.getLatest().toFullString() +
                "/data/en_US/champion.json")) {
            champions = gson.fromJson(new InputStreamReader(in), ChampionList.class);
        }
        List<ChampionDTO> values = new ArrayList<>(champions.data.values());
        values.sort(Comparator.comparing(o -> o.name));

        Patch patch = Patch.getLatest();
        List<Patch> latest = Patch.getLatest(5);
        for (Patch p : latest) {
            try {
                if (isOk("https://cdn.communitydragon.org/" + p.toFullString() + "/champion/Annie/square")) {
                    patch = p;
                    break;
                }
            } catch (IOException ignored) {
            }
        }

        synchronized (Champion.values) {
            boolean allExist = true;
            for (ChampionDTO champion : values) {
                Champion c = new Champion(Integer.parseInt(champion.key), champion.id, champion.name,
                        champion.name.replaceAll(" ", ""),
                        "https://cdn.communitydragon.org/" + patch.toFullString() + "/champion" +
                                "/" + champion.key);
                if (Champion.values.contains(c)) {
                    Champion champion1 =
                            Champion.values.stream().filter(champ -> champ.id == c.id).findFirst().orElseThrow();
                    champion1.name = c.name;
                    champion1.internalName = c.internalName;
                    champion1.alias = c.alias;
                    champion1.url = c.url;
                }
                else {
                    Champion.values.add(c);
                }

                File f = new File(portraitsDir, c.id + ".jpg");
                if (!f.exists()) {
                    allExist = false;
                }
            }
            if (allExist) {
                log.info("Portraits ready");
                DownloadThread.publishImagesReadyEvent();
            }
        }

        //Save json with champions to file for faster initialization next time
        try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
            out.print(gson.toJson(Champion.values()));
            out.flush();
        }

        new DownloadThread().start();
    }

    /**
     * Returns InputStream from provided url
     *
     * @param url url
     * @return InputStream from provided url
     */
    private static InputStream getUrl(String url) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
        return urlConnection.getInputStream();
    }

    private static boolean isOk(String url) throws IOException {
        URL urlObject = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) urlObject.openConnection();
        urlConnection.setRequestMethod("HEAD");
        int responseCode = urlConnection.getResponseCode();
        urlConnection.disconnect();
        return responseCode >= 200 && responseCode < 300;
    }

    /**
     * Returns whether all images are downloaded and loaded into memory.
     */
    public static boolean areImagesReady() {
        return IMAGES_READY.get();
    }

    /**
     * Returns image with champion's portrait
     */
    public java.awt.Image getPortrait() {
        return portraitCache.get(this);
    }

    /**
     * Get champion id
     *
     * @return champion id
     */
    public int getId() {
        return id;
    }

    /**
     * Get riot internal champion name
     *
     * @return internal champion name
     */
    public String getInternalName() {
        return internalName;
    }

    /**
     * Get champion name
     *
     * @return champion name
     */
    public String getName() {
        return name;
    }

    /**
     * Get alternative champion name
     *
     * @return alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * Get champion pick quote
     *
     * @return alias
     */
    public String getPickQuote() {
        return pickQuote;
    }

    public BufferedImage getSplashArt() {
        try {
            return ImageIO.read(new File(portraitsDir, id + "_full.jpg"));
        } catch (IOException e) {
            return null;
        }
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Champion && ((Champion) obj).id == id;
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", name, id);
    }

    private static class DownloadThread extends Thread {

        @Override
        public void run() {
            Gson gson = new Gson();
            Map<String, Long> splashes = new HashMap<>();
            try (InputStream in = new URL("https://runechanger.stirante.com/splashes.json").openStream()) {
                splashes = gson.fromJson(new InputStreamReader(in), new TypeToken<Map<String, Long>>() {
                }.getType());
            } catch (IOException | IllegalStateException e) {
                e.printStackTrace();
            }
            portraitsDir.getParentFile().mkdirs();
            for (Champion champion : values) {
                // Checking and downloading portrait
                File f = new File(portraitsDir, champion.id + ".jpg");
                boolean force = splashes.containsKey(String.valueOf(champion.getId())) &&
                        f.exists() &&
                        splashes.get(String.valueOf(champion.getId())) > f.lastModified();
                checkAndDownload(f,
                        champion.url + "/tile",
                        "v1/champion-tiles/" + champion.getId() + "/" + champion.getId() + "000.jpg", force);
                // Checking and downloading splash art
                f = new File(portraitsDir, champion.id + "_full.jpg");
                force = splashes.containsKey(String.valueOf(champion.getId())) &&
                        f.exists() &&
                        splashes.get(String.valueOf(champion.getId())) > f.lastModified();
                checkAndDownload(f,
                        champion.url + "/splash-art/centered",
                        "v1/champion-splashes/" + champion.getId() + "/" + champion.getId() + "000.jpg", force);
                // Getting pick quote for champion
                if (champion.pickQuote.isEmpty()) {
                    getQuoteForChampion(champion);
                }
                ChampionGGSource source = SourceStore.getSource(ChampionGGSource.class);
                if (source != null) {
                    champion.position = source.getPositionForChampion(champion);
                }
            }

            try {
                File cache = new File(portraitsDir, "champions.json");
                //Save json with champions to file for faster initialization next time (this time with pick quotes)
                try (PrintStream out = new PrintStream(new FileOutputStream(cache))) {
                    out.print(gson.toJson(Champion.values()));
                    out.flush();
                }
            } catch (IOException e) {
                log.error("Exception occurred while saving champions cache", e);
                AnalyticsUtil.addCrashReport(e, "Exception occurred while saving champions cache", false);
            }

            log.info("Champions initialized");
            publishImagesReadyEvent();
            PerformanceMonitor.pushEvent(PerformanceMonitor.EventType.CHAMPIONS_INIT_END);
        }

        private static void publishImagesReadyEvent() {
            IMAGES_READY.set(true);
            try {
                EventBus.publish(IMAGES_READY_EVENT);
            } catch (Throwable t) {
                log.error("Exception occurred while publishing images ready event", t);
                AnalyticsUtil.addCrashReport(t, "Exception occurred while publishing images ready event", false);
            }
        }

        private void getQuoteForChampion(Champion champion) {
            try {
                Document doc = Jsoup.parse(new URL("https://leagueoflegends.fandom.com/wiki/" +
                        // Special case for Nunu...
                        URLEncoder.encode(champion.getName()
                                .replaceAll(" ", "_")
                                .replaceAll("_&.+", ""), StandardCharsets.UTF_8) +
                        "/LoL/Audio"), 60000);
                Elements select = doc.select("#mw-content-text .mw-parser-output").first().children();
                boolean isPick = false;
                for (Element element : select) {
                    if (element.tagName().equalsIgnoreCase("dl") && element.text().equalsIgnoreCase("Pick")) {
                        isPick = true;
                        continue;
                    }
                    if (isPick) {
                        champion.pickQuote = StringUtils.fixString(element.text()).replaceAll("\"", "");
                        break;
                    }
                }
                if (!isPick) {
                    Elements elements = doc.select("#mw-content-text > .tabber > .tabbertab");
                    if (elements != null && elements.first() != null) {
                        select = elements.first().children();
                        for (Element element : select) {
                            if (element.tagName().equalsIgnoreCase("dl") &&
                                    element.text().equalsIgnoreCase("Pick")) {
                                isPick = true;
                                continue;
                            }
                            if (isPick) {
                                champion.pickQuote = StringUtils.fixString(element.select("i").first().text())
                                        .replaceAll("\"", "");
                                break;
                            }
                        }
                    }
                }
                if (!isPick || champion.pickQuote == null || champion.pickQuote.isBlank()) {
                    champion.pickQuote = champion.name + " laughs.";
                }
            } catch (IOException e) {
                log.error("Exception occurred while getting quote for champion " + champion.name, e);
            }
        }

        private void checkAndDownload(File f, String url, String lcuPath, boolean force) {
            if (!f.exists() || force) {
                if (force) {
                    log.info("Forcing download, because champion splash art changed");
                }
                try {
                    BufferedInputStream in;
                    if (RuneChanger.getInstance() == null || RuneChanger.getInstance().getApi() == null ||
                            !RuneChanger.getInstance().getApi().isConnected()) {
                        log.info("Downloading " + url);
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
                        in = new BufferedInputStream(conn.getInputStream());
                    }
                    else {
                        log.info("Getting LCU asset " + lcuPath);
                        in = new BufferedInputStream(RuneChanger.getInstance()
                                .getApi()
                                .getAsset("lol-game-data", lcuPath));
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(f);
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                    in.close();
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.error("Exception occurred while downloading asset", e);
                }
            }
        }

    }

    public static class ChampionDTO {
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

    public static class ChampionList {
        public String type;
        public String format;
        public String version;
        public HashMap<String, ChampionDTO> data;
    }

    public static class Image {
        public String full;
        public String sprite;
        public String group;
        public Integer x;
        public Integer y;
        public Integer w;
        public Integer h;
    }

    public static class Info {
        public Double attack;
        public Double defense;
        public Double magic;
        public Double difficulty;
    }

    public static class Stats {
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
