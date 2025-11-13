package com.travel.frontend;

import com.travel.frontend.ui.Navigator;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialize a single stage + global stylesheet, then go to Login
        Navigator.init(primaryStage);
        Navigator.goLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
