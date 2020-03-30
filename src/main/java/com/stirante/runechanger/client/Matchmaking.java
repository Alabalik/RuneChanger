package com.stirante.runechanger.client;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.lolclient.ClientApi;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.util.SimplePreferences;
import generated.LolLobbyLobby;
import generated.LolLobbyLobbyMatchmakingSearchState;
import generated.LolMatchmakingMatchmakingDodgeState;
import generated.LolMatchmakingMatchmakingSearchResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Matchmaking extends ClientModule {
    private static final Logger log = LoggerFactory.getLogger(Matchmaking.class);

    public Matchmaking(ClientApi api) {
        super(api);
        EventBus.register(this);
    }

    @Subscribe(ClientEventListener.MatchmakingSearchEvent.NAME)
    public void onMatchmakingSearch(ClientEventListener.MatchmakingSearchEvent event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.RESTART_ON_DODGE, false)) {
            log.debug("matchmaking update");
            LolMatchmakingMatchmakingSearchResource data = event.getData();
            if (data != null && data.dodgeData != null &&
                    data.dodgeData.state == LolMatchmakingMatchmakingDodgeState.STRANGERDODGED &&
                    data.isCurrentlyInQueue) {
                log.debug(String.valueOf(data.dodgeData.state));
                log.debug("in queue: " + data.isCurrentlyInQueue);
                try {
                    log.debug("restart matchmaking");
                    LolLobbyLobby lobby = getApi().executeGet("/lol-lobby/v1/lobby", LolLobbyLobby.class);
                    log.debug("canStartMatchmaking: " + lobby.canStartMatchmaking);
                    if (lobby.canStartMatchmaking) {
                        getApi().executeDelete("/lol-lobby/v2/lobby/matchmaking/search");
                        RuneChanger.EXECUTOR_SERVICE.submit(() -> {
                            try {
                                Thread.sleep(1000);
                                getApi().executePost("/lol-lobby/v2/lobby/matchmaking/search");
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Subscribe(ClientEventListener.MatchmakingSearchStateEvent.NAME)
    public void onMatchmakingSearchState(ClientEventListener.MatchmakingSearchStateEvent event) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.AUTO_ACCEPT, false)) {
            if (event.getData().searchState == LolLobbyLobbyMatchmakingSearchState.FOUND) {
                try {
                    getApi().executePost("/lol-matchmaking/v1/ready-check/accept");
                } catch (IOException e) {
                    log.error("Exception occurred while autoaccepting", e);
                }
            }
        }
    }

}
