package minesweeper.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class BoardGameController implements Initializable {

    @FXML
    private GridPane minesweeperGrid;

    private int rows = 10;
    private int cols = 10;
    private final int BUTTON_SIZE = 35;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createBoard();
    }
    private void createBoard() {
        minesweeperGrid.getChildren().clear();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Button cell = new Button();
                cell.setPrefSize(BUTTON_SIZE, BUTTON_SIZE);

                cell.getStyleClass().add("mine-cell-covered");

                minesweeperGrid.add(cell, c, r);
            }
        }
    }
    @FXML
    public void btn(ActionEvent actionEvent) {
        createBoard();
    }
}