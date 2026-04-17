package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.*;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class OrderDetailController implements Initializable {

    @FXML private Button backButton;
    @FXML private ImageView modelImageView;
    @FXML private Label modelNameLabel;
    @FXML private Label modelDescriptionLabel;
    @FXML private Label basePriceLabel;
    @FXML private ComboBox<String> colorComboBox;
    @FXML private ComboBox<String> sizeComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Label estimatedPriceLabel;
    @FXML private Button submitOrderButton;
    @FXML private Label statusLabel;
    @FXML private Label colorLabel;

    private Model currentModel;

    //фиксированные цвета для лимитированных моделей
    private static final String[] LIMITED_COLORS = {
            "черный",     // для Худи EXE GXNG
            "графит"      // для Свитшот DJ SPLASH
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActions();
        initializeSizeComboBox();
        initializeColorComboBox();
    }
    public void setModel(Model model) {
        this.currentModel = model;
        loadModelData();
        setupColorSelection();
    }
    private void setupActions() {
        backButton.setOnAction(event -> SceneManager.getInstance().showCatalog());
        colorComboBox.setOnAction(event -> updateEstimatedPrice());
        sizeComboBox.setOnAction(event -> updateEstimatedPrice());
        submitOrderButton.setOnAction(event -> handleSubmitOrder());
    }
    private void initializeSizeComboBox() {
        sizeComboBox.getItems().addAll("XS (42-44)", "S (46-48)", "M (50-52)", "L (54-56)", "XL (58-60)", "XXL (62-64)");
        sizeComboBox.setPromptText("Выберите размер");
    }
    private void initializeColorComboBox() {
        colorComboBox.getItems().addAll("Черный", "Белый", "Серый", "Графит", "Бежевый", "Темно-синий", "Коричневый");
        colorComboBox.setPromptText("Выберите цвет");
    }
    private void setupColorSelection() {
        if (currentModel != null) {
            //проверяем, лимитированная ли модель
            if ("limited".equals(currentModel.getCollection())) {
                //для лимиток цвет выбран
                colorComboBox.setDisable(true);
                String fixedColor;
                if (currentModel.getId() == 4) {
                    fixedColor = "Черный";
                } else if (currentModel.getId() == 5) {
                    fixedColor = "Графит";
                } else {
                    fixedColor = "Фиксированный";
                }
                colorComboBox.setPromptText("Цвет: " + fixedColor);
                colorComboBox.setValue(fixedColor);
                Label fixedColorNote = new Label("(цвет фиксированный для данной модели)");
                fixedColorNote.setStyle("-fx-text-fill: #6E473B; -fx-font-size: 11; -fx-font-style: italic;");
            } else {
                colorComboBox.setDisable(false);
                colorComboBox.setPromptText("Выберите цвет");
                colorComboBox.setValue(null);
            }
        }
    }
    private void loadModelData() {
        if (currentModel != null) {
            modelNameLabel.setText(currentModel.getName());
            modelDescriptionLabel.setText(currentModel.getDescription());
            basePriceLabel.setText(String.format("Базовая цена: %,d ₽", (int) currentModel.getPrice()));
            try {
                if (currentModel.getImagePath() != null && !currentModel.getImagePath().isEmpty()) {
                    Image image = new Image(currentModel.getImagePath(), true);
                    modelImageView.setImage(image);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void updateEstimatedPrice() {
        double price = currentModel.getPrice();
        // добавляем наценку за индивидуальный пошив
        price += 0;

        estimatedPriceLabel.setText(String.format("%,d ₽", (int) price));
    }
    private void handleSubmitOrder() {
        if (!UserSession.getInstance().isLoggedIn()) {
            statusLabel.setText("Пожалуйста, войдите в систему");
            SceneManager.getInstance().showLoginScreen();
            return;}
        // для лимитированных моделей цвет уже выбран автоматически
        if (!"limited".equals(currentModel.getCollection())) {
            String selectedColor = colorComboBox.getValue();
            if (selectedColor == null || selectedColor.isEmpty()) {
                statusLabel.setText("Пожалуйста, выберите цвет");
                return;
            }
        }
        String selectedSize = sizeComboBox.getValue();
        if (selectedSize == null || selectedSize.isEmpty()) {
            statusLabel.setText("Пожалуйста, выберите размер");
            return;}
        try {
            Order order = new Order();
            User currentUser = UserSession.getInstance().getCurrentUser();
            order.setUserFirstName(currentUser.getFirstName());
            order.setUserLastName(currentUser.getLastName());
            order.setUserPhone(currentUser.getPhone());
            order.setUserId(UserSession.getInstance().getCurrentUser().getId());
            order.setModelId(currentModel.getId());
            order.setOrderType("individual");
            order.setStatus("new");
            order.setSize(selectedSize);
            // устанавливаем цвет в зависимости от типа модели
            if ("limited".equals(currentModel.getCollection())) {
                if (currentModel.getId() == 4) {
                    order.setColor("Черный");
                } else if (currentModel.getId() == 5) {
                    order.setColor("Графит");
                } else {
                    order.setColor("Фиксированный");
                }
            } else {
                order.setColor(colorComboBox.getValue());}

            order.setQuantity(1);
            // формируем описание заказа
            StringBuilder fullDescription = new StringBuilder();
            fullDescription.append("Заказ модели: ").append(currentModel.getName()).append("\n");
            fullDescription.append("Цвет: ").append(order.getColor()).append("\n");
            fullDescription.append("Размер: ").append(selectedSize).append("\n");

            // ДОБАВЛЕНО: включение дополнительных пожеланий в описание заказа
            if (descriptionArea.getText() != null && !descriptionArea.getText().isEmpty()) {
                fullDescription.append("Пожелания: ").append(descriptionArea.getText());
            }
            order.setDescription(fullDescription.toString());
            order.setCreatedAt(LocalDateTime.now());
            Order createdOrder = SupabaseClient.getInstance().createOrder(order);

            if (createdOrder != null) {
                showAlert("Успешно", "Заказ успешно оформлен! " +
                        "Наш администратор свяжется с вами для уточнения деталей.");
                SceneManager.getInstance().showCatalog();
            } else {
                statusLabel.setText("Ошибка при оформлении заказа");}
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка подключения к серверу: " + e.getMessage());}
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}