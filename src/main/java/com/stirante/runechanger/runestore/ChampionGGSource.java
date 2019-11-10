package com.stirante.runechanger.runestore;

import com.stirante.runechanger.model.client.*;
import com.stirante.runechanger.util.FxUtils;
import generated.Position;
import javafx.collections.ObservableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ChampionGGSource implements RuneSource {
    private static final Logger log = LoggerFactory.getLogger(ChampionGGSource.class);

    private final static String CHAMPION_URL = "https://champion.gg/champion/";
    private final static String BASE_URL = "https://champion.gg/";
    //    private final String[] ROLES = {"Jungle", "Middle", "ADC", "Top", "Support"};
    private final HashMap<Champion, Position> positionCache = new HashMap<>();

    private void extractRunePage(Document webPage, Champion champion, String role, ObservableList<RunePage> pages) {
        RunePage page = new RunePage();
        Elements elements = webPage.select("div.o-wrap");
        elements = elements.select("div.RuneBuilder__PathBody-dchrMz.bKqgWU");
        Element mainSide;
        Element secondarySide;
        try {
            mainSide = elements.first().child(0).child(0);
            secondarySide = elements.first().child(1).child(0);
        } catch (NullPointerException e) {
            return;
        }
        page.setName(role);
        page.setMainStyle(Style.getByName(mainSide.child(0).text().substring(2)));
        page.setSubStyle(Style.getByName(secondarySide.child(0).text().substring(2)));
        for (Element e : mainSide.children()) {
            String rune =
                    e.select(".iSYqxs.Slot__RightSide-bGHpkV > div:nth-of-type(1) > .hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH")
                            .text();
            if (rune.equals("")) {
                continue;
            }
            page.getRunes().add(Rune.getByName(rune));
        }

        Element subRunesParent = secondarySide.child(1);
        Elements subRunes =
                subRunesParent.select(".iSYqxs.Slot__RightSide-bGHpkV > .hGZpqL.Description__Block-bJdjrS > .eOLOWg.Description__Title-jfHpQH");
        for (Element e : subRunes) {
            if (e.text().equals("")) {
                continue;
            }
            page.getRunes().add(Rune.getByName(e.text()));
        }

        Element modifierParent = secondarySide.child(2);
        Elements modifiers =
                modifierParent.select(".iLoveCSS.iSYqxs.Slot__RightSide-bGHpkV > .statShardsOS.hGZpqL.Description__Block-bJdjrS > .bJtdXG.Description__Title-jfHpQH");
        for (Element e : modifiers) {
            page.getModifiers().add(modifierConverter(e.text()));
        }

        page.setSource(webPage.baseUri());
        page.setChampion(champion);
        if (page.verify() && !pages.contains(page)) {
            FxUtils.doOnFxThread(() -> pages.add(page));
        }
    }

    private void extractRunes(Champion champion, ObservableList<RunePage> pages) {
        final String URL = CHAMPION_URL + champion.getInternalName() + "/";
        log.info("Visiting page to check for runes: " + URL);
        try {
            Document webPage = Jsoup.connect(URL).get();
            List<String> collect = webPage.select(".champion-profile ul li a h3")
                    .stream()
                    .map(Element::text)
                    .collect(Collectors.toList());
            String role = webPage.baseUri().replace(URL, "").replace("?", "");
            extractRunePage(webPage, champion, role, pages);
            for (String s : collect) {
                if (s.equalsIgnoreCase(role)) {
                    continue;
                }
                extractRunePage(webPage, champion, s, pages);
            }

        } catch (IOException e) {
            log.warn("ERROR RETRIEVING CHAMPION FROM Champion.gg! " + e);
        }
    }

    private Modifier modifierConverter(String modifierName) {
        switch (modifierName.toLowerCase()) {
            case "attack speed":
                return Modifier.RUNE_5005;
            case "adaptive force":
                return Modifier.RUNE_5008;
            case "armor":
                return Modifier.RUNE_5002;
            case "magic resist":
                return Modifier.RUNE_5003;
            case "scaling cooldown reduction":
                return Modifier.RUNE_5007;
            case "scaling health":
                return Modifier.RUNE_5001;
            default:
                return null;
        }
    }

    public Position getPositionForChampion(Champion champion) {
        if (positionCache.isEmpty()) {
            try {
                Document webPage = Jsoup.connect(BASE_URL).get();
                Elements select = webPage.select(".champ-index-img a:nth-child(2)");
                for (Element element : select) {
                    String championName = element.attr("href").split("/")[2];
                    Position pos = Position.UNSELECTED;
                    switch (element.text()) {
                        case "Support":
                            pos = Position.UTILITY;
                            break;
                        case "Middle":
                            pos = Position.MIDDLE;
                            break;
                        case "ADC":
                            pos = Position.BOTTOM;
                            break;
                        case "Top":
                            pos = Position.TOP;
                            break;
                        case "Jungle":
                            pos = Position.JUNGLE;
                            break;
                        default:
                            log.warn("Unknown position name: " + element.text());
                            break;
                    }
                    positionCache.put(Champion.getByName(championName), pos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!positionCache.containsKey(champion)) {
            log.warn("Champion not found: " + champion.getName());
            return Position.UNSELECTED;
        }
        return positionCache.get(champion);
    }

    @Override
    public String getSourceName() {
        return "Champion.gg";
    }

    @Override
    public void getForChampion(Champion champion, ObservableList<RunePage> pages) {
        extractRunes(champion, pages);
    }

//    public static void main(String[] args) throws IOException {
//        Champion.init();
//
//        ChampionGGSource source = new ChampionGGSource();
//        Champion.values()
//                .forEach(champion -> System.out.println(
//                        source.getForChampion(champion).size() + " pages were found for " + champion.getName()));
//    }
}
