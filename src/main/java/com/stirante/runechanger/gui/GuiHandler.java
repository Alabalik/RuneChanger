package com.stirante.runechanger.gui;

import com.stirante.runechanger.DebugConsts;
import com.stirante.runechanger.RuneChanger;
import com.stirante.runechanger.client.ScreenPointChecker;
import com.stirante.runechanger.gui.overlay.ChampionSuggestions;
import com.stirante.runechanger.gui.overlay.ClientOverlay;
import com.stirante.runechanger.gui.overlay.RuneMenu;
import com.stirante.runechanger.model.client.Champion;
import com.stirante.runechanger.model.client.RunePage;
import com.stirante.runechanger.util.LangHelper;
import com.stirante.runechanger.util.NativeUtils;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class GuiHandler {
    public static final int MINIMIZED_POSITION = -32000;
    private static final Logger log = LoggerFactory.getLogger(GuiHandler.class);
    private final AtomicBoolean threadRunning = new AtomicBoolean(false);
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);
    private final List<RunePage> runes = Collections.synchronizedList(new ArrayList<>());
    private final ResourceBundle resourceBundle = LangHelper.getLang();
    private final RuneChanger runeChanger;
    private final ReentrantLock lock = new ReentrantLock();
    private JWindow win;
    private ClientOverlay clientOverlay;
    private WinDef.HWND hwnd;
    private Consumer<RunePage> runeSelectedListener;
    private TrayIcon trayIcon;
    private SceneType type = SceneType.NONE;
    private ArrayList<Champion> suggestedChampions;
    private Consumer<Champion> suggestedChampionSelectedListener;
    private ArrayList<Champion> bannedChampions;
    private int screenCheckCounter = 0;

    public GuiHandler(RuneChanger runeChanger) {
        this.runeChanger = runeChanger;
        init();
    }

    /**
     * Is window open
     *
     * @return is open
     */
    public boolean isWindowOpen() {
        return windowOpen.get();
    }

    /**
     * Closes window
     */
    public void closeWindow() {
        lock.lock();
        log.info("Closing window");
        if (win != null) {
            win.dispose();
            win = null;
        }
        windowOpen.set(false);
        lock.unlock();
    }

    /**
     * Opens window
     */
    public void openWindow() {
        lock.lock();
        log.info("Opening window");
        if (!windowOpen.get()) {
            startWindow();
            windowOpen.set(true);
        }
        lock.unlock();
        startThread();
    }

    /**
     * Gets scene type. Currently, there can be only 3 types, but there will be more in the future
     */
    public SceneType getSceneType() {
        return type;
    }

    /**
     * Sets scene type. Currently, there can be only 3 types, but there will be more in the future
     */
    public void setSceneType(SceneType type) {
        this.type = type;
        if (clientOverlay != null) {
            clientOverlay.setSceneType(type);
        }
        if (type == SceneType.NONE) {
            runes.clear();
            if (clientOverlay != null) {
                clientOverlay.getLayer(RuneMenu.class).setRuneData(runes, null);
            }
        }
    }

    public void setRunes(List<RunePage> runeList, Consumer<RunePage> onClickListener) {
        runes.clear();
        runes.addAll(runeList);
        runeSelectedListener = onClickListener;
        if (clientOverlay != null) {
            clientOverlay.getLayer(RuneMenu.class).setRuneData(runes, onClickListener);
        }
    }

    public void setRunes(List<RunePage> runeList) {
        runes.clear();
        runes.addAll(runeList);
        if (clientOverlay != null) {
            clientOverlay.getLayer(RuneMenu.class).setRuneData(runes, runeSelectedListener);
        }
    }

    public void showInfoMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.INFO);
        }
    }

    public void showErrorMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.ERROR);
        }
    }

    public void showWarningMessage(String message) {
        if (!DebugConsts.DISABLE_NOTIFICATIONS) {
            trayIcon.displayMessage(Constants.APP_NAME, message, TrayIcon.MessageType.WARNING);
        }
    }

    /**
     * Actually create and show client overlay
     *
     * @param rect client window bounds
     */
    private void showWindow(Rectangle rect) {
        if (win != null) {
            win.dispose();
        }
        win = new JWindow();
        clientOverlay = new ClientOverlay(runeChanger);
        clientOverlay.getLayer(RuneMenu.class).setRuneData(runes, runeSelectedListener);
        clientOverlay.getLayer(ChampionSuggestions.class)
                .setSuggestedChampions(suggestedChampions, bannedChampions, suggestedChampionSelectedListener);
        clientOverlay.setSceneType(type);
        win.setContentPane(clientOverlay);
        win.setAlwaysOnTop(true);
        win.setAutoRequestFocus(false);
        win.setFocusable(false);
        win.pack();
        win.setSize((int) (rect.width + (Constants.CHAMPION_SUGGESTION_WIDTH * rect.height)), rect.height);
        win.setBackground(new Color(0f, 0f, 0f, 0f));
        clientOverlay.setSize(rect.width, rect.height);
        trackPosition(rect);
        win.setVisible(true);
        win.setOpacity(1f);
        clientOverlay.addMouseMotionListener(clientOverlay);
        clientOverlay.addMouseListener(clientOverlay);
        clientOverlay.addMouseWheelListener(clientOverlay);
    }

    private void trackPosition(Rectangle rect) {
        win.setLocation(rect.x, rect.y);
    }

    /**
     * Starts GUI thread for button
     */
    private void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        try {
            //Create icon in system tray and right click menu
            SystemTray systemTray = SystemTray.getSystemTray();
            //Create icon in tray
            Image image =
                    ImageIO.read(GuiHandler.class.getResourceAsStream("/images/runechanger-runeforge-icon-32x32.png"));
            //Create tray menu
            PopupMenu trayPopupMenu = new PopupMenu();
            MenuItem action = new MenuItem("RuneChanger v" + Constants.VERSION_STRING);
            action.setEnabled(false);
            trayPopupMenu.add(action);

            MenuItem settings = new MenuItem(resourceBundle.getString("show_gui"));
            settings.addActionListener(e -> Settings.show());
            trayPopupMenu.add(settings);

            MenuItem close = new MenuItem(resourceBundle.getString("exit"));
            close.addActionListener(e -> {
                closeWindow();
                System.exit(0);
            });
            trayPopupMenu.add(close);

            trayIcon = new TrayIcon(image, "RuneChanger", trayPopupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Settings.toggle());
            systemTray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Internal method, that starts the thread, which adjusts overlay to client window
     */
    private void startThread() {
        if (threadRunning.get()) {
            return;
        }
        new Thread(() -> {
            threadRunning.set(true);
            while (windowOpen.get()) {
                //if window is open set it's position or hide if client is not active window
                lock.lock();
                WinDef.HWND top = User32Extended.INSTANCE.GetForegroundWindow();
                WinDef.RECT rect = new WinDef.RECT();
                User32Extended.INSTANCE.GetWindowRect(top, rect);
                boolean isClientWindow = NativeUtils.isLeagueOfLegendsClientWindow(top);
                if (isClientWindow) {
                    screenCheckCounter++;
                    if (screenCheckCounter >= 10) {
                        screenCheckCounter = 0;
                        if (getSceneType() == SceneType.CHAMPION_SELECT ||
                                getSceneType() == SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT) {
                            if (ScreenPointChecker.testScreenPoint(top, ScreenPointChecker.CHAMPION_SELECTION_RUNE_PAGE_EDIT)) {
                                setSceneType(SceneType.CHAMPION_SELECT_RUNE_PAGE_EDIT);
                            }
                            else {
                                setSceneType(SceneType.CHAMPION_SELECT);
                            }
                        }
                    }
                }
                if (win != null) {
                    try {
                        //apparently if left is -32000 then window is minimized
                        if (rect.left != MINIMIZED_POSITION && top != null && hwnd != null &&
                                top.getPointer().equals(hwnd.getPointer()) && !win.isVisible()) {
                            win.setVisible(true);
                        }
                        else {
                            //If top window is not named League of Legends, then hide overlay. Makes funny results when opening folder named League of Legends
                            if (isClientWindow && !win.isVisible()) {
                                win.setVisible(true);
                                hwnd = top;
                            }
                            else if (!isClientWindow) {
                                win.setVisible(false);
                            }
                        }
                        Rectangle rect1 = rect.toRectangle();
                        if (rect1 != null) {
                            trackPosition(rect1);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                else {
                    if (rect.left != MINIMIZED_POSITION && isClientWindow) {
                        showWindow(rect.toRectangle());
                    }
                }
                lock.unlock();
                try {
                    //60FPS master race
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            threadRunning.set(false);
        }).start();
    }

    /**
     * Searches for League of Legends window and if found, creates a window for it
     */
    private void startWindow() {
        //firstly get client window
        final User32 user32 = User32.INSTANCE;
        user32.EnumWindows((hWnd, arg1) -> {
            boolean isClientWindow = NativeUtils.isLeagueOfLegendsClientWindow(hWnd);

            if (!isClientWindow) {
                return true;
            }
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(hWnd, rect);
            if (rect.top == 0 && rect.left == 0) {
                return true;
            }
            //ignore minimized windows
            if (rect.left == MINIMIZED_POSITION) {
                return true;
            }
            hwnd = hWnd;
            showWindow(rect.toRectangle());
            return true;
        }, null);
    }

    public void setSuggestedChampions(ArrayList<Champion> lastChampions,
                                      ArrayList<Champion> bannedChampions, Consumer<Champion> suggestedChampionSelectedListener) {
        this.suggestedChampions = lastChampions;
        this.suggestedChampionSelectedListener = suggestedChampionSelectedListener;
        this.bannedChampions = bannedChampions;
        if (clientOverlay != null) {
            clientOverlay.getLayer(ChampionSuggestions.class)
                    .setSuggestedChampions(lastChampions, bannedChampions, suggestedChampionSelectedListener);
        }
    }

    /**
     * Extended User32 library with GetForegroundWindow method
     */
    public interface User32Extended extends User32 {
        User32Extended INSTANCE = Native.loadLibrary("user32", User32Extended.class);

        HWND GetForegroundWindow();

    }
}
