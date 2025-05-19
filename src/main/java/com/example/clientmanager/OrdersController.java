package com.example.clientmanager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Order;
import java.util.*;

public class OrdersController {
    public TextField minPriceField;
    public TextField maxPriceField;
    public Label countLabel;
    public TextField skuField;
    public TextField nameField;
    public Label avgTimeLabel;
    @FXML
    private TableView<Order> orderTable;
    @FXML
    private TableColumn<Order, Number> idCol;
    @FXML
    private TableColumn<Order, String> customerCol;
    @FXML
    private TableColumn<Order, Number> priceCol;
    @FXML
    private TableColumn<Order, String> statusCol;
    @FXML
    private TableColumn<Order, String> SKUCol;
    private ObservableList<Order> orders;

    @FXML
    public void initialize() {
        // Formatowanie kolumny "priceCol" do wyświetlania ceny z dwoma miejscami po przecinku
        priceCol.setCellFactory(column -> new TableCell<Order, Number>() {
                    @Override
                    protected void updateItem(Number item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(String.format("%.2f", item.doubleValue()));
                        }
                    }
        });

        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        customerCol.setCellValueFactory(data -> data.getValue().customerProperty());
        priceCol.setCellValueFactory(data -> data.getValue().priceProperty());
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());

        // Konfiguracja kolumny dla SKU (Produkty zamówienia)
        SKUCol.setCellValueFactory(cellData -> {
            List<String> products = cellData.getValue().getProducts(); // Pobieranie listy SKU
            String skuDisplay = String.join(", ", products); // Łączenie listy w jeden ciąg
            return new SimpleStringProperty(skuDisplay); // Wyświetlanie w tabeli
        });

        orders = FXCollections.observableArrayList();
        generateRandomOrders(100000); // Losowe dane
        orderTable.setItems(orders);
        updateOrderCount();

        List<Order> ordersList = new ArrayList<>(orders);
        populatePriceMap(ordersList); //PAMIETAC ZE TEGO NIE UWZGLEDNIAMY ALE TO TRWAAA - TWORZENIE MAPY
        populateSkuMap(ordersList);      // Mapa hashy SKU
        indexNames(ordersList);
    }

    private void generateRandomOrders(int count) {
        orders.clear();
        for (int i = 1; i <= count; i++) {
            orders.add(Order.generateRandomOrder(i));
        }
    }

    private void addRandomOrders(int count) {
        // Ustal następne ID (ostatnie + 1)
        int nextId = orders.stream()
                .mapToInt(Order::getId)
                .max()
                .orElse(0) + 1;

        List<Order> justAdded = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Order newOrder = Order.generateRandomOrder(nextId + i);
            orders.add(newOrder);          // ObservableList ⇒ TableView odświeży się sama
            justAdded.add(newOrder);
        }

        populatePriceMap(justAdded);        // dopisujemy tylko nowe rekordy do mapy cen
        populateSkuMap(justAdded);       // aktualizujemy indeks SKU
        indexNames(justAdded);            // hashmap, set imion i nazwisk
        updateOrderCount();                 // odświeżamy licznik w Label
        orderTable.sort();
    }

    // Mapy Hashe etc
    private final TreeMap<Double, List<Order>> orderPriceMap = new TreeMap<>();
    private final Map<String, List<Order>> skuOrderMap = new HashMap<>();
    private final Map<String, Set<Order>> nameIndex = new HashMap<>();



    // Budowanie mapy podczas inicjalizacji lub dodawania zamówienia
    private void populatePriceMap(List<Order> orders) {
        for (Order order : orders) {
            double price = order.getPrice();
            orderPriceMap.computeIfAbsent(price, k -> new ArrayList<>()).add(order);
        }
    }

    // Uzupełnia mapę sku→zamówienia dla przekazanej listy
    private void populateSkuMap(List<Order> orders) {
        for (Order order : orders) {

            // Używamy Set, aby usunąć ewentualne duplikaty SKU w jednym zamówieniu
            for (String sku : new HashSet<>(order.getProducts())) {
                skuOrderMap
                        .computeIfAbsent(sku, k -> new ArrayList<>())
                        .add(order);
            }
        }
    }

    private void indexNames(Collection<Order> orders) {
        for (Order o : orders) {
            String[] parts = o.getCustomer().toLowerCase().split("\\s+"); // ["jan","kowalski"]
            for (String part : parts) {
                nameIndex
                        .computeIfAbsent(part, k -> new HashSet<>())
                        .add(o);
            }
        }
    }

    // Zwraca wszystkie zamówienia pasujące do pojedynczego tokenu (imię lub nazwisko).
    private List<Order> findByName(String query) {
        return new ArrayList<>(nameIndex.getOrDefault(query.toLowerCase(), Set.of()));
    }

    // Obsługuje dwa tokeny – zwraca przecięcie zbiorów (imię ∧ nazwisko).
    private List<Order> findByFullName(String full) {
        String[] t = full.toLowerCase().split("\\s+");     // tokenizacja
        if (t.length != 2) return List.of();               // niepoprawny format
        Set<Order> first  = nameIndex.getOrDefault(t[0], Set.of());
        Set<Order> second = nameIndex.getOrDefault(t[1], Set.of());

        // przecięcie zbiorów → lista
        return first.stream()
                .filter(second::contains)
                .toList();
    }


    // Filtrowanie za pomocą zakresu subMap
    private List<Order> filterByPriceAdvanced(double minPrice, double maxPrice) {
        List<Order> filteredOrders = new ArrayList<>();
        orderPriceMap
                .subMap(minPrice, true, maxPrice, true)
                .values()
                .forEach(filteredOrders::addAll);
        return filteredOrders;
    }

    @FXML
    public void onPrices() {
        try {
            long start, end;
            String minPriceText = minPriceField.getText();
            String maxPriceText = maxPriceField.getText();

            double minPrice = minPriceText.isEmpty() ? 0 : Double.parseDouble(minPriceText);
            double maxPrice = maxPriceText.isEmpty() ? Double.MAX_VALUE : Double.parseDouble(maxPriceText);

            // Filtrowanie listy na podstawie zakresu cen
            start = System.nanoTime();
            List<Order> filteredOrders = filterByPriceAdvanced(minPrice, maxPrice);
            end = System.nanoTime();
            long advancedDuration = end - start;

            // Aktualizacja widoku: czyścimy tabelę i wstawiamy tylko przefiltrowane elementy
            orderTable.setItems(FXCollections.observableArrayList(filteredOrders));

            // Aktualizacja statystyk, np. liczba widocznych elementów
            updateOrderCount();

            // Aktualizacja czasu (TU czasu przefiltrowania gdu juz tree mapa istnieje)
            avgTimeLabel.setText(String.format("%.2f ms", (double) advancedDuration/1_000_000));
        } catch (NumberFormatException e) {
            // Obsługa błędu parsowania liczb
            System.err.println("Wartości cen muszą być liczbowe: " + e.getMessage());
        }
    }

    public void onSKU(ActionEvent actionEvent) {
        long start = 0, end;
        String sku = skuField.getText().trim();
        List<Order> bySku = List.of();

        // jeśli pole puste – pokaż ponownie wszystkie zamówienia
        if (sku.isEmpty()) {
            orderTable.setItems(orders);    // cały zbiór
        } else {
            start = System.nanoTime();
            bySku = skuOrderMap.getOrDefault(sku, List.of());
        }
        end = System.nanoTime();
        long duration = end - start;

        if (!sku.isEmpty()){
            orderTable.setItems(FXCollections.observableArrayList(bySku)); // tylko pasujące - wyciagniete z poprzedzajacego else zebymierzyc czas prosciej
        }

        if(!sku.isEmpty()){
            avgTimeLabel.setText(String.format("%.2f μs", (double) duration/1_000));
        }else{
            avgTimeLabel.setText("-");
        }

        updateOrderCount();
    }

    public void onName(ActionEvent actionEvent) {
        long start, end;
        String raw = nameField.getText().trim();

        // puste pole – pokaż wszystkie rekordy
        if (raw.isEmpty()) {
            orderTable.setItems(orders);
            avgTimeLabel.setText("-");
            updateOrderCount();
            return;
        }

        String[] tokens = raw.split("\\s+");

        List<Order> result;

        start = System.nanoTime();
        if (tokens.length == 1) {                 // tylko imię LUB nazwisko
            result = findByName(tokens[0]);
        } else if (tokens.length == 2) {          // imię i nazwisko
            result = findByFullName(raw);
        } else {                                  // więcej niż 2 słowa – brak wyników
            result = List.of();
        }
        end = System.nanoTime();
        long duration = end - start;

        avgTimeLabel.setText(String.format("%.2f μs", (double) duration/1_000));
        orderTable.setItems(FXCollections.observableArrayList(result));
        updateOrderCount();
    }

    @FXML
    public void onSearch() {
        /* ─────────────────────────── 1. Dane z pól filtrów ────────────────────────── */
        final String rawName = nameField.getText().trim();
        final String sku     = skuField.getText().trim();
        final String minTxt  = minPriceField.getText().trim();
        final String maxTxt  = maxPriceField.getText().trim();

        final boolean nameActive  = !rawName.isEmpty();
        final boolean skuActive   = !sku.isEmpty();
        final boolean priceActive = !minTxt.isEmpty() || !maxTxt.isEmpty();

        /* ───────────────────────── 2. Parsowanie cen (final) ──────────────────────── */
        final double minPrice;
        final double maxPrice;
        try {
            minPrice = minTxt.isBlank() ? 0.0 : Double.parseDouble(minTxt);
            maxPrice = maxTxt.isBlank() ? Double.MAX_VALUE : Double.parseDouble(maxTxt);
        } catch (NumberFormatException ex) {
            System.err.println("Niepoprawny format ceny: " + ex.getMessage());
            return;                                  // przerywamy wyszukiwanie
        }

        /* ──────────────── 3. Bez filtrów ⇒ pokaż wszystkie zamówienia ─────────────── */
        if (!nameActive && !skuActive && !priceActive) {
            orderTable.setItems(orders);
            avgTimeLabel.setText("-");          // czyścimy etykietę czasu
            updateOrderCount();
            return;
        }

        long start = System.nanoTime();
        /* ────────────────────── 4. Wstępne listy kandydatów z indeksów ─────────────── */
        List<Order> priceCand = priceActive
                ? filterByPriceAdvanced(minPrice, maxPrice)           // TreeMap subMap
                : orders;

        List<Order> skuCand   = skuActive
                ? skuOrderMap.getOrDefault(sku, List.of())
                : orders;

        List<Order> nameCand;
        if (nameActive) {
            String[] tokens = rawName.toLowerCase().split("\\s+");
            if (tokens.length == 1) {                        // jedno słowo → imię LUB nazwisko
                nameCand = findByName(tokens[0]);
            } else if (tokens.length == 2) {                 // „imię nazwisko”
                nameCand = findByFullName(rawName);
            } else {                                         // >2 tokeny → brak wyników
                nameCand = List.of();
            }
        } else {
            nameCand = orders;
        }

        /* ──────────────── 5. Wybór najmniejszej listy startowej (optymalizacja) ───── */
        List<Order> base = priceCand;
        if (skuCand.size()   < base.size()) base = skuCand;
        if (nameCand.size()  < base.size()) base = nameCand;

        /* ────────────────────── 6. Końcowe przecinanie warunków ────────────────────── */
        List<Order> finalResult = base.stream()
                .filter(o -> !priceActive || (o.getPrice() >= minPrice && o.getPrice() <= maxPrice))
                .filter(o -> !skuActive   ||  o.getProducts().contains(sku))
                .filter(o -> {
                    if (!nameActive) return true;
                    if (rawName.contains(" ")) {                        // pełne imię + nazwisko
                        return o.getCustomer().equalsIgnoreCase(rawName);
                    }
                    String token = rawName.toLowerCase();
                    return Arrays.stream(o.getCustomer().split("\\s+"))
                            .anyMatch(part -> part.equalsIgnoreCase(token));
                })
                .toList();

        long duration = System.nanoTime() - start;   // czas samego filtrowania

        /* ───────────────────────── 7. Aktualizacja widoku i licznika ───────────────── */
        /* 8.  Aktualizacja etykiety czasu ─ tu możesz wybrać jednostki */
        if (!finalResult.isEmpty()) {
            avgTimeLabel.setText(String.format("%.2f ms", (double) duration/1_000_000));
        } else {
            avgTimeLabel.setText("Brak wyników (0 µs)");
        }
        orderTable.setItems(FXCollections.observableArrayList(finalResult));
        updateOrderCount();
    }

    @FXML
    public void onAdd() {
        addRandomOrders(5); // stare zostają, dochodzi 5 nowych

    }

    public void onClearFilters(ActionEvent actionEvent) {
        nameField.clear();
        skuField.clear();
        minPriceField.clear();
        maxPriceField.clear();
        onSearch();
    }

    private void updateOrderCount() {
        int visibleOrders = orderTable.getItems().size(); // Pobieranie liczby elementów widocznych w tabeli
        countLabel.setText(String.valueOf(visibleOrders)); // Aktualizacja tekstu etykiety
    }
}