module com.bhanuka.chatapplication {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.bhanuka.chatapplication.client to javafx.fxml;
    opens com.bhanuka.chatapplication.server to javafx.fxml;
    exports com.bhanuka.chatapplication;
}