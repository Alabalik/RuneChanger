package com.stirante.runechanger;

import ch.qos.logback.classic.Level;
import com.stirante.runechanger.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DebugConsts {
    //Used for testing the UI
    public static final boolean MOCK_SESSION = false;
    public static final boolean DISPLAY_FAKE = false;

    //those are really annoying while testing
    public static final boolean DISABLE_NOTIFICATIONS = false;
    public static final boolean DISABLE_AUTOUPDATE = false;

    //logs every event (except for voice) to System.out
    public static final boolean PRINT_EVENTS = false;

    //set to lang code for overriding language
    public static final String OVERRIDE_LANGUAGE = null;

    /**
     * If instead of a JAR we have a folder, it is very likely, that RuneChanger is running from an IDE
     */
    public static boolean isRunningFromIDE() {
        return new File(PathUtils.getJarLocation()).isDirectory();
    }

    public static void enableDebugMode() {
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.getAppender("STDOUT").clearAllFilters();
        logger.setLevel(Level.DEBUG);
    }

}
