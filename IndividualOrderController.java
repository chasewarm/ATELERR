package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.Order;
import com.gibsie.atelier.model.User;
import com.gibsie.atelier.model.UserSession;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class IndividualOrderController implements Initializable {

    @FXML private Button backButton;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private Button uploadSketchButton;
    @FXML private Label fileNameLabel;
    @FXML private ComboBox<String> fabricComboBox;
    @FXML private TextField sizeField;
    @FXML private Button submitOrderButton;
    @FXML private Label statusLabel;

    private File selectedSketch;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActions();
        initializeComboBoxes();
        loadFabricsFromDatabase();
    }

    private void setupActions() {
        backButton.setOnAction(event -> SceneManager.getInstance().showMainMenu());
        uploadSketchButton.setOnAction(event -> handleUploadSketch());
        submitOrderButton.setOnAction(event -> handleSubmitOrder());
    }

    private void initializeComboBoxes() {
        typeComboBox.getItems().addAll("Худи", "Толстовка", "Свитшот", "Футболка", "Брюки",
                "Платье", "Пальто", "Костюм","Свой вариант, опишу далее");
    }
    private void loadFabricsFromDatabase() {
        try {
            var fabrics = SupabaseClient.getInstance().getAllFabrics();
            for (var fabric : fabrics) {
                fabricComboBox.getItems().add(fabric.getName() + " (" + fabric.getColor() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // если не удалось загрузить, используем стандартные
            fabricComboBox.getItems().addAll("Коттон", "Футер 2-нитка", "Футер 3-нитка", "Кашкорсе", "Велюр",
                    "Трикотаж", "Шерсть", "Лен", "Шелк", "Вельвет", "Джинса тонкая","Джинса толстая");
        }
    }
    private void handleUploadSketch() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите эскиз");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        selectedSketch = fileChooser.showOpenDialog(uploadSketchButton.getScene().getWindow());

        if (selectedSketch != null) {
            fileNameLabel.setText(selectedSketch.getName());
        }
    }
    private void handleSubmitOrder() {
        // проверка авторизации
        if (!UserSession.getInstance().isLoggedIn()) {
            statusLabel.setText("Пожалуйста, войдите в систему");
            SceneManager.getInstance().showLoginScreen();
            return;
        }

        if (typeComboBox.getValue() == null) {
            statusLabel.setText("Пожалуйста, выберите тип изделия");
            return;
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            statusLabel.setText("Пожалуйста, опишите вашу идею");
            return;
        }
        try {
            Order order = new Order();
            User currentUser = UserSession.getInstance().getCurrentUser();
            order.setUserId(currentUser.getId());
            order.setUserFirstName(currentUser.getFirstName());
            order.setUserLastName(currentUser.getLastName());
            order.setUserPhone(currentUser.getPhone());
            order.setUserId(UserSession.getInstance().getCurrentUser().getId());
            order.setOrderType("individual");
            order.setStatus("new");
            order.setDescription(descriptionArea.getText().trim());
            order.setFabric(fabricComboBox.getValue());
            order.setSize(sizeField.getText().trim());
            if (selectedSketch != null) {
                order.setSketchPath(selectedSketch.getAbsolutePath());
            }
            order.setQuantity(1);
            order.setCreatedAt(LocalDateTime.now());

            Order createdOrder = SupabaseClient.getInstance().createOrder(order);
            if (createdOrder != null) {
                statusLabel.setText("Заказ успешно отправлен! Мы свяжемся с вами в ближайшее время.");
                clearForm();
            } else {
                statusLabel.setText("Ошибка при отправке заказа");
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка подключения к серверу");
        }
    }
    private void clearForm() {
        typeComboBox.setValue(null);
        descriptionArea.clear();
        fileNameLabel.setText("Файл не выбран");
        selectedSketch = null;
        fabricComboBox.setValue(null);
        sizeField.clear();
    }
}