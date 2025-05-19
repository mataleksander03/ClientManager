module com.example.clientmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires net.datafaker;


    opens com.example.clientmanager to javafx.fxml;
    exports com.example.clientmanager;
}