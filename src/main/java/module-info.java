module vn.edu.nlu.fit.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens app to javafx.fxml;
    opens minesweeper.controller to javafx.fxml;
    opens minesweeper.model      to javafx.base;

    exports app;
    exports minesweeper.controller;
    exports minesweeper.model;
}