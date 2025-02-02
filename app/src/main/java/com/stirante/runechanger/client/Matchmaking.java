package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.lolclient.ClientWebSocket;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.utils.SimplePreferences;
import com.stirante.runechanger.utils.AsyncTask;
import generated.LolMatchmakingMatchmakingDodgeState;
import generated.LolMatchmakingMatchmakingSearchResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Matchmaking extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Matchmaking.class);

    public static final String MATCHMAKING_SEARCH_EVENT = "/lol-matchmaking/v1/search";

    public Matchmaking(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(MATCHMAKING_SEARCH_EVENT)
    public void onMatchmakingSearch(ClientEventListener.ClientEvent<LolMatchmakingMatchmakingSearchResource> event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false)) {
            LolMatchmakingMatchmakingSearchResource data = event.getData();
            if (data != null && data.dodgeData != null &&
                    data.dodgeData.state == LolMatchmakingMatchmakingDodgeState.STRANGERDODGED &&
                    data.isCurrentlyInQueue) {
                try {
                    log.debug("restart matchmaking");
                    getApi().executeDelete("/lol-lobby/v2/lobby/matchmaking/search");
                    AsyncTask.EXECUTOR_SERVICE.submit(() -> {
                        try {
                            Thread.sleep(1000);
                            getApi().executePost("/lol-lobby/v2/lobby/matchmaking/search");
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
