package com.stirante.runechanger.gui.overlay;

import com.stirante.eventbus.EventBus;
import com.stirante.eventbus.Subscribe;
import com.stirante.runechanger.gui.Constants;
import com.stirante.runechanger.gui.SceneType;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.util.SimplePreferences;
import com.stirante.runechanger.util.UiEventExecutor;

import java.util.List;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ChampionSuggestions extends OverlayLayer {
    private int selectedChampionIndex = -1;
    private List<Champion> lastChampions;
    private List<Champion> bannedChampions;
    private Consumer<Champion> suggestedChampionSelectedListener;
    private float currentChampionsPosition = 0f;

    ChampionSuggestions(ClientOverlay overlay) {
        super(overlay);
    }

    public void setSuggestedChampions(List<Champion> lastChampions,
                                      List<Champion> bannedChampions, Consumer<Champion> suggestedChampionSelectedListener) {
        this.lastChampions = lastChampions;
        this.bannedChampions = bannedChampions;
        this.suggestedChampionSelectedListener = suggestedChampionSelectedListener;
        repaintNow();
    }

    @Override
    protected void draw(Graphics g) {
        if (SimplePreferences.getBooleanValue(SimplePreferences.SettingsKeys.CHAMPION_SUGGESTIONS, true)) {
            if (Champion.areImagesReady()) {
                if (lastChampions == null) {
                    return;
                }
                if (getRuneChanger().getChampionSelectionModule().getGameMode() == null ||
                        !getRuneChanger().getChampionSelectionModule().getGameMode().hasChampionSelection()) {
                    return;
                }
                if ((getSceneType() != SceneType.CHAMPION_SELECT &&
                        getSceneType() != SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT) ||
                        getRuneChanger().getChampionSelectionModule().isChampionLocked()) {
                    if (currentChampionsPosition > 1f) {
                        currentChampionsPosition = ease(currentChampionsPosition, 0f);
                        repaintLater();
                    }
                    else {
                        currentChampionsPosition = 0f;
                    }
                }
                else {
                    if (currentChampionsPosition < 99f) {
                        currentChampionsPosition = ease(currentChampionsPosition, 100f);
                        repaintLater();
                    }
                    else {
                        currentChampionsPosition = 100f;
                    }
                }
                g.setColor(DARKER_TEXT_COLOR);
                int barWidth = (int) (Constants.CHAMPION_SUGGESTION_WIDTH * getHeight());
                g.drawRect(getWidth() - barWidth + 1 + (int) (currentChampionsPosition / 100f * barWidth) - barWidth, 0,
                        barWidth - 2, getHeight() - 1);
                g.setColor(BACKGROUND_COLOR);
                g.fillRect(getWidth() - barWidth + (int) (currentChampionsPosition / 100f * barWidth) - barWidth, 1,
                        barWidth - 1, getHeight() - 2);
                int tileIndex = 0;
                for (Champion champion : lastChampions) {
                    if (bannedChampions.contains(champion)) {
                        continue;
                    }
                    Image img = champion.getPortrait();
                    int tileSize = (int) (Constants.CHAMPION_TILE_SIZE * getHeight());
                    int rowSize = getHeight() / 6;
                    if (selectedChampionIndex == tileIndex) {
                        g.setColor(LIGHTEN_COLOR);
                        g.fillRect(getClientWidth(), rowSize * tileIndex, barWidth, rowSize);
                    }
                    g.drawImage(img,
                            (getClientWidth() + (barWidth - tileSize) / 2) +
                                    (int) (currentChampionsPosition / 100f * barWidth) - barWidth,
                            (rowSize - tileSize) / 2 + (rowSize * tileIndex),
                            tileSize, tileSize, null);
                    if (tileIndex >= 6) {
                        break;
                    }
                    tileIndex++;
                }
                clearRect(g, getClientWidth() - barWidth, 0, barWidth, getHeight());
            }
            else if (!Champion.areImagesReady()) {
                EventBus.register(this);
            }
        }
    }

    @Subscribe(value = Champion.IMAGES_READY_EVENT, eventExecutor = UiEventExecutor.class)
    public void onImagesReady() {
        repaintLater();
        EventBus.unregister(this);
    }

    public void mouseReleased(MouseEvent e) {
        if (selectedChampionIndex != -1 && suggestedChampionSelectedListener != null && bannedChampions != null && lastChampions != null) {
            // Fix wrong champion selected, when one or more of them are banned
            int index = selectedChampionIndex;
            for (int i = 0; i <= index; i++) {
                if (bannedChampions.contains(lastChampions.get(i))) {
                    index++;
                }
            }
            if (index < lastChampions.size()) {
                suggestedChampionSelectedListener.accept(lastChampions.get(index));
            }
        }
    }

    public void mouseExited(MouseEvent e) {
        if (selectedChampionIndex != -1) {
            selectedChampionIndex = -1;
            repaintNow();
        }
    }

    public void mouseMoved(MouseEvent e) {
        int championIndex;
        if (e.getX() < getClientWidth()) {
            championIndex = -1;
        }
        else {
            championIndex = (int) ((float) e.getY() / (float) (getHeight() / 6));
            getClientOverlay().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        if (championIndex > 5) {
            championIndex = -1;
        }
        if (championIndex != selectedChampionIndex) {
            selectedChampionIndex = championIndex;
            repaintNow();
        }
    }
}
