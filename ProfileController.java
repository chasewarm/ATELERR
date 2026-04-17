package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.*;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Button backButton;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Label userTypeLabel;
    @FXML private Button saveChangesButton;
    @FXML private Button changePasswordButton;
    @FXML private Label profileStatusLabel;

    // Таблица заказов
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, String> orderModelColumn;  // НОВАЯ КОЛОНКА
    @FXML private TableColumn<Order, String> orderTypeColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;
    @FXML private TableColumn<Order, String> orderDeadlineColumn;
    @FXML private TableColumn<Order, Void> orderActionColumn;

    // Таблица записей на примерку
    @FXML private TableView<Fitting> fittingsTable;
    @FXML private TableColumn<Fitting, LocalDate> fittingDateColumn;
    @FXML private TableColumn<Fitting, LocalTime> fittingTimeColumn;
    @FXML private TableColumn<Fitting, String> fittingTypeColumn;
    @FXML private TableColumn<Fitting, String> fittingNotesColumn;
    @FXML private TableColumn<Fitting, String> fittingStatusColumn;
    @FXML private TableColumn<Fitting, Void> fittingActionColumn;

    private ObservableList<Order> userOrders = FXCollections.observableArrayList();
    private ObservableList<Fitting> userFittings = FXCollections.observableArrayList();
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupActions();
        loadUserData();
        setupOrdersTable();
        setupFittingsTable();
        loadUserOrders();
        loadUserFittings();
    }

    private void setupActions() {
        backButton.setOnAction(event -> SceneManager.getInstance().showMainMenu());
        saveChangesButton.setOnAction(event -> handleSaveChanges());
        changePasswordButton.setOnAction(event -> handleChangePassword());
    }

    private void loadUserData() {
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            firstNameField.setText(currentUser.getFirstName());
            lastNameField.setText(currentUser.getLastName());
            emailField.setText(currentUser.getEmail());
            phoneField.setText(currentUser.getPhone());
            userTypeLabel.setText(currentUser.getIsCorporate() ? "Корпоративный клиент" : "Физическое лицо");
        }
    }

    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        // ДОБАВЛЕНО: отображение модели заказа
        orderModelColumn.setCellValueFactory(cellData -> {
            Order order = cellData.getValue();
            String modelName = order.getModelName();
            if (modelName != null && !modelName.isEmpty()) {
                return new SimpleStringProperty(modelName);
            }
            // Если модель не указана, показываем тип заказа
            String type = order.getOrderType();
            if ("individual".equals(type)) {
                return new SimpleStringProperty("Индивидуальный пошив");
            } else if ("corporate".equals(type)) {
                return new SimpleStringProperty("Корпоративный заказ");
            } else if ("collaboration".equals(type)) {
                return new SimpleStringProperty("Коллаборация");
            }
            return new SimpleStringProperty("—");
        });

        orderTypeColumn.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getOrderType();
            String typeText = switch (type) {
                case "individual" -> "Индивидуальный";
                case "corporate" -> "Корпоративный";
                case "collaboration" -> "Коллаборация";
                default -> type;
            };
            return new SimpleStringProperty(typeText);
        });

        orderStatusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String statusText = switch (status) {
                case "new" -> "Новый";
                case "inProgress" -> "В работе";
                case "ready" -> "Готов";
                case "completed" -> "Завершен";
                default -> status;
            };
            return new SimpleStringProperty(statusText);
        });

        orderStatusColumn.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    Order order = getTableView().getItems().get(getIndex());
                    if (order != null) {
                        String status = order.getStatus();
                        if ("new".equals(status)) {
                            setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
                        } else if ("inProgress".equals(status)) {
                            setStyle("-fx-text-fill: #FF8C00; -fx-font-weight: bold;");
                        } else if ("ready".equals(status)) {
                            setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                        } else if ("completed".equals(status)) {
                            setStyle("-fx-text-fill: #6E473B; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        orderDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(date != null ?
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "");
        });

        orderDeadlineColumn.setCellValueFactory(cellData -> {
            LocalDateTime deadline = cellData.getValue().getDeadline();
            return new SimpleStringProperty(deadline != null ?
                    deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Не указан");
        });

        // Кнопка удаления заказа
        orderActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = new Button("Удалить");

            {
                deleteButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E;");
                deleteButton.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    handleDeleteOrder(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });

        ordersTable.setItems(userOrders);
    }

    private void setupFittingsTable() {
        fittingDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        fittingTimeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));

        fittingTypeColumn.setCellValueFactory(cellData -> {
            String type = cellData.getValue().getConsultationType();
            String typeText = "measurements".equals(type) ? "Снятие мерок" : "Примерка";
            return new SimpleStringProperty(typeText);
        });

        fittingNotesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        fittingStatusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String statusText;
            if ("scheduled".equals(status)) {
                statusText = "Подтверждена";
            } else if ("completed".equals(status)) {
                statusText = "Завершена";
            } else {
                statusText = "Отменена";
            }
            return new SimpleStringProperty(statusText);
        });

        fittingStatusColumn.setCellFactory(column -> new TableCell<Fitting, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    Fitting fitting = getTableView().getItems().get(getIndex());
                    if (fitting != null) {
                        String status = fitting.getStatus();
                        if ("scheduled".equals(status)) {
                            setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                        } else if ("completed".equals(status)) {
                            setStyle("-fx-text-fill: #1976D2; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        // Кнопка отмены записи
        fittingActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button cancelButton = new Button("Отменить");

            {
                cancelButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E;");
                cancelButton.setOnAction(event -> {
                    Fitting fitting = getTableView().getItems().get(getIndex());
                    handleCancelFitting(fitting);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Fitting fitting = getTableView().getItems().get(getIndex());
                    if (fitting != null && !"cancelled".equals(fitting.getStatus()) && !"completed".equals(fitting.getStatus())) {
                        setGraphic(cancelButton);
                    } else {
                        Label dashLabel = new Label("—");
                        dashLabel.setStyle("-fx-text-fill: #6E473B;");
                        setGraphic(dashLabel);
                    }
                }
            }
        });

        fittingsTable.setItems(userFittings);
    }

    private void loadUserOrders() {
        if (currentUser == null) return;
        try {
            var orders = SupabaseClient.getInstance().getOrdersByUserId(currentUser.getId());
            // загружаем названия моделей для каждого заказа
            for (Order order : orders) {
                if (order.getModelId() > 0) {
                    try {
                        Model model = SupabaseClient.getInstance().getModelById(order.getModelId());
                        if (model != null) {
                            order.setModelName(model.getName());
                        }
                    } catch (Exception e) {
                    }
                }
            }
            userOrders.setAll(orders);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Ошибка загрузки", "Не удалось загрузить список заказов");
        }
    }

    private void loadUserFittings() {
        if (currentUser == null) return;

        try {
            var fittings = SupabaseClient.getInstance().getFittingsByUserId(currentUser.getId());
            userFittings.setAll(fittings);
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Ошибка загрузки", "Не удалось загрузить список записей");
        }
    }

    private void handleSaveChanges() {
        if (firstNameField.getText().trim().isEmpty() ||
                lastNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                phoneField.getText().trim().isEmpty()) {
            profileStatusLabel.setText("Все поля должны быть заполнены");
            return;
        }

        try {
            currentUser.setFirstName(firstNameField.getText().trim());
            currentUser.setLastName(lastNameField.getText().trim());
            currentUser.setEmail(emailField.getText().trim());
            currentUser.setPhone(phoneField.getText().trim());

            boolean updated = SupabaseClient.getInstance().updateUser(currentUser);
            if (updated) {
                profileStatusLabel.setText("Данные успешно сохранены");
            } else {
                profileStatusLabel.setText("Ошибка при сохранении данных");
            }
        } catch (Exception e) {
            e.printStackTrace();
            profileStatusLabel.setText("Ошибка подключения к серверу");
        }
    }

    // ДОБАВЛЕНО: диалоговое окно для смены пароля
    private void handleChangePassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Смена пароля");
        dialog.setHeaderText("Введите новый пароль");
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Старый пароль");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Новый пароль");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Подтвердите пароль");
        grid.add(new Label("Старый пароль:"), 0, 0);
        grid.add(oldPasswordField, 1, 0);
        grid.add(new Label("Новый пароль:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Подтвердите:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return newPasswordField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPassword -> {
            String oldPassword = oldPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showErrorAlert("Ошибка", "Все поля должны быть заполнены");
                return;}
            if (!newPassword.equals(confirmPassword)) {
                showErrorAlert("Ошибка", "Пароли не совпадают");
                return;}
            if (newPassword.length() < 6) {
                showErrorAlert("Ошибка", "Пароль должен содержать минимум 6 символов");
                return;}
            try {
                User checkUser = SupabaseClient.getInstance().login(currentUser.getEmail(), oldPassword);
                if (checkUser == null) {
                    showErrorAlert("Ошибка", "Старый пароль введен неверно");
                    return;}
                boolean updated = SupabaseClient.getInstance().updateUserPassword(currentUser.getId(), newPassword);
                if (updated) {
                    currentUser.setPassword(newPassword);
                    showSuccessAlert("Успешно", "Пароль успешно изменен");
                } else {
                    showErrorAlert("Ошибка", "Не удалось изменить пароль");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage());
            }
        });
    }

    private void handleDeleteOrder(Order order) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите удалить заказ №" + order.getId() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = SupabaseClient.getInstance().deleteOrder(order.getId());
                    if (deleted) {
                        userOrders.remove(order);
                        showSuccessAlert("Успешно", "Заказ удален");
                    } else {
                        showErrorAlert("Ошибка", "Не удалось удалить заказ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorAlert("Ошибка", "Ошибка подключения к серверу");
                }
            }
        });
    }

    private void handleCancelFitting(Fitting fitting) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение отмены");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите отменить запись на " +
                fitting.getDate() + " в " + fitting.getTime() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean cancelled = SupabaseClient.getInstance().cancelFitting(fitting.getId());
                    if (cancelled) {
                        loadUserFittings();
                        showSuccessAlert("Успешно", "Запись отменена");
                    } else {
                        showErrorAlert("Ошибка", "Не удалось отменить запись");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorAlert("Ошибка", "Ошибка подключения к серверу");
                }
            }
        });
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}