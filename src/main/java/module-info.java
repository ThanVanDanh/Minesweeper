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
    requires jbcrypt;

    opens app to javafx.fxml;
    opens minesweeper.controller to javafx.fxml;
    opens minesweeper.model      to javafx.base;

    exports app;
    exports minesweeper.controller;
    exports minesweeper.model;
    exports minesweeper.repository;
    exports minesweeper.repository.connection;
    exports minesweeper.repository.log;
    exports minesweeper.repository.config;
    exports minesweeper.repository.exception;
    exports minesweeper.repository.pagination;
    exports minesweeper.repository.spec;
    exports minesweeper.dto;
    exports minesweeper.service;
    exports utils;
    exports minesweeper.model.enums;
    opens minesweeper.model.enums to javafx.base;
}