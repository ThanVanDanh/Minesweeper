module vn.edu.nlu.fit.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.slf4j;

    requires org.controlsfx.controls;

    opens app to javafx.fxml;
    opens minesweeper.controller to javafx.fxml;

    exports app;
    exports minesweeper.controller;
    exports minesweeper.model;
    exports minesweeper.repository;
    exports minesweeper.dto;
    exports minesweeper.service;
}