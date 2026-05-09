module vn.edu.nlu.fit.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.jfoenix;
    requires javafx.web;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    requires org.controlsfx.controls;
    requires javafx.media;

    opens app to javafx.fxml;
    opens minesweeper.controller to javafx.fxml;
    opens minesweeper.dto to javafx.base;
    opens minesweeper.model      to javafx.base;

    exports app;
    exports minesweeper.controller;
    exports minesweeper.model;
    exports minesweeper.repository;
    exports minesweeper.dto;
    exports minesweeper.service;
}