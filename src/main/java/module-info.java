module com.example.soundhub {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens ui to javafx.fxml; // package avec tes contrôleurs FXML
    exports test; // package avec la classe Main
}
