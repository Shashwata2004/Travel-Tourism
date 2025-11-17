/* Boots up the desktop app, prepares the shared JavaFX window, and shows the
   login screen that lets every other view load afterward. Acts as the single
   entry point people double-click when launching the project. */
package com.travel.frontend;

import com.travel.frontend.ui.Navigator;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    /* JavaFX lifecycle hook: hands us the Stage so we can set up shared styling
       and pass control to the Navigator. Uses the standard Application.start
       contract so FX knows where to begin. */
    @Override
    public void start(Stage primaryStage) {
        // Initialize a single stage + global stylesheet, then go to Login
        Navigator.init(primaryStage);
        Navigator.goLogin();
    }

    /* Regular Java entry point that defers to Application.launch so JavaFX can
       spin up its toolkit threads and call start(...) when ready. */
    public static void main(String[] args) {
        launch(args);
    }
}
