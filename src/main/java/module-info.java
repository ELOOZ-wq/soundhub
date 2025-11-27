module com.example.soundhub {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires transitive javafx.graphics;
    requires transitive java.sql;
    requires jbcrypt;

    exports ui;
    exports ui.views;
    exports ui.components;
    exports model;
    exports service;
    exports utils;
    exports dao;
}
