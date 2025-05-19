package model;

import javafx.beans.property.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.datafaker.Faker;
import java.util.Locale;

public class Order {
    private final IntegerProperty id;
    private final StringProperty customer;
    private final DoubleProperty price;
    private final StringProperty status;
    private List<String> products; // Lista produktów SKU
    private static final Faker FAKER = new Faker(new Locale("pl")); // Polski genrator loswoych danych


    public Order(int id, String customer, double price, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.customer = new SimpleStringProperty(customer);
        this.price = new SimpleDoubleProperty(price);
        this.status = new SimpleStringProperty(status);
        this.products = generateRandomProducts(); // Generowanie SKU przy tworzeniu zamówienia
    }

    public static Order generateRandomOrder(int id) {
        String[] statuses = {"Pending", "Completed", "Cancelled"};
        String first = FAKER.name().firstName();
        String last = FAKER.name().lastName();

        String customer = first + " " + last;
        double price = ThreadLocalRandom.current().nextDouble(5, 1212);
        String status = statuses[ThreadLocalRandom.current().nextInt(statuses.length)];

        return new Order(id, customer, price, status);
    }

    // ---------- KONFIGURACJA „Katalogu produktów” ----------
    // Pula wszystkich możliwych SKU
    private static final List<String> CATALOG;

    // SKU, które mają być bestsellerami (mogą, ale nie muszą, znajdować się również w CATALOG)
    private static final List<String> BESTSELLERS = List.of(
            "AAA111", "BBB222", "CCC333", "DDD444", "GGG777"
    );

    // Szansa (0-1), że wylosujemy bestseller zamiast zwykłego SKU
    private static final double BESTSELLER_PROBABILITY = 0.05;

    static {
        // jednorazowo budujemy katalog 200 losowych SKU w formacie ABC123
        CATALOG = IntStream.rangeClosed(100, 299)
                .mapToObj(i -> {
                    Random r = ThreadLocalRandom.current();
                    char a = (char) ('A' + r.nextInt(26));
                    char b = (char) ('A' + r.nextInt(26));
                    char c = (char) ('A' + r.nextInt(26));
                    return "" + a + b + c + i;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // dokładamy bestsellery (jeśli nie są jeszcze w katalogu)
        CATALOG.addAll(BESTSELLERS.stream()
                .filter(sku -> !CATALOG.contains(sku))
                .toList());
    }
    // ---------- KONIEC KONFIGURACJI ----------

    private List<String> generateRandomProducts() {
        Random random = ThreadLocalRandom.current();
        int productCount = random.nextInt(4) + 2;          // 2–5 pozycji
        List<String> productList = new ArrayList<>();

        for (int i = 0; i < productCount; i++) {
            productList.add(pickSku(random));

            // ~50 % szans na wrzucenie drugiej sztuki tego samego SKU w tym samym zamówieniu
            if (random.nextBoolean() && productList.size() < productCount) {
                productList.add(productList.get(productList.size() - 1));
            }
        }
        return productList;
    }

    // Zwraca SKU wybrane z katalogu; częściej trafia się bestseller
    private String pickSku(Random random) {
        if (random.nextDouble() < BESTSELLER_PROBABILITY) {
            // bestseller
            return BESTSELLERS.get(random.nextInt(BESTSELLERS.size()));
        }
        // zwykłe SKU
        return CATALOG.get(random.nextInt(CATALOG.size()));
    }

    public List<String> getProducts() {
        return products;
    }

    public void setProducts(List<String> products) {
        this.products = products;
    }

    public int getId() { return id.get(); }
    public String getCustomer() { return customer.get(); }
    public double getPrice() { return price.get(); }
    public String getStatus() { return status.get(); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty customerProperty() { return customer; }
    public DoubleProperty priceProperty() { return price; }
    public StringProperty statusProperty() { return status; }
}

