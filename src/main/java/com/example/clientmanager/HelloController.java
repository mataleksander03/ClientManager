package com.example.clientmanager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloController {
    public Button ordersButton;
    public Button exitButton;
    @FXML
    private Label welcomeText;

    public void openOrdersWindow() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("orders-view.fxml"));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("Zamówienia");
        stage.show();
    }

    public void onOrderPanelAction(ActionEvent actionEvent) throws IOException {
        openOrdersWindow();
        Stage stage = (Stage) ordersButton.getScene().getWindow();
        stage.close();
    }

    public void onExitAction(ActionEvent actionEvent) {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }
}