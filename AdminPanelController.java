package com.gibsie.atelier.controller;

import com.gibsie.atelier.model.*;
import com.gibsie.atelier.util.SceneManager;
import com.gibsie.atelier.util.SupabaseClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminPanelController implements Initializable {

    @FXML private Button backToMenuButton;
    @FXML private Label adminNameLabel;
    @FXML private TabPane adminTabPane;
    @FXML private Tab ordersTab;
    @FXML private Tab fittingsTab;
    @FXML private Tab modelsTab;
    @FXML private Tab fabricsTab;

    //таблица заказов
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> orderIdColumn;
    @FXML private TableColumn<Order, String> orderUserColumn;
    @FXML private TableColumn<Order, String> orderTypeColumn;
    @FXML private TableColumn<Order, String> orderStatusColumn;
    @FXML private TableColumn<Order, String> orderDateColumn;
    @FXML private TableColumn<Order, String> orderDeadlineColumn;
    @FXML private TableColumn<Order, String> orderSizeColumn;
    @FXML private TableColumn<Order, String> orderFabricColumn;
    @FXML private TableColumn<Order, String> orderColorColumn;
    @FXML private TableColumn<Order, Integer> orderQuantityColumn;
    @FXML private TableColumn<Order, Void> orderActionsColumn;

    //таблица записей на примерку
    @FXML private TableView<Fitting> fittingsTable;
    @FXML private TableColumn<Fitting, Integer> fittingIdColumn;
    @FXML private TableColumn<Fitting, String> fittingUserColumn;
    @FXML private TableColumn<Fitting, LocalDate> fittingDateColumn;
    @FXML private TableColumn<Fitting, LocalTime> fittingTimeColumn;
    @FXML private TableColumn<Fitting, String> fittingTypeColumn;
    @FXML private TableColumn<Fitting, String> fittingNotesColumn;
    @FXML private TableColumn<Fitting, String> fittingStatusColumn;
    @FXML private TableColumn<Fitting, Void> fittingActionsColumn;

    //таблица моделей
    @FXML private TableView<Model> modelsTable;
    @FXML private TableColumn<Model, Integer> modelIdColumn;
    @FXML private TableColumn<Model, String> modelNameColumn;
    @FXML private TableColumn<Model, String> modelTypeColumn;
    @FXML private TableColumn<Model, String> modelCollectionColumn;
    @FXML private TableColumn<Model, Double> modelPriceColumn;
    @FXML private TableColumn<Model, Integer> modelStockColumn;
    @FXML private TableColumn<Model, Void> modelActionsColumn;

    //таблица тканей
    @FXML private TableView<Fabric> fabricsTable;
    @FXML private TableColumn<Fabric, Integer> fabricIdColumn;
    @FXML private TableColumn<Fabric, String> fabricNameColumn;
    @FXML private TableColumn<Fabric, String> fabricCompositionColumn;
    @FXML private TableColumn<Fabric, String> fabricColorColumn;
    @FXML private TableColumn<Fabric, Double> fabricPriceColumn;
    @FXML private TableColumn<Fabric, Integer> fabricStockColumn;
    @FXML private TableColumn<Fabric, Void> fabricActionsColumn;

    @FXML private Button addModelButton;
    @FXML private Button addFabricButton;

    private ObservableList<Order> allOrders = FXCollections.observableArrayList();
    private ObservableList<Fitting> allFittings = FXCollections.observableArrayList();
    private ObservableList<Model> allModels = FXCollections.observableArrayList();
    private ObservableList<Fabric> allFabrics = FXCollections.observableArrayList();
    private User currentAdmin;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentAdmin = UserSession.getInstance().getCurrentUser();
        if (currentAdmin != null) {
            adminNameLabel.setText("Администратор: " + currentAdmin.getFirstName() + " " + currentAdmin.getLastName());
        }
        setupActions();
        setupOrdersTable();
        setupFittingsTable();
        setupModelsTable();
        setupFabricsTable();
        loadAllData();
    }
    private void setupActions() {
        backToMenuButton.setOnAction(event -> {
            SceneManager.getInstance().showMainMenu();
        });

        addModelButton.setOnAction(event -> showAddModelDialog());
        addFabricButton.setOnAction(event -> showAddFabricDialog());
    }
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        //отображение имени пользователя
        orderUserColumn.setCellValueFactory(cellData -> {
            Order order = cellData.getValue();
            String fullName = order.getFullName();
            String phone = order.getUserPhone();
            if (fullName != null && !fullName.isEmpty()) {
                return new SimpleStringProperty(fullName + (phone != null ? " (" + phone + ")" : ""));
            }
            return new SimpleStringProperty("Пользователь ID: " + order.getUserId());
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
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "");
        });
        orderDeadlineColumn.setCellValueFactory(cellData -> {
            LocalDateTime deadline = cellData.getValue().getDeadline();
            return new SimpleStringProperty(deadline != null ?
                    deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "Не указан");
        });
        orderSizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        orderFabricColumn.setCellValueFactory(new PropertyValueFactory<>("fabric"));
        orderColorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
        orderQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        orderActionsColumn.setCellFactory(createOrderActionsCellFactory());
        ordersTable.setItems(allOrders);
    }

    private void setupFittingsTable() {
        fittingIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        //отображение имени пользователя
        fittingUserColumn.setCellValueFactory(cellData -> {
            Fitting fitting = cellData.getValue();
            String fullName = fitting.getFullName();
            String phone = fitting.getUserPhone();
            if (fullName != null && !fullName.isEmpty()) {
                return new SimpleStringProperty(fullName + (phone != null ? " (" + phone + ")" : ""));
            }
            return new SimpleStringProperty("Пользователь ID: " + fitting.getUserId());
        });

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
            String statusText = switch (status) {
                case "scheduled" -> "Подтверждена";
                case "completed" -> "Завершена";
                case "cancelled" -> "Отменена";
                default -> status;
            };
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

        fittingActionsColumn.setCellFactory(createFittingActionsCellFactory());

        fittingsTable.setItems(allFittings);
    }

    private void setupModelsTable() {
        modelIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        modelNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        modelTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        modelCollectionColumn.setCellValueFactory(cellData -> {
            String collection = cellData.getValue().getCollection();
            String collectionText = switch (collection) {
                case "basic" -> "Базовая";
                case "limited" -> "Лимитированная";
                case "creator" -> "От создателя";
                default -> collection;
            };
            return new SimpleStringProperty(collectionText);
        });
        modelPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        modelStockColumn.setCellValueFactory(new PropertyValueFactory<>("inStock"));

        modelPriceColumn.setCellFactory(column -> new TableCell<Model, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f ₽", item));
                }
            }
        });
        modelActionsColumn.setCellFactory(createModelActionsCellFactory());
        modelsTable.setItems(allModels);
    }

    private void setupFabricsTable() {
        fabricIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fabricNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        fabricCompositionColumn.setCellValueFactory(new PropertyValueFactory<>("composition"));
        fabricColorColumn.setCellValueFactory(new PropertyValueFactory<>("color"));
        fabricPriceColumn.setCellValueFactory(new PropertyValueFactory<>("pricePerMeter"));
        fabricStockColumn.setCellValueFactory(new PropertyValueFactory<>("inStock"));
        fabricPriceColumn.setCellFactory(column -> new TableCell<Fabric, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f ₽/м", item));
                }
            }
        });

        fabricActionsColumn.setCellFactory(createFabricActionsCellFactory());
        fabricsTable.setItems(allFabrics);
    }
    private Callback<TableColumn<Order, Void>, TableCell<Order, Void>> createOrderActionsCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E; -fx-font-size: 14;");
                deleteButton.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 14;");

                editButton.setOnAction(event -> {
                    Order order = getTableView().getItems().get(getIndex());
                    showEditOrderDialog(order);
                });

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
                    setGraphic(pane);
                }
            }
        };
    }

    private Callback<TableColumn<Fitting, Void>, TableCell<Fitting, Void>> createFittingActionsCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E; -fx-font-size: 14;");
                deleteButton.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 14;");
                editButton.setOnAction(event -> {
                    Fitting fitting = getTableView().getItems().get(getIndex());
                    showEditFittingDialog(fitting);
                });
                deleteButton.setOnAction(event -> {
                    Fitting fitting = getTableView().getItems().get(getIndex());
                    handleDeleteFitting(fitting);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }

    private Callback<TableColumn<Model, Void>, TableCell<Model, Void>> createModelActionsCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E; -fx-font-size: 14;");
                deleteButton.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 14;");

                editButton.setOnAction(event -> {
                    Model model = getTableView().getItems().get(getIndex());
                    showEditModelDialog(model);
                });

                deleteButton.setOnAction(event -> {
                    Model model = getTableView().getItems().get(getIndex());
                    handleDeleteModel(model);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }
    private Callback<TableColumn<Fabric, Void>, TableCell<Fabric, Void>> createFabricActionsCellFactory() {
        return param -> new TableCell<>() {
            private final Button editButton = new Button("✏️");
            private final Button deleteButton = new Button("🗑️");
            private final HBox pane = new HBox(5, editButton, deleteButton);

            {
                editButton.setStyle("-fx-background-color: #A78D78; -fx-text-fill: #291C0E; -fx-font-size: 14;");
                deleteButton.setStyle("-fx-background-color: #C62828; -fx-text-fill: white; -fx-font-size: 14;");
                editButton.setOnAction(event -> {
                    Fabric fabric = getTableView().getItems().get(getIndex());
                    showEditFabricDialog(fabric);
                });
                deleteButton.setOnAction(event -> {
                    Fabric fabric = getTableView().getItems().get(getIndex());
                    handleDeleteFabric(fabric);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }
    private void loadAllData() {
        try {
            allOrders.setAll(SupabaseClient.getInstance().getAllOrders());
            allFittings.setAll(SupabaseClient.getInstance().getAllFittings());
            allModels.setAll(SupabaseClient.getInstance().getAllModels());
            allFabrics.setAll(SupabaseClient.getInstance().getAllFabrics());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка загрузки", "Не удалось загрузить данные: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showEditOrderDialog(Order order) {
        Dialog<Order> dialog = new Dialog<>();
        dialog.setTitle("Редактирование заказа");
        dialog.setHeaderText("Заказ №" + order.getId());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("new", "inProgress", "ready", "completed");
        statusCombo.setValue(order.getStatus());

        TextField sizeField = new TextField(order.getSize() != null ? order.getSize() : "");
        TextField fabricField = new TextField(order.getFabric() != null ? order.getFabric() : "");
        TextField colorField = new TextField(order.getColor() != null ? order.getColor() : "");
        TextField quantityField = new TextField(String.valueOf(order.getQuantity()));
        TextArea descriptionArea = new TextArea(order.getDescription() != null ? order.getDescription() : "");
        descriptionArea.setPrefRowCount(3);

        DatePicker deadlinePicker = new DatePicker();
        if (order.getDeadline() != null) {
            deadlinePicker.setValue(order.getDeadline().toLocalDate());
        }
        grid.add(new Label("Статус:"), 0, 0);
        grid.add(statusCombo, 1, 0);
        grid.add(new Label("Размер:"), 0, 1);
        grid.add(sizeField, 1, 1);
        grid.add(new Label("Ткань:"), 0, 2);
        grid.add(fabricField, 1, 2);
        grid.add(new Label("Цвет:"), 0, 3);
        grid.add(colorField, 1, 3);
        grid.add(new Label("Количество:"), 0, 4);
        grid.add(quantityField, 1, 4);
        grid.add(new Label("Срок:"), 0, 5);
        grid.add(deadlinePicker, 1, 5);
        grid.add(new Label("Описание:"), 0, 6);
        grid.add(descriptionArea, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                order.setStatus(statusCombo.getValue());
                order.setSize(sizeField.getText());
                order.setFabric(fabricField.getText());
                order.setColor(colorField.getText());
                try {
                    order.setQuantity(Integer.parseInt(quantityField.getText()));
                } catch (NumberFormatException e) {
                    order.setQuantity(1);
                }
                order.setDescription(descriptionArea.getText());
                if (deadlinePicker.getValue() != null) {
                    order.setDeadline(deadlinePicker.getValue().atStartOfDay());
                }
                return order;
            }
            return null;
        });

        Optional<Order> result = dialog.showAndWait();
        result.ifPresent(updatedOrder -> {
            try {
                boolean success = SupabaseClient.getInstance().updateOrder(updatedOrder);
                if (success) {
                    loadAllData();
                    showAlert("Успех", "Заказ обновлен", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Успех", "Заказ обновлен", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    private void showEditFittingDialog(Fitting fitting) {
        Dialog<Fitting> dialog = new Dialog<>();
        dialog.setTitle("Редактирование записи");
        dialog.setHeaderText("Запись ID: " + fitting.getId());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker datePicker = new DatePicker(fitting.getDate());
        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.getItems().addAll("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00");
        timeCombo.setValue(fitting.getTime().toString());

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("measurements", "fitting");
        typeCombo.setValue(fitting.getConsultationType());

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("scheduled", "completed", "cancelled");
        statusCombo.setValue(fitting.getStatus());

        TextArea notesArea = new TextArea(fitting.getNotes() != null ? fitting.getNotes() : "");
        notesArea.setPrefRowCount(3);

        grid.add(new Label("Дата:"), 0, 0);
        grid.add(datePicker, 1, 0);
        grid.add(new Label("Время:"), 0, 1);
        grid.add(timeCombo, 1, 1);
        grid.add(new Label("Тип:"), 0, 2);
        grid.add(typeCombo, 1, 2);
        grid.add(new Label("Статус:"), 0, 3);
        grid.add(statusCombo, 1, 3);
        grid.add(new Label("Заметки:"), 0, 4);
        grid.add(notesArea, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                fitting.setDate(datePicker.getValue());
                fitting.setTime(LocalTime.parse(timeCombo.getValue()));
                fitting.setConsultationType(typeCombo.getValue());
                fitting.setStatus(statusCombo.getValue());
                fitting.setNotes(notesArea.getText());
                return fitting;
            }
            return null;
        });

        Optional<Fitting> result = dialog.showAndWait();
        result.ifPresent(updatedFitting -> {
            try {
                boolean success = SupabaseClient.getInstance().updateFitting(updatedFitting);
                if (success) {
                    loadAllData();
                    showAlert("Успех", "Запись обновлена", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Успех", "Запись обновлена", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showEditModelDialog(Model model) {
        Dialog<Model> dialog = new Dialog<>();
        dialog.setTitle("Редактирование модели");
        dialog.setHeaderText("Модель: " + model.getName());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(model.getName());
        TextField typeField = new TextField(model.getType());

        ComboBox<String> collectionCombo = new ComboBox<>();
        collectionCombo.getItems().addAll("basic", "limited", "creator");
        collectionCombo.setValue(model.getCollection());

        TextField priceField = new TextField(String.valueOf(model.getPrice()));
        TextField stockField = new TextField(String.valueOf(model.getInStock()));
        TextField imageField = new TextField(model.getImagePath() != null ? model.getImagePath() : "");
        TextArea descArea = new TextArea(model.getDescription() != null ? model.getDescription() : "");
        descArea.setPrefRowCount(3);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Тип:"), 0, 1);
        grid.add(typeField, 1, 1);
        grid.add(new Label("Коллекция:"), 0, 2);
        grid.add(collectionCombo, 1, 2);
        grid.add(new Label("Цена:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("В наличии:"), 0, 4);
        grid.add(stockField, 1, 4);
        grid.add(new Label("Изображение:"), 0, 5);
        grid.add(imageField, 1, 5);
        grid.add(new Label("Описание:"), 0, 6);
        grid.add(descArea, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                model.setName(nameField.getText());
                model.setType(typeField.getText());
                model.setCollection(collectionCombo.getValue());
                try {
                    model.setPrice(Double.parseDouble(priceField.getText()));
                } catch (NumberFormatException e) {
                    model.setPrice(0);
                }
                try {
                    model.setInStock(Integer.parseInt(stockField.getText()));
                } catch (NumberFormatException e) {
                    model.setInStock(0);
                }
                model.setImagePath(imageField.getText());
                model.setDescription(descArea.getText());
                return model;
            }
            return null;
        });

        Optional<Model> result = dialog.showAndWait();
        result.ifPresent(updatedModel -> {
            try {
                boolean success = SupabaseClient.getInstance().updateModel(updatedModel);
                if (success) {
                    loadAllData();
                    showAlert("Успех", "Модель обновлена", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Успех", "Модель обновлена", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showEditFabricDialog(Fabric fabric) {
        Dialog<Fabric> dialog = new Dialog<>();
        dialog.setTitle("Редактирование ткани");
        dialog.setHeaderText("Ткань: " + fabric.getName());

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(fabric.getName());
        TextField compositionField = new TextField(fabric.getComposition());
        TextField colorField = new TextField(fabric.getColor());
        TextField priceField = new TextField(String.valueOf(fabric.getPricePerMeter()));
        TextField stockField = new TextField(String.valueOf(fabric.getInStock()));

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Состав:"), 0, 1);
        grid.add(compositionField, 1, 1);
        grid.add(new Label("Цвет:"), 0, 2);
        grid.add(colorField, 1, 2);
        grid.add(new Label("Цена за метр:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("В наличии (метров):"), 0, 4);
        grid.add(stockField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                fabric.setName(nameField.getText());
                fabric.setComposition(compositionField.getText());
                fabric.setColor(colorField.getText());
                try {
                    fabric.setPricePerMeter(Double.parseDouble(priceField.getText()));
                } catch (NumberFormatException e) {
                    fabric.setPricePerMeter(0);
                }
                try {
                    fabric.setInStock(Integer.parseInt(stockField.getText()));
                } catch (NumberFormatException e) {
                    fabric.setInStock(0);
                }
                return fabric;
            }
            return null;
        });
        Optional<Fabric> result = dialog.showAndWait();
        result.ifPresent(updatedFabric -> {
            try {
                boolean success = SupabaseClient.getInstance().updateFabric(updatedFabric);
                if (success) {
                    loadAllData();
                    showAlert("Успех", "Ткань обновлена", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Успех", "Ткань обновлена", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    private void showAddModelDialog() {
        Dialog<Model> dialog = new Dialog<>();
        dialog.setTitle("Добавление модели");
        dialog.setHeaderText("Новая модель");

        ButtonType saveButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Название");

        TextField typeField = new TextField();
        typeField.setPromptText("Тип (hoodie, sweatshirt, dress...)");

        ComboBox<String> collectionCombo = new ComboBox<>();
        collectionCombo.getItems().addAll("basic", "limited", "creator");
        collectionCombo.setPromptText("Коллекция");
        collectionCombo.setValue("basic");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена");

        TextField stockField = new TextField();
        stockField.setPromptText("В наличии");

        TextField imageField = new TextField();
        imageField.setPromptText("URL изображения");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Описание");
        descArea.setPrefRowCount(3);

        grid.add(new Label("Название:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Тип:*"), 0, 1);
        grid.add(typeField, 1, 1);
        grid.add(new Label("Коллекция:*"), 0, 2);
        grid.add(collectionCombo, 1, 2);
        grid.add(new Label("Цена:*"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("В наличии:*"), 0, 4);
        grid.add(stockField, 1, 4);
        grid.add(new Label("Изображение:"), 0, 5);
        grid.add(imageField, 1, 5);
        grid.add(new Label("Описание:"), 0, 6);
        grid.add(descArea, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите название модели", Alert.AlertType.ERROR);
                    return null;
                }
                if (typeField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите тип модели", Alert.AlertType.ERROR);
                    return null;
                }
                if (collectionCombo.getValue() == null) {
                    showAlert("Ошибка", "Выберите коллекцию", Alert.AlertType.ERROR);
                    return null;
                }
                if (priceField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите цену", Alert.AlertType.ERROR);
                    return null;
                }
                if (stockField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите количество в наличии", Alert.AlertType.ERROR);
                    return null;
                }

                Model model = new Model();
                model.setName(nameField.getText().trim());
                model.setType(typeField.getText().trim());
                model.setCollection(collectionCombo.getValue());

                try {
                    model.setPrice(Double.parseDouble(priceField.getText().trim().replace(",", ".")));
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Цена должна быть числом", Alert.AlertType.ERROR);
                    return null;
                }

                try {
                    model.setInStock(Integer.parseInt(stockField.getText().trim()));
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Количество должно быть целым числом", Alert.AlertType.ERROR);
                    return null;
                }

                model.setImagePath(imageField.getText().trim());
                model.setDescription(descArea.getText().trim());
                return model;
            }
            return null;
        });

        Optional<Model> result = dialog.showAndWait();
        result.ifPresent(newModel -> {
            try {
                Model created = SupabaseClient.getInstance().createModel(newModel);
                if (created != null && created.getId() > 0) {
                    loadAllData();
                    showAlert("Успех", "Модель успешно добавлена с ID: " + created.getId(), Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Успех", "Модель успешно добавлена с ID: ", Alert.AlertType.INFORMATION);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }
    private void showAddFabricDialog() {
        Dialog<Fabric> dialog = new Dialog<>();
        dialog.setTitle("Добавление ткани");
        dialog.setHeaderText("Новая ткань");

        ButtonType saveButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Название ткани");

        TextField compositionField = new TextField();
        compositionField.setPromptText("Состав (например: 100% хлопок)");

        TextField colorField = new TextField();
        colorField.setPromptText("Цвет");

        TextField priceField = new TextField();
        priceField.setPromptText("Цена за метр");

        TextField stockField = new TextField();
        stockField.setPromptText("В наличии (метров)");

        grid.add(new Label("Название:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Состав:*"), 0, 1);
        grid.add(compositionField, 1, 1);
        grid.add(new Label("Цвет:*"), 0, 2);
        grid.add(colorField, 1, 2);
        grid.add(new Label("Цена за метр:*"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("В наличии:*"), 0, 4);
        grid.add(stockField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите название ткани", Alert.AlertType.ERROR);
                    return null;
                }
                if (compositionField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите состав ткани", Alert.AlertType.ERROR);
                    return null;
                }
                if (colorField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите цвет ткани", Alert.AlertType.ERROR);
                    return null;}
                if (priceField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите цену", Alert.AlertType.ERROR);
                    return null;}
                if (stockField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите количество в наличии", Alert.AlertType.ERROR);
                    return null;}

                Fabric fabric = new Fabric();
                fabric.setName(nameField.getText().trim());
                fabric.setComposition(compositionField.getText().trim());
                fabric.setColor(colorField.getText().trim());

                try {
                    fabric.setPricePerMeter(Double.parseDouble(priceField.getText().trim().replace(",", ".")));
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Цена должна быть числом", Alert.AlertType.ERROR);
                    return null;}

                try {
                    fabric.setInStock(Integer.parseInt(stockField.getText().trim()));
                } catch (NumberFormatException e) {
                    showAlert("Ошибка", "Количество должно быть целым числом", Alert.AlertType.ERROR);
                    return null;}

                return fabric;}
            return null;
        });

        Optional<Fabric> result = dialog.showAndWait();
        result.ifPresent(newFabric -> {
            try {
                Fabric created = SupabaseClient.getInstance().createFabric(newFabric);
                if (created != null && created.getId() > 0) {
                    loadAllData();
                    showAlert("Успех", "Ткань успешно добавлена с ID: " + created.getId(), Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Ошибка", "Не удалось добавить ткань", Alert.AlertType.ERROR);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
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
                        loadAllData();
                        showAlert("Успех", "Заказ удален", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Ошибка", "Не удалось удалить заказ", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void handleDeleteFitting(Fitting fitting) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите удалить запись на " +
                fitting.getDate() + " в " + fitting.getTime() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = SupabaseClient.getInstance().deleteFitting(fitting.getId());
                    if (deleted) {
                        loadAllData();
                        showAlert("Успех", "Запись удалена", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Ошибка", "Не удалось удалить запись", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    private void handleDeleteModel(Model model) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите удалить модель \"" + model.getName() + "\"?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = SupabaseClient.getInstance().deleteModel(model.getId());
                    if (deleted) {
                        loadAllData();
                        showAlert("Успех", "Модель удалена", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Ошибка", "Не удалось удалить модель", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    private void handleDeleteFabric(Fabric fabric) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы уверены, что хотите удалить ткань \"" + fabric.getName() + "\"?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean deleted = SupabaseClient.getInstance().deleteFabric(fabric.getId());
                    if (deleted) {
                        loadAllData();
                        showAlert("Успех", "Ткань удалена", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("Ошибка", "Не удалось удалить ткань", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Ошибка", "Ошибка подключения к серверу: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}