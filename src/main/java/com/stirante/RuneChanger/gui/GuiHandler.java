package com.stirante.RuneChanger.gui;

import com.stirante.RuneChanger.model.RunePage;
import com.stirante.RuneChanger.util.LangHelper;
import com.stirante.RuneChanger.util.RuneSelectedListener;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuiHandler {
    private JWindow win;
    private RuneButton canvas;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean windowOpen = new AtomicBoolean(false);
    private final AtomicBoolean openCommand = new AtomicBoolean(false);
    private final AtomicBoolean closeCommand = new AtomicBoolean(false);
    private final List<RunePage> runes = Collections.synchronizedList(new ArrayList<>());
    private WinDef.HWND hwnd;
    private RuneSelectedListener runeSelectedListener;
    private final ResourceBundle resourceBundle = LangHelper.getLang();

    public GuiHandler() {
        handleWindowThread();
    }

    /**
     * Is window running
     *
     * @return is running
     */
    public boolean isRunning() {
        return running.get();
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
     * Tries to close window
     */
    public void tryClose() {
        closeCommand.set(true);
    }

    public void setRunes(List<RunePage> runeList, RuneSelectedListener onClickListener) {
        runes.clear();
        runes.addAll(runeList);
        runeSelectedListener = onClickListener;
        if (canvas != null) {
            canvas.setRuneData(runes, onClickListener);
        }
    }

    /**
     * Actually create and show our button
     *
     * @param rect client window bounds
     */
    private void showWindow(Rectangle rect) {
        if (win != null) {
            win.dispose();
        }
        win = new JWindow();
        canvas = new RuneButton();
        canvas.setRuneData(runes, runeSelectedListener);
        win.setContentPane(canvas);
        win.setAlwaysOnTop(true);
        win.setAutoRequestFocus(false);
        win.setFocusable(false);
        win.pack();
        win.setSize(rect.width, rect.height);
        win.setBackground(new Color(0f, 0f, 0f, 0f));
        canvas.setSize(rect.width, rect.height);
        trackPosition(rect);
        win.setVisible(true);
        win.setOpacity(1f);
        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        canvas.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                canvas.mouseMoved(e);
            }
        });
        canvas.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                canvas.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                canvas.mouseExited(e);
            }
        });
    }

    private void trackPosition(Rectangle rect) {
        win.setLocation(rect.x, rect.y);
    }

    /**
     * Starts GUI thread for button
     */
    private void handleWindowThread() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        try {
            //Create icon in system tray and right click menu
            SystemTray systemTray = SystemTray.getSystemTray();
            //this actually don't work
            Image image =
                    ImageIO.read(GuiHandler.class.getResourceAsStream("/images/runechanger-runeforge-icon-32x32.png"));
            PopupMenu trayPopupMenu = new PopupMenu();
            MenuItem action = new MenuItem("RuneChanger v" + Constants.VERSION_STRING);
            action.setEnabled(false);
            trayPopupMenu.add(action);
            action = new MenuItem("By stirante");
            action.setEnabled(true);
            action.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(new URI("http://stirante.com"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            });
            trayPopupMenu.add(action);

            MenuItem settings = new MenuItem(resourceBundle.getString("settings"));
            settings.addActionListener(e -> Settings.show());
            trayPopupMenu.add(settings);

            MenuItem close = new MenuItem(resourceBundle.getString("exit"));
            close.addActionListener(e -> {
                running.set(false);
                tryClose();
                System.exit(0);
            });
            trayPopupMenu.add(close);

            TrayIcon trayIcon = new TrayIcon(image, "RuneChanger", trayPopupMenu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Settings.toggle());
            systemTray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        new Thread(() -> {
            while (running.get()) {
                //command to open window
                if (openCommand.get()) {
                    //don't open window, when it's already open
                    if (!windowOpen.get()) {
                        startWindow();
                        openCommand.set(false);
                        windowOpen.set(true);
                    }
                }
                //command to close window
                if (closeCommand.get()) {
                    if (win != null) {
                        win.dispose();
                        win = null;
                    }
                    windowOpen.set(false);
                    openCommand.set(false);
                    closeCommand.set(false);
                }
                //if window is open set it's position or hide if client is not active window
                if (windowOpen.get()) {
                    WinDef.HWND top = User32Extended.INSTANCE.GetForegroundWindow();
                    WinDef.RECT rect = new WinDef.RECT();
                    User32Extended.INSTANCE.GetWindowRect(top, rect);
                    if (win != null) {
                        try {
                            //apparently if left is -32000 then window is minimized
                            if (rect.left != -32000 && top != null && hwnd != null &&
                                    top.getPointer().equals(hwnd.getPointer())) {
                                win.setVisible(true);
                            }
                            else {
                                char[] windowText = new char[512];
                                User32.INSTANCE.GetWindowText(top, windowText, 512);
                                String wText = Native.toString(windowText);
                                if (wText.equalsIgnoreCase("League of Legends")) {
                                    win.setVisible(true);
                                    hwnd = top;
                                }
                                else {
                                    win.setVisible(false);
                                }
                            }
                            Rectangle rect1 = rect.toRectangle();
                            if (rect1 != null) {
                                trackPosition(rect1);
                            }
                        } catch (Throwable t) {
                            //sometimes 'win' becomes null async, so this code throws NullPointerException
                            t.printStackTrace();
                        }
                    }
                }
                try {
                    //60FPS master race
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Opens window
     */
    private void startWindow() {
        //firstly get client window
        final User32 user32 = User32.INSTANCE;
        user32.EnumWindows((hWnd, arg1) -> {
            char[] windowText = new char[512];
            user32.GetWindowText(hWnd, windowText, 512);
            String wText = Native.toString(windowText);

            if (wText.isEmpty() || !wText.equalsIgnoreCase("League of Legends")) {
                return true;
            }
            WinDef.RECT rect = new WinDef.RECT();
            user32.GetWindowRect(hWnd, rect);
            if (rect.top == 0 && rect.left == 0) {
                return true;
            }
            //ignore minimized windows
            if (rect.left == -32000) {
                return true;
            }
            hwnd = hWnd;
            showWindow(rect.toRectangle());
            return true;
        }, null);
    }

    /**
     * Opens window
     */
    public void openWindow() {
        openCommand.set(true);
    }

    /**
     * Extended User32 library with GetForegroundWindow method
     */
    public interface User32Extended extends User32 {
        User32Extended INSTANCE = Native.loadLibrary("user32", User32Extended.class);

        HWND GetForegroundWindow();

    }
}
