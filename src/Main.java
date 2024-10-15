import java.util.concurrent.Semaphore;

class Store {
    private int items = 0;  // Кількість товарів на складі
    private final Semaphore semaphore;
    private boolean isOpen = true;  // Флаг для завершення роботи

    public Store(int capacity) {
        this.semaphore = new Semaphore(capacity);
    }

    // Метод для додавання товару
    public void addItem() {
        try {
            semaphore.acquire();  // Очікування вільного місця на складі
            if (!isOpen) return;  // Перевірка чи склад все ще відкритий
            items++;
            System.out.println("Постачальник додав товар. Кількість товарів на складі: " + items);
        } catch (InterruptedException e) {
            System.out.println("Виникла помилка під час додавання товару: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    // Метод для забору товару
    public void takeItem() {
        try {
            if (!isOpen) return;  // Перевірка чи склад все ще відкритий
            if (isWorkingHours()) {
                semaphore.acquire();  // Очікування доступного товару
                if (items > 0) {
                    items--;
                    System.out.println("Покупець забрав товар. Кількість товарів на складі: " + items);
                } else {
                    System.out.println("Склад порожній. Покупець не може забрати товар.");
                }
            } else {
                System.out.println("Забір товарів заборонено в неробочі години.");
            }
        } catch (InterruptedException e) {
            System.out.println("Виникла помилка під час забору товару: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    // Метод для завершення роботи складу
    public void closeStore() {
        isOpen = false;
        System.out.println("Склад закрито.");
    }

    // Метод для перевірки робочих годин (змінено на 7:00 - 23:00)
    private boolean isWorkingHours() {
        int hour = java.time.LocalTime.now().getHour();
        return hour >= 7 && hour < 23;  // Робочі години з 7:00 до 23:00
    }
}

class Supplier implements Runnable {
    private final Store store;
    private final int limit;  // Ліміт товарів, які постачальник може додати

    public Supplier(Store store, int limit) {
        this.store = store;
        this.limit = limit;
    }

    @Override
    public void run() {
        for (int i = 0; i < limit; i++) {
            store.addItem();
            try {
                Thread.sleep(2000);  // Затримка перед додаванням наступного товару
            } catch (InterruptedException e) {
                System.out.println("Постачальника перервано: " + e.getMessage());
            }
        }
    }
}

class Customer implements Runnable {
    private final Store store;

    public Customer(Store store) {
        this.store = store;
    }

    @Override
    public void run() {
        while (true) {
            store.takeItem();
            try {
                Thread.sleep(3000);  // Затримка перед наступною спробою забору товару
            } catch (InterruptedException e) {
                System.out.println("Покупця перервано: " + e.getMessage());
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Store store = new Store(10);  // Створення складу з ємністю 10 товарів
        Supplier supplier = new Supplier(store, 20);  // Постачальник додасть 20 товарів

        Thread supplierThread = new Thread(supplier);
        Thread customerThread = new Thread(new Customer(store));

        supplierThread.start();
        customerThread.start();

        try {
            // Чекаємо, поки постачальник додасть всі товари
            supplierThread.join();
        } catch (InterruptedException e) {
            System.out.println("Головний потік перервано: " + e.getMessage());
        }

        // Закриваємо склад після завершення постачальника
        store.closeStore();
        customerThread.interrupt();  // Зупиняємо потік покупця після закриття складу

        System.out.println("Програма завершила роботу.");
    }
}
