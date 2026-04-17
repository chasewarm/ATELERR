package com.gibsie.atelier.util;

import com.gibsie.atelier.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SupabaseClient {
    private static SupabaseClient instance;
    private final String SUPABASE_URL = "https://xdngfvnarfqnwljupwdt.supabase.co";
    private final String SUPABASE_KEY = "sb_publishable_GxCEfD2kQbW_h8e67ZKr6g_wZDnZJUV";
    private final HttpClient httpClient;

    private SupabaseClient() {
        this.httpClient = HttpClient.newHttpClient();
    }
    public static SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();}
        return instance;}

    //методы для пользователей
    public User login(String email, String password) throws IOException, InterruptedException {
        String query = String.format("/rest/v1/users?email=eq.%s&password=eq.%s&select=*", email, password);
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseUser(jsonArray.getJSONObject(0));
            }
        }
        return null;}

    public User register(User user) throws IOException, InterruptedException {
        // проверка существования пользователя
        String checkQuery = "/rest/v1/users?email=eq." + user.getEmail() + "&select=id";
        HttpRequest checkRequest = createGetRequest(checkQuery);
        HttpResponse<String> checkResponse = httpClient.send(checkRequest, HttpResponse.BodyHandlers.ofString());

        if (checkResponse.statusCode() == 200) {
            JSONArray checkArray = new JSONArray(checkResponse.body());
            if (!checkArray.isEmpty()) {
                return null; // пользователь уже существует
            }
        }
        JSONObject json = new JSONObject();
        json.put("first_name", user.getFirstName());
        json.put("last_name", user.getLastName());
        json.put("email", user.getEmail());
        json.put("phone", user.getPhone());
        json.put("password", user.getPassword());
        json.put("is_corporate", user.getIsCorporate());
        json.put("is_admin", false); // по умолчанию не админ
        HttpRequest request = createPostRequest("/rest/v1/users", json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseUser(jsonArray.getJSONObject(0));
            }
        }
        return null;}

    public boolean updateUser(User user) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("first_name", user.getFirstName());
        json.put("last_name", user.getLastName());
        json.put("email", user.getEmail());
        json.put("phone", user.getPhone());
        HttpRequest request = createPatchRequest("/rest/v1/users?id=eq." + user.getId(), json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;
    }
    // ДОБАВЛЕНО: метод для обновления пароля пользователя
    public boolean updateUserPassword(int userId, String newPassword) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("password", newPassword);

        HttpRequest request = createPatchRequest("/rest/v1/users?id=eq." + userId, json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;}

    //админские методы для управления заказами
    public List<Order> getAllOrders() throws IOException, InterruptedException {
        String query = "/rest/v1/orders?select=*&order=created_at.desc";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Order> orders = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                Order order = parseOrder(jsonArray.getJSONObject(i));
                //подгружаем информацию о пользователе
                try {
                    User user = getUserById(order.getUserId());
                    if (user != null) {
                        order.setUserFirstName(user.getFirstName());
                        order.setUserLastName(user.getLastName());
                        order.setUserPhone(user.getPhone());
                    }
                } catch (Exception e) {
                }
                orders.add(order);
            }
        }
        return orders;}

    public boolean updateOrder(Order order) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("user_id", order.getUserId());
        if (order.getModelId() > 0) {
            json.put("model_id", order.getModelId());
        }
        json.put("order_type", order.getOrderType());
        json.put("status", order.getStatus());
        json.put("size", order.getSize());
        json.put("fabric", order.getFabric());
        json.put("color", order.getColor());
        json.put("quantity", order.getQuantity());
        json.put("description", order.getDescription());
        if (order.getDeadline() != null) {
            json.put("deadline", order.getDeadline().format(DateTimeFormatter.ISO_DATE_TIME));
        }

        HttpRequest request = createPatchRequest("/rest/v1/orders?id=eq." + order.getId(), json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;}

    //админские методы для управления записями на примерку
    public List<Fitting> getAllFittings() throws IOException, InterruptedException {
        String query = "/rest/v1/fittings?select=*&order=date.desc,time.desc";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Fitting> fittings = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                Fitting fitting = parseFitting(jsonArray.getJSONObject(i));
                // Подгружаем информацию о пользователе
                try {
                    User user = getUserById(fitting.getUserId());
                    if (user != null) {
                        fitting.setUserFirstName(user.getFirstName());
                        fitting.setUserLastName(user.getLastName());
                        fitting.setUserPhone(user.getPhone());
                    }
                } catch (Exception e) {
                }
                fittings.add(fitting);
            }
        }
        return fittings;}

    public boolean updateFitting(Fitting fitting) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("user_id", fitting.getUserId());
        json.put("date", fitting.getDate().format(DateTimeFormatter.ISO_DATE));
        json.put("time", fitting.getTime().format(DateTimeFormatter.ISO_TIME));
        json.put("consultation_type", fitting.getConsultationType());
        json.put("notes", fitting.getNotes());
        json.put("status", fitting.getStatus());
        HttpRequest request = createPatchRequest("/rest/v1/fittings?id=eq." + fitting.getId(), json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;}

    public boolean deleteFitting(int fittingId) throws IOException, InterruptedException {
        HttpRequest request = createDeleteRequest("/rest/v1/fittings?id=eq." + fittingId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;}

    // админские методы для управления моделями
    public Model createModel(Model model) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("name", model.getName());
        json.put("type", model.getType());
        json.put("collection", model.getCollection());
        json.put("description", model.getDescription());
        json.put("price", model.getPrice());
        json.put("image_path", model.getImagePath());
        json.put("in_stock", model.getInStock());
        HttpRequest request = createPostRequest("/rest/v1/models", json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseModel(jsonArray.getJSONObject(0));}
        }
        return null;}

    public boolean updateModel(Model model) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("name", model.getName());
        json.put("type", model.getType());
        json.put("collection", model.getCollection());
        json.put("description", model.getDescription());
        json.put("price", model.getPrice());
        json.put("image_path", model.getImagePath());
        json.put("in_stock", model.getInStock());
        HttpRequest request = createPatchRequest("/rest/v1/models?id=eq." + model.getId(), json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;}

    public boolean deleteModel(int modelId) throws IOException, InterruptedException {
        HttpRequest request = createDeleteRequest("/rest/v1/models?id=eq." + modelId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;}

    //методы для моделй одежды
    public List<Model> getAllModels() throws IOException, InterruptedException {
        HttpRequest request = createGetRequest("/rest/v1/models?select=*");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Model> models = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                models.add(parseModel(jsonArray.getJSONObject(i)));
            }
        }
        return models;}

    public Model getModelById(int modelId) throws IOException, InterruptedException {
        String query = "/rest/v1/models?id=eq." + modelId + "&select=*";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseModel(jsonArray.getJSONObject(0));
            }
        }
        return null;}

    public List<Model> getModelsByCollection(String collection) throws IOException, InterruptedException {
        String query = "/rest/v1/models?collection=eq." + collection + "&select=*";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Model> models = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                models.add(parseModel(jsonArray.getJSONObject(i)));
            }
        }
        return models;}

    //методы для заказов
    public Order createOrder(Order order) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("user_id", order.getUserId());
        if (order.getModelId() > 0) {
            json.put("model_id", order.getModelId());}
        json.put("order_type", order.getOrderType());
        json.put("status", order.getStatus() != null ? order.getStatus() : "new");
        json.put("size", order.getSize());
        json.put("fabric", order.getFabric());
        json.put("color", order.getColor());
        json.put("quantity", order.getQuantity() > 0 ? order.getQuantity() : 1);
        json.put("description", order.getDescription());
        json.put("sketch_path", order.getSketchPath());

        HttpRequest request = createPostRequest("/rest/v1/orders", json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseOrder(jsonArray.getJSONObject(0));}
        }
        return null;
    }
    public List<Order> getOrdersByUserId(int userId) throws IOException, InterruptedException {
        String query = "/rest/v1/orders?user_id=eq." + userId + "&select=*&order=created_at.desc";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Order> orders = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                Order order = new Order();
                order.setId(json.getInt("id"));
                order.setUserId(json.getInt("user_id"));
                if (json.has("model_id") && !json.isNull("model_id")) {
                    order.setModelId(json.getInt("model_id"));
                }
                order.setOrderType(json.getString("order_type"));

                //чтение статуса
                String status = json.getString("status");
                System.out.println("Parsing order #" + order.getId() + " with status: '" + status + "'");
                order.setStatus(status);

                order.setSize(json.optString("size", null));
                order.setFabric(json.optString("fabric", null));
                order.setColor(json.optString("color", null));
                order.setQuantity(json.optInt("quantity", 1));
                order.setDescription(json.optString("description", null));
                order.setSketchPath(json.optString("sketch_path", null));

                if (json.has("created_at") && !json.isNull("created_at")) {
                    String dateStr = json.getString("created_at").replace("Z", "");
                    order.setCreatedAt(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                }
                if (json.has("deadline") && !json.isNull("deadline")) {
                    String dateStr = json.getString("deadline").replace("Z", "");
                    order.setDeadline(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                }

                //загружаем название модели
                if (order.getModelId() > 0) {
                    try {
                        Model model = getModelById(order.getModelId());
                        if (model != null) {
                            order.setModelName(model.getName());
                        }
                    } catch (Exception e) {

                    }
                }

                orders.add(order);
            }
        }
        return orders;
    }

    public boolean deleteOrder(int orderId) throws IOException, InterruptedException {
        HttpRequest request = createDeleteRequest("/rest/v1/orders?id=eq." + orderId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;}

    //методы для записи на примерку
    public Fitting createFitting(Fitting fitting) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("user_id", fitting.getUserId());
        json.put("date", fitting.getDate().format(DateTimeFormatter.ISO_DATE));
        json.put("time", fitting.getTime().format(DateTimeFormatter.ISO_TIME));
        json.put("consultation_type", fitting.getConsultationType());
        json.put("notes", fitting.getNotes());
        json.put("status", fitting.getStatus() != null ? fitting.getStatus() : "scheduled");

        HttpRequest request = createPostRequest("/rest/v1/fittings", json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseFitting(jsonArray.getJSONObject(0));}
        }
        return null;
    }
    public List<Fitting> getFittingsByUserId(int userId) throws IOException, InterruptedException {
        String query = "/rest/v1/fittings?user_id=eq." + userId + "&select=*&order=date.asc";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Fitting> fittings = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                fittings.add(parseFitting(jsonArray.getJSONObject(i)));
            }
        }
        return fittings;
    }
    public boolean cancelFitting(int fittingId) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("status", "cancelled");

        HttpRequest request = createPatchRequest("/rest/v1/fittings?id=eq." + fittingId, json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;}

    // методы для тканей
    public List<Fabric> getAllFabrics() throws IOException, InterruptedException {
        HttpRequest request = createGetRequest("/rest/v1/fabrics?select=*");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        List<Fabric> fabrics = new ArrayList<>();
        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            for (int i = 0; i < jsonArray.length(); i++) {
                fabrics.add(parseFabric(jsonArray.getJSONObject(i)));
            }
        }
        return fabrics;}

    //методы парсинга
    private User parseUser(JSONObject json) {
        User user = new User();
        user.setId(json.getInt("id"));
        user.setFirstName(json.getString("first_name"));
        user.setLastName(json.getString("last_name"));
        user.setEmail(json.getString("email"));
        user.setPhone(json.getString("phone"));
        user.setPassword(json.getString("password"));
        user.setIsCorporate(json.optBoolean("is_corporate", false));
        user.setIsAdmin(json.optBoolean("is_admin", false));
        return user;}

    private Model parseModel(JSONObject json) {
        Model model = new Model();
        model.setId(json.getInt("id"));
        model.setName(json.getString("name"));
        model.setType(json.getString("type"));
        model.setCollection(json.getString("collection"));
        model.setDescription(json.optString("description", ""));
        model.setPrice(json.getDouble("price"));
        model.setImagePath(json.optString("image_path", ""));
        model.setInStock(json.getInt("in_stock"));
        return model;}

    private Order parseOrder(JSONObject json) {
        Order order = new Order();
        order.setId(json.getInt("id"));
        order.setUserId(json.getInt("user_id"));
        if (json.has("model_id") && !json.isNull("model_id")) {
            order.setModelId(json.getInt("model_id"));}
        order.setOrderType(json.getString("order_type"));
        order.setStatus(json.getString("status"));
        order.setSize(json.optString("size", null));
        order.setFabric(json.optString("fabric", null));
        order.setColor(json.optString("color", null));
        order.setQuantity(json.optInt("quantity", 1));
        order.setDescription(json.optString("description", null));
        order.setSketchPath(json.optString("sketch_path", null));

        if (json.has("created_at") && !json.isNull("created_at")) {
            String dateStr = json.getString("created_at").replace("Z", "");
            order.setCreatedAt(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));}
        if (json.has("deadline") && !json.isNull("deadline")) {
            String dateStr = json.getString("deadline").replace("Z", "");
            order.setDeadline(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));}
        return order;}

    private Fitting parseFitting(JSONObject json) {
        Fitting fitting = new Fitting();
        fitting.setId(json.getInt("id"));
        fitting.setUserId(json.getInt("user_id"));
        fitting.setDate(LocalDate.parse(json.getString("date")));
        fitting.setTime(LocalTime.parse(json.getString("time")));
        fitting.setConsultationType(json.getString("consultation_type"));
        fitting.setNotes(json.optString("notes", null));
        fitting.setStatus(json.getString("status"));
        return fitting;}

    private Fabric parseFabric(JSONObject json) {
        Fabric fabric = new Fabric();
        fabric.setId(json.getInt("id"));
        fabric.setName(json.getString("name"));
        fabric.setComposition(json.getString("composition"));
        fabric.setColor(json.getString("color"));
        fabric.setPricePerMeter(json.getDouble("price_per_meter"));
        fabric.setInStock(json.getInt("in_stock"));
        return fabric;}

    //методы для создания запросов
    private HttpRequest createGetRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .GET()
                .build();
    }

    private HttpRequest createPostRequest(String endpoint, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest createPatchRequest(String endpoint, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest createDeleteRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(SUPABASE_URL + endpoint))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();
    }
    //методы для работы с тканями (админка)
    public Fabric createFabric(Fabric fabric) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("name", fabric.getName());
        json.put("composition", fabric.getComposition());
        json.put("color", fabric.getColor());
        json.put("price_per_meter", fabric.getPricePerMeter());
        json.put("in_stock", fabric.getInStock());

        HttpRequest request = createPostRequest("/rest/v1/fabrics", json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseFabric(jsonArray.getJSONObject(0));
            }
        }
        return null;}

    public boolean updateFabric(Fabric fabric) throws IOException, InterruptedException {
        JSONObject json = new JSONObject();
        json.put("name", fabric.getName());
        json.put("composition", fabric.getComposition());
        json.put("color", fabric.getColor());
        json.put("price_per_meter", fabric.getPricePerMeter());
        json.put("in_stock", fabric.getInStock());

        HttpRequest request = createPatchRequest("/rest/v1/fabrics?id=eq." + fabric.getId(), json.toString());
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return response.statusCode() == 204;
    }
    public boolean deleteFabric(int fabricId) throws IOException, InterruptedException {
        HttpRequest request = createDeleteRequest("/rest/v1/fabrics?id=eq." + fabricId);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 204;}

    //метод для получения пользователя по ID
    public User getUserById(int userId) throws IOException, InterruptedException {
        String query = "/rest/v1/users?id=eq." + userId + "&select=*";
        HttpRequest request = createGetRequest(query);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONArray jsonArray = new JSONArray(response.body());
            if (!jsonArray.isEmpty()) {
                return parseUser(jsonArray.getJSONObject(0));}
        }
        return null;}
}