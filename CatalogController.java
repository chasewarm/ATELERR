package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.Model;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CatalogController implements Initializable {

    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private Button basicCollectionBtn;
    @FXML private Button limitedCollectionBtn;
    @FXML private Button creatorCollectionBtn;
    @FXML private Button allCollectionBtn;
    @FXML private GridPane modelsGrid;

    private List<Model> allModels = new ArrayList<>();
    private String currentFilter = "all";

    // фиксированные цвета для лимитированных моделей
    private static final String[] LIMITED_COLORS = {
            "черный",     // для Худи EXE GXNG
            "графит"      // для Свитшот DJ SPLASH
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActions();
        loadModelsFromDatabase();
    }

    private void setupActions() {
        backButton.setOnAction(event -> SceneManager.getInstance().showMainMenu());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterModels();
        });

        basicCollectionBtn.setOnAction(event -> {
            currentFilter = "basic";
            filterModels();
        });

        limitedCollectionBtn.setOnAction(event -> {
            currentFilter = "limited";
            filterModels();
        });

        creatorCollectionBtn.setOnAction(event -> {
            currentFilter = "creator";
            filterModels();
        });

        allCollectionBtn.setOnAction(event -> {
            currentFilter = "all";
            displayModels(allModels);
        });
    }
    private void loadModelsFromDatabase() {
        try {
            allModels = SupabaseClient.getInstance().getAllModels();
            displayModels(allModels);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Ошибка загрузки данных", "Не удалось загрузить каталог моделей");
        }
    }
    private void filterModels() {
        String searchText = searchField.getText().toLowerCase().trim();

        List<Model> filtered = allModels.stream()
                .filter(model -> {
                    if (!currentFilter.equals("all") && !model.getCollection().equals(currentFilter)) {
                        return false;
                    }
                    if (!searchText.isEmpty()) {
                        return model.getName().toLowerCase().contains(searchText) ||
                                model.getDescription().toLowerCase().contains(searchText);
                    }
                    return true;
                })
                .toList();

        displayModels(filtered);
    }
    private void displayModels(List<Model> models) {
        modelsGrid.getChildren().clear();
        int row = 0;
        int col = 0;

        for (Model model : models) {
            VBox card = createModelCard(model);
            modelsGrid.add(card, col, row);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }
    private VBox createModelCard(Model model) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #BEB5A9; -fx-background-radius: 10; -fx-padding: 15;");
        card.setPrefWidth(200);

        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(180);
        imageView.setPreserveRatio(true);

        try {
            if (model.getImagePath() != null && !model.getImagePath().isEmpty()) {
                Image image = new Image(model.getImagePath(), true);
                imageView.setImage(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Label nameLabel = new Label(model.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #291C0E;");
        Label descLabel = new Label(model.getDescription());
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #6E473B; -fx-font-size: 12;");
        Label priceLabel = new Label(String.format("%,d ₽", (int)model.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #291C0E; -fx-font-size: 16;");
        Label stockLabel = new Label("В наличии: " + model.getInStock());
        stockLabel.setStyle("-fx-text-fill: #6E473B; -fx-font-size: 11;");

        //добавление информацию о фиксированном цвете для лимитированных моделей
        if ("limited".equals(model.getCollection())) {
            Label fixedColorLabel = new Label("Цвет: фиксированный");
            fixedColorLabel.setStyle("-fx-text-fill: #6E473B; -fx-font-size: 11; -fx-font-style: italic;");
            card.getChildren().add(fixedColorLabel);
        }

        Button detailsButton = new Button("Подробнее");
        detailsButton.setStyle("-fx-background-color: #6E473B; -fx-text-fill: #E1D4C2;");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/orderDetail.fxml"));
                Parent root = loader.load();

                OrderDetailController controller = loader.getController();
                controller.setModel(model);

                Scene scene = new Scene(root);
                Stage stage = (Stage) detailsButton.getScene().getWindow();
                stage.setScene(scene);
            } catch (IOException e) {
                e.printStackTrace();
                showErrorAlert("Ошибка", "Не удалось открыть форму заказа");
            }
        });

        if (imageView.getImage() != null) {
            card.getChildren().addAll(imageView, nameLabel, descLabel, priceLabel, stockLabel, detailsButton);
        } else {
            Label placeholder = new Label("🖼️");
            placeholder.setStyle("-fx-font-size: 48; -fx-alignment: center; -fx-background-color: #A78D78; -fx-background-radius: 5;");
            placeholder.setPrefHeight(150);
            placeholder.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().addAll(placeholder, nameLabel, descLabel, priceLabel, stockLabel, detailsButton);
        }
        return card;
    }
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}