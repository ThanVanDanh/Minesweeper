module vn.edu.nlu.fit.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;
    requires jakarta.mail;

    requires org.controlsfx.controls;
    requires javafx.media;

    opens app to javafx.fxml;
    opens minesweeper.controller to javafx.fxml;
    opens minesweeper.model      to javafx.base;

    exports app;
    exports minesweeper.controller;
    exports minesweeper.model;
    exports minesweeper.repository;
    exports minesweeper.dto;
    exports minesweeper.service;
    exports utils;
    exports minesweeper.model.enums;
    opens minesweeper.model.enums to javafx.base;
}