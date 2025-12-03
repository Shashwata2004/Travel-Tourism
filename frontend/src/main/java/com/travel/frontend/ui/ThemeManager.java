package com.travel.frontend.ui;

import javafx.scene.Scene;
import javafx.scene.Parent;

import java.util.List;

/**
 * Simple global theme switcher. Keeps track of the current theme and
 * applies/removes the dark stylesheet on any scene you hand it.
 */
public final class ThemeManager {
    public enum Theme { LIGHT, DARK }

    private static final String DARK_CSS = "/css/theme-dark.css";
    private static Theme current = Theme.LIGHT;

    private ThemeManager() {}

    public static void setTheme(Theme t) {
        current = t == null ? Theme.LIGHT : t;
    }

    public static Theme getTheme() {
        return current;
    }

    public static boolean isDark() {
        return current == Theme.DARK;
    }

    /** Attach or remove the dark stylesheet on the given scene. */
    public static void apply(Scene scene) {
        if (scene == null) return;
        List<String> sheets = scene.getStylesheets();
        sheets.removeIf(s -> s.endsWith("theme-dark.css"));
        Parent root = scene.getRoot();
        if (root != null) {
            root.getStylesheets().removeIf(s -> s.endsWith("theme-dark.css"));
        }
        if (root != null) {
            root.getStyleClass().remove("dark-theme");
        }
        if (current == Theme.DARK) {
            var url = ThemeManager.class.getResource(DARK_CSS);
            if (url != null) {
                String css = url.toExternalForm();
                if (sheets.stream().noneMatch(s -> s.endsWith("theme-dark.css"))) {
                    sheets.add(css); // scene-level
                }
                if (root != null) {
                    if (root.getStylesheets().stream().noneMatch(s -> s.endsWith("theme-dark.css"))) {
                        root.getStylesheets().add(css); // ensure node-level precedence
                    }
                    if (!root.getStyleClass().contains("dark-theme")) {
                        root.getStyleClass().add("dark-theme");
                    }
                }
                System.out.println("[ThemeManager] Applied dark theme to scene; stylesheets now: " + sheets);
            } else {
                System.out.println("[ThemeManager] Could not find theme-dark.css on classpath");
            }
        } else {
            System.out.println("[ThemeManager] Using light theme; dark sheet removed");
        }
    }
}
