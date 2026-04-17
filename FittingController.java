package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.*;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.DateCell;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FittingController implements Initializable {

    @FXML private Button backButton;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private RadioButton measurementsRadio;
    @FXML private RadioButton fittingRadio;
    @FXML private ToggleGroup consultationTypeGroup;
    @FXML private TextArea notesArea;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;

    @FXML private ComboBox<String> productToTryComboBox;
    @FXML private Label productStatusLabel;

    private List<Fitting> allFittings;
    private List<Order> userOrders;
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActions();
        initializeTimeComboBox();
        setupDateRestrictions();
        loadAllFittings();
        loadUserOrders();
    }

    private void setupActions() {
        backButton.setOnAction(event -> SceneManager.getInstance().showMainMenu());
        submitButton.setOnAction(event -> handleSubmit());
        datePicker.setOnAction(event -> updateAvailableTimes());

        consultationTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateProductSelectionState();
        });
    }

    private void initializeTimeComboBox() {
        timeComboBox.getItems().addAll("10:00", "11:00", "12:00", "13:00", "14:00",
                "15:00", "16:00", "17:00", "18:00", "19:00");
        timeComboBox.setPromptText("Выберите время");
    }

    private void setupDateRestrictions() {
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });
    }

    private void updateProductSelectionState() {
        if (fittingRadio.isSelected()) {
            productToTryComboBox.setDisable(false);
            productStatusLabel.setVisible(true);
        } else {
            productToTryComboBox.setDisable(true);
            productStatusLabel.setVisible(false);
        }
    }

    private void loadUserOrders() {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser == null) {
            productToTryComboBox.getItems().clear();
            productToTryComboBox.getItems().add("— Войдите в систему —");
            productToTryComboBox.setDisable(true);
            productStatusLabel.setText("🔒 Войдите в систему, чтобы выбрать товар");
            productStatusLabel.setVisible(true);
            return;
        }

        try {
            System.out.println("ЗАГРУЗКА ЗАКАЗОВ ДЛЯ ПОЛЬЗОВАТЕЛЯ ID: " + currentUser.getId());
            userOrders = SupabaseClient.getInstance().getOrdersByUserId(currentUser.getId());

            System.out.println("Всего заказов найдено: " + userOrders.size());
            for (Order order : userOrders) {
                System.out.println("  Заказ #" + order.getId() +
                        ", Статус: '" + order.getStatus() +
                        "', Тип: " + order.getOrderType() +
                        ", Model ID: " + order.getModelId());
            }

            //фильтруем заказы со статусом "ready" (Готов)
            List<Order> availableOrders = userOrders.stream()
                    .filter(order -> {
                        String status = order.getStatus();
                        boolean isReady = "ready".equals(status.trim());
                        if (isReady) {
                            System.out.println("  ✅ Заказ #" + order.getId() + " - статус 'ready' - ДОБАВЛЯЕМ в список!");
                        }
                        return isReady;
                    })
                    .collect(Collectors.toList());

            System.out.println("Доступно заказов для примерки (статус 'ready'): " + availableOrders.size());

            productToTryComboBox.getItems().clear();

            if (availableOrders.isEmpty()) {
                productToTryComboBox.getItems().add("— Нет готовых заказов —");
                productToTryComboBox.setValue("— Нет готовых заказов —");
                productToTryComboBox.setDisable(true);
                productStatusLabel.setText("⚠️ У вас пока нет заказов со статусом 'Готов' для примерки");
                productStatusLabel.setStyle("-fx-text-fill: #C62828; -fx-font-size: 12;");
                productStatusLabel.setVisible(true);
            } else {
                for (Order order : availableOrders) {
                    String displayText = buildOrderDisplayText(order);
                    productToTryComboBox.getItems().add(displayText);
                    System.out.println("  📦 Добавлен в выпадающий список: " + displayText);
                }
                productToTryComboBox.setPromptText("Выберите товар для примерки");
                productStatusLabel.setText("✅ Доступно заказов для примерки: " + availableOrders.size());
                productStatusLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 12;");
                productToTryComboBox.setDisable(false);
                productStatusLabel.setVisible(true);
            }
            updateProductSelectionState();
        } catch (Exception e) {
            e.printStackTrace();
            productToTryComboBox.getItems().clear();
            productToTryComboBox.getItems().add("— Ошибка загрузки —");
            productToTryComboBox.setDisable(true);
            productStatusLabel.setText("❌ Ошибка загрузки заказов: " + e.getMessage());
            productStatusLabel.setStyle("-fx-text-fill: #C62828; -fx-font-size: 12;");
            productStatusLabel.setVisible(true);
        }
    }
    private String buildOrderDisplayText(Order order) {
        String orderType = order.getOrderType();
        String size = order.getSize() != null ? order.getSize() : "не указан";

        if ("corporate".equals(orderType)) {
            return "Заказ №" + order.getId() + " — Корпоративный заказ, размер: " + size;
        } else if ("collaboration".equals(orderType)) {
            if (order.getModelId() > 0) {
                try {
                    Model model = SupabaseClient.getInstance().getModelById(order.getModelId());
                    if (model != null) {
                        return "Заказ №" + order.getId() + " — ," + model.getName() + " размер: " + size;
                    }
                } catch (Exception e) {
                }
            }
            return "Заказ №" + order.getId() + " — Коллаборация, размер: " + size;
        } else if ("individual".equals(orderType)) {
            if (order.getModelId() > 0) {
                try {
                    Model model = SupabaseClient.getInstance().getModelById(order.getModelId());
                    if (model != null) {
                        return "Заказ №" + order.getId() + " — " + model.getName() + ", размер: " + size;
                    }
                } catch (Exception e) {
                }
            }
            return "Заказ №" + order.getId() + " — Индивидуальный заказ, размер: " + size;
        }

        return "Заказ №" + order.getId() + ", размер: " + size;
    }

    private void loadAllFittings() {
        try {
            allFittings = SupabaseClient.getInstance().getAllFittings();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось загрузить информацию о записях");
        }
    }

    private void updateAvailableTimes() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) return;

        timeComboBox.getItems().clear();
        timeComboBox.setDisable(true);
        timeComboBox.setPromptText("Проверка доступности...");

        List<String> bookedTimes = allFittings.stream()
                .filter(f -> f.getDate().equals(selectedDate) && !"cancelled".equals(f.getStatus()))
                .map(f -> f.getTime().toString())
                .collect(Collectors.toList());

        List<String> allTimes = List.of("10:00", "11:00", "12:00", "13:00", "14:00",
                "15:00", "16:00", "17:00", "18:00", "19:00");

        for (String time : allTimes) {
            if (!bookedTimes.contains(time)) {
                timeComboBox.getItems().add(time);
            }
        }

        if (timeComboBox.getItems().isEmpty()) {
            timeComboBox.setPromptText("Нет свободного времени");
            timeComboBox.setDisable(true);
        } else {
            timeComboBox.setPromptText("Выберите время");
            timeComboBox.setDisable(false);
        }
    }

    private void handleSubmit() {
        if (!UserSession.getInstance().isLoggedIn()) {
            statusLabel.setText("Пожалуйста, войдите в систему");
            SceneManager.getInstance().showLoginScreen();
            return;
        }

        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            statusLabel.setText("Пожалуйста, выберите дату");
            return;
        }

        String selectedTime = timeComboBox.getValue();
        if (selectedTime == null || selectedTime.isEmpty()) {
            statusLabel.setText("Пожалуйста, выберите время");
            return;
        }

        if (consultationTypeGroup.getSelectedToggle() == null) {
            statusLabel.setText("Пожалуйста, выберите тип консультации");
            return;
        }

        boolean isTimeBooked = allFittings.stream()
                .anyMatch(f -> f.getDate().equals(selectedDate)
                        && f.getTime().equals(LocalTime.parse(selectedTime))
                        && !"cancelled".equals(f.getStatus()));

        if (isTimeBooked) {
            statusLabel.setText("Это время уже занято. Пожалуйста, выберите другое время");
            updateAvailableTimes();
            return;
        }

        try {
            currentUser = UserSession.getInstance().getCurrentUser();

            Fitting fitting = new Fitting();
            fitting.setUserId(currentUser.getId());
            fitting.setDate(selectedDate);
            fitting.setTime(LocalTime.parse(selectedTime));
            fitting.setConsultationType(
                    measurementsRadio.isSelected() ? "measurements" : "fitting"
            );

            StringBuilder fullNotes = new StringBuilder();

            if (fittingRadio.isSelected() && productToTryComboBox.getValue() != null
                    && !productToTryComboBox.getValue().startsWith("—")) {
                fullNotes.append("📦 Товар для примерки: ").append(productToTryComboBox.getValue()).append("\n");
            } else if (fittingRadio.isSelected()) {
                fullNotes.append("📦 Товар для примерки: не выбран\n");
            }

            if (notesArea.getText() != null && !notesArea.getText().trim().isEmpty()) {
                if (fullNotes.length() > 0) fullNotes.append("\n");
                fullNotes.append("💬 Пожелания: ").append(notesArea.getText().trim());
            }

            fitting.setNotes(fullNotes.toString());
            fitting.setStatus("scheduled");
            fitting.setUserFirstName(currentUser.getFirstName());
            fitting.setUserLastName(currentUser.getLastName());
            fitting.setUserPhone(currentUser.getPhone());

            Fitting createdFitting = SupabaseClient.getInstance().createFitting(fitting);
            if (createdFitting != null) {
                showSuccessAlert();
                clearForm();
                statusLabel.setText("✅ Запись успешно создана!");
                loadAllFittings();
            } else {
                statusLabel.setText("❌ Ошибка при создании записи");
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("❌ Ошибка подключения к серверу: " + e.getMessage());
        }
    }

    private void showSuccessAlert() {
        String message = "✅ Вы успешно записаны!\n\n";
        message += "📅 Дата: " + datePicker.getValue() + "\n";
        message += "⏰ Время: " + timeComboBox.getValue() + "\n";
        message += "📋 Тип: " + (measurementsRadio.isSelected() ? "Снятие мерок" : "Примерка") + "\n";

        if (fittingRadio.isSelected() && productToTryComboBox.getValue() != null
                && !productToTryComboBox.getValue().startsWith("—")) {
            message += "📦 Товар: " + productToTryComboBox.getValue() + "\n";
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Успешно!");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        datePicker.setValue(null);
        timeComboBox.getItems().clear();
        timeComboBox.setPromptText("Сначала выберите дату");
        timeComboBox.setDisable(true);
        consultationTypeGroup.selectToggle(null);
        notesArea.clear();
        productToTryComboBox.setValue(null);
        loadUserOrders();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

