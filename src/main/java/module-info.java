module vn.edu.nlu.fit.minesweeper {
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

    opens vn.edu.nlu.fit.minesweeper to javafx.fxml;
    exports vn.edu.nlu.fit.minesweeper;
}