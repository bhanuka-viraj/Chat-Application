package com.bhanuka.chatapplication.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class ChatClient {
    @FXML
    private ListView<Object> messageView;

    @FXML
    private Button btnSend;

    @FXML
    private TextField txtMessage;

    @FXML
    private Button btnImage;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private boolean nameAccepted = false;

    public void initialize(){
        messageView.setCellFactory(listView -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String) {
                    setText((String) item);
                    setGraphic(null);
                } else if (item instanceof Image) {
                    Image image = (Image) item;
                    ImageView imageView = new ImageView(image);
                    imageView.setFitHeight(100);
                    imageView.setFitWidth(100);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);
                    imageView.setCache(true);

                    setGraphic(imageView);
                    setText(null);
                }
            }
        });


        try {
            Socket socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(() -> listenForMessages());
            thread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void promptForName(){
        Platform.runLater(()->{
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your name");
            dialog.setHeaderText("Please enter your name");

            dialog.showAndWait().ifPresent(name ->{
                clientName = name.trim();
                if (clientName.isEmpty()){
                    messageView.getItems().add("Name cannot be empty, Please try again");
                    promptForName();
                }else {
                    try {
                        out.writeObject(clientName);
                        out.flush();
                        dialog.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageView.getItems().add("Error sending name, Please try again");
                    }
                }
            });

            if (dialog.getResult().isEmpty()){
                Platform.exit();
            }
        });
    }

    private void listenForMessages() {
       try {
            while(true){
                Object message = in.readObject();
                if (message == null) break;
                if (message instanceof String){
                    String text = (String) message;
                    if (text.startsWith("SUBMITNAME")){
                        if (!nameAccepted){
                            promptForName();
                        }
                    }else if (text.startsWith("NAMEACCEPTED")){
                        nameAccepted = true;
                        Platform.runLater(() -> messageView.getItems().add("Connected as " + clientName));
                    }else if (text.startsWith("TEXT")){
                        Platform.runLater(() ->{
                            if (text.startsWith("TEXT " + clientName + ": ")){
                                messageView.getItems().add("You: " + text.substring(clientName.length()+2+5));
                            }else {
                                messageView.getItems().add(text.substring(5));
                            }
                        });
                    } else if (text.startsWith("IMAGE")) {
                        byte[] imageData = (byte[]) in.readObject();
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        Platform.runLater(() -> {
                            messageView.getItems().add(text.substring(6) + " sent an image");
                            messageView.getItems().add(image);
                        });
                    }
                }
            }
       } catch (IOException | ClassNotFoundException e) {
           Platform.runLater(() -> messageView.getItems().add("Disconnected" + e.getMessage()));
       }finally {
           closeConnection();
       }
    }

     public void closeConnection() {
        try {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
        @FXML
    void btnSendOnAction(ActionEvent event) {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;
        try {
            out.writeObject(message);
            out.flush();
            txtMessage.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void btnImageOnAction(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {

            try {
                byte [] imageData = Files.readAllBytes(file.toPath());
                out.writeObject(imageData);
                out.flush();
            } catch (IOException e) {
                Platform.runLater(() -> messageView.getItems().add("Error sending image, Please try again"));
            }
        }
    }
}
