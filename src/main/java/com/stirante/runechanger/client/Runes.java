package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.SimplePreferences;
import generated.LolPerksPerkPageResource;
import generated.LolPerksPlayerInventory;
import ly.count.sdk.java.Countly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Runes extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Runes.class);

    public Runes(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(ClientEventListener.CurrentSummonerEvent.NAME)
    public void onCurrentSummoner(ClientEventListener.CurrentSummonerEvent event) {
        resetSummoner();
    }

    public void setCurrentRunePage(RunePage page) {
        if (Countly.isInitialized()) {
            Countly.session()
                    .event("rune_page_selection")
                    .addSegment("remote", String.valueOf(page.getSource().startsWith("http")))
                    .record();
        }
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            ArrayList<LolPerksPerkPageResource> availablePages = new ArrayList<>();
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    availablePages.add(p);
                }
            }
            //change pages
            LolPerksPerkPageResource page1 =
                    getApi().executeGet("/lol-perks/v1/currentpage", LolPerksPerkPageResource.class);
            if (!page1.isEditable || !page1.isActive) {
                if (availablePages.size() > 0) {
                    page1 = availablePages.get(0);
                }
                else {
                    page1 = new LolPerksPerkPageResource();
                }
            }
            page.toClient(page1);
            //updating rune page sometimes bugs out client, so we remove and add new one
            if (page1.id != null) {
                getApi().executeDelete("/lol-perks/v1/pages/" + page1.id);
            }
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException ex) {
            log.error("Exception occurred while setting current rune page", ex);
        }
    }

    public List<RunePage> getRunePages() {
        List<RunePage> availablePages = new ArrayList<>();
        try {
            //get all rune pages
            LolPerksPerkPageResource[] pages =
                    getApi().executeGet("/lol-perks/v1/pages", LolPerksPerkPageResource[].class);
            //find available pages
            for (LolPerksPerkPageResource p : pages) {
                if (p.isEditable) {
                    RunePage value = RunePage.fromClient(p);
                    if (value != null) {
                        availablePages.add(value);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Exception occurred while getting all rune pages", e);
        }
        return availablePages;
    }

    public int getOwnedPageCount() {
        try {
            return getApi().executeGet("/lol-perks/v1/inventory", LolPerksPlayerInventory.class).ownedPageCount;
        } catch (IOException e) {
            log.error("Exception occurred while getting owned rune page count", e);
        }
        return 0;
    }

    @Subscribe(ClientEventListener.RunePagesEvent.NAME)
    public void onRunePages(ClientEventListener.RunePagesEvent event) {
        // Auto sync rune pages to RuneChanger
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_SYNC, false)) {
            syncRunePages();
        }
    }

    public void deletePage(RunePage page) {
        try {
            getApi().executeDelete("/lol-perks/v1/pages/" + page.getSource());
        } catch (IOException e) {
            log.error("Exception occurred while deleting a rune page", e);
        }
    }

    public void addPage(RunePage page) {
        LolPerksPerkPageResource page1 = new LolPerksPerkPageResource();
        page.toClient(page1);
        try {
            getApi().executePost("/lol-perks/v1/pages/", page1);
        } catch (IOException e) {
            log.error("Exception occurred while adding a rune page", e);
        }
    }

    public void syncRunePages() {
        ArrayList<RunePage> savedPages = SimplePreferences.getRuneBookValues();
        List<RunePage> clientPages = getRunePages();
        for (RunePage p : clientPages) {
            Optional<RunePage> savedPage = savedPages.stream()
                    .filter(runePage -> runePage.getName().equalsIgnoreCase(p.getName()))
                    .findFirst();
            if (savedPage.isPresent()) {
                savedPage.get().copyFrom(p);
            }
            else {
                SimplePreferences.addRuneBookPage(p);
            }
        }
    }

}
