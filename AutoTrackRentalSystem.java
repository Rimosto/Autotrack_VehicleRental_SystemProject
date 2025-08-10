import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class AutoTrackRentalSystem {
    // Enums
    public enum VehicleType { CAR, VAN, MOTORCYCLE }
    public enum VehicleStatus { AVAILABLE, BOOKED, MAINTENANCE }
    public enum BookingStatus { ACTIVE, COMPLETED, CANCELLED }
    public enum UserType { ADMIN, CUSTOMER }

    // Classes
    static class Vehicle {
        private String id;
        private String brand;
        private String model;
        private VehicleType type;
        private int capacity;
        private double dailyRate;
        private VehicleStatus status;

        public Vehicle(String id, String brand, String model, VehicleType type, int capacity, double dailyRate) {
            this.id = id;
            this.brand = brand;
            this.model = model;
            this.type = type;
            this.capacity = capacity;
            this.dailyRate = dailyRate;
            this.status = VehicleStatus.AVAILABLE;
        }

        // Getters and setters
        public String getId() { return id; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public VehicleType getType() { return type; }
        public int getCapacity() { return capacity; }
        public double getDailyRate() { return dailyRate; }
        public VehicleStatus getStatus() { return status; }
        public void setStatus(VehicleStatus status) { this.status = status; }

        @Override
        public String toString() {
            return String.format("%s: %s %s (%s, %d seats) - $%.2f/day - Status: %s",
                    id, brand, model, type, capacity, dailyRate, status);
        }
    }

    static class User {
        private String id;
        private String username;
        private String password;
        private String name;
        private UserType type;

        public User(String id, String username, String password, String name, UserType type) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.name = name;
            this.type = type;
        }

        // Getters
        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getName() { return name; }
        public UserType getType() { return type; }

        public boolean isAdmin() { return type == UserType.ADMIN; }
    }

    static class Booking {
        private String id;
        private Vehicle vehicle;
        private User customer;
        private LocalDate startDate;
        private LocalDate endDate;
        private double totalCost;
        private BookingStatus status;

        public Booking(String id, Vehicle vehicle, User customer, LocalDate startDate, LocalDate endDate) {
            this.id = id;
            this.vehicle = vehicle;
            this.customer = customer;
            this.startDate = startDate;
            this.endDate = endDate;
            this.totalCost = calculateCost();
            this.status = BookingStatus.ACTIVE;
            vehicle.setStatus(VehicleStatus.BOOKED);
        }

        public double calculateCost() {
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            return days * vehicle.getDailyRate();
        }

        public void completeBooking() {
            this.status = BookingStatus.COMPLETED;
            vehicle.setStatus(VehicleStatus.AVAILABLE);
        }

        // Added missing getters
        public Vehicle getVehicle() {
            return vehicle;
        }

        public BookingStatus getStatus() {
            return status;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getId() {
            return id;
        }

        public User getCustomer() {
            return customer;
        }

        @Override
        public String toString() {
            return String.format("Booking %s: %s rented %s from %s to %s - Total: $%.2f - Status: %s",
                    id, customer.getName(), vehicle.getModel(), startDate, endDate, totalCost, status);
        }
    }

    // Rental System Core
    static class RentalSystem {
        private List<Vehicle> vehicles = new ArrayList<>();
        private List<User> users = new ArrayList<>();
        private List<Booking> bookings = new ArrayList<>();
        private User currentUser = null;

        // Initialize with sample data
        public RentalSystem() {
            initializeSampleData();
        }

        private void initializeSampleData() {
            // Add sample vehicles
            vehicles.add(new Vehicle("V001", "Toyota", "Corolla", VehicleType.CAR, 5, 50.0));
            vehicles.add(new Vehicle("V002", "Honda", "CR-V", VehicleType.CAR, 5, 70.0));
            vehicles.add(new Vehicle("V003", "Ford", "Transit", VehicleType.VAN, 12, 100.0));
            vehicles.add(new Vehicle("V004", "Harley-Davidson", "Sportster", VehicleType.MOTORCYCLE, 2, 60.0));

            // Add sample users
            users.add(new User("ADM001", "admin", "admin123", "System Admin", UserType.ADMIN));
            users.add(new User("CUS001", "john", "john123", "John Doe", UserType.CUSTOMER));
            users.add(new User("CUS002", "jane", "jane123", "Jane Smith", UserType.CUSTOMER));
        }

        // Authentication
        public boolean login(String username, String password) {
            Optional<User> user = users.stream()
                    .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                    .findFirst();
            
            if (user.isPresent()) {
                currentUser = user.get();
                return true;
            }
            return false;
        }

        public void logout() {
            currentUser = null;
        }

        public User getCurrentUser() {
            return currentUser;
        }

        // Vehicle Management
        public boolean addVehicle(Vehicle vehicle) {
            if (currentUser == null || !currentUser.isAdmin()) return false;
            vehicles.add(vehicle);
            return true;
        }

        public boolean updateVehicleStatus(String vehicleId, VehicleStatus status) {
            if (currentUser == null || !currentUser.isAdmin()) return false;
            Optional<Vehicle> vehicle = vehicles.stream().filter(v -> v.getId().equals(vehicleId)).findFirst();
            if (vehicle.isPresent()) {
                vehicle.get().setStatus(status);
                return true;
            }
            return false;
        }

        // Booking Management
        public boolean makeBooking(String vehicleId, LocalDate startDate, LocalDate endDate) {
            if (currentUser == null || currentUser.isAdmin()) return false;
            
            Optional<Vehicle> vehicle = vehicles.stream()
                    .filter(v -> v.getId().equals(vehicleId) && v.getStatus() == VehicleStatus.AVAILABLE)
                    .findFirst();
            
            if (!vehicle.isPresent()) return false;
            
            // Check if vehicle is already booked for these dates
            boolean isAvailable = bookings.stream()
                    .noneMatch(b -> b.getVehicle().getId().equals(vehicleId) && 
                            b.getStatus() == BookingStatus.ACTIVE &&
                            !(endDate.isBefore(b.getStartDate()) || startDate.isAfter(b.getEndDate())));
            
            if (!isAvailable) return false;
            
            String bookingId = "B" + (bookings.size() + 1);
            bookings.add(new Booking(bookingId, vehicle.get(), currentUser, startDate, endDate));
            return true;
        }

        public boolean returnVehicle(String bookingId) {
            Optional<Booking> booking = bookings.stream()
                    .filter(b -> b.getId().equals(bookingId) && b.getStatus() == BookingStatus.ACTIVE)
                    .findFirst();
            
            if (booking.isPresent()) {
                booking.get().completeBooking();
                return true;
            }
            return false;
        }

        // Getters for data
        public List<Vehicle> getAvailableVehicles() {
            return vehicles.stream()
                    .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                    .collect(Collectors.toList());
        }

        // Added missing getAllVehicles method
        public List<Vehicle> getAllVehicles() {
            return new ArrayList<>(vehicles);
        }

        public List<Booking> getUserBookings() {
            if (currentUser == null) return new ArrayList<>();
            return bookings.stream()
                    .filter(b -> b.getCustomer().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
        }

        public List<Booking> getAllBookings() {
            if (currentUser == null || !currentUser.isAdmin()) return new ArrayList<>();
            return new ArrayList<>(bookings);
        }
    }

    // Console UI
    public static class ConsoleUI {
        private Scanner scanner = new Scanner(System.in);
        private RentalSystem system = new RentalSystem();

        public void run() {
            System.out.println("=== AutoTrack Rental System ===");
            
            while (true) {
                if (system.getCurrentUser() == null) {
                    showLoginMenu();
                } else if (system.getCurrentUser().isAdmin()) {
                    showAdminMenu();
                } else {
                    showCustomerMenu();
                }
            }
        }

        private void showLoginMenu() {
            System.out.println("\nPlease login:");
            System.out.print("Username: ");
            String username = scanner.nextLine();
            System.out.print("Password: ");
            String password = scanner.nextLine();

            if (system.login(username, password)) {
                System.out.println("Login successful! Welcome, " + system.getCurrentUser().getName());
            } else {
                System.out.println("Invalid credentials. Please try again.");
            }
        }

        private void showAdminMenu() {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("1. View all vehicles");
            System.out.println("2. Add new vehicle");
            System.out.println("3. Update vehicle status");
            System.out.println("4. View all bookings");
            System.out.println("5. Logout");
            System.out.print("Select an option: ");

            int choice = Integer.parseInt(scanner.nextLine());
            
            switch (choice) {
                case 1:
                    displayVehicles(system.getAllVehicles());
                    break;
                case 2:
                    addVehicle();
                    break;
                case 3:
                    updateVehicleStatus();
                    break;
                case 4:
                    displayBookings(system.getAllBookings());
                    break;
                case 5:
                    system.logout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }

        private void showCustomerMenu() {
            System.out.println("\n=== CUSTOMER MENU ===");
            System.out.println("1. View available vehicles");
            System.out.println("2. Make a booking");
            System.out.println("3. View my bookings");
            System.out.println("4. Return a vehicle");
            System.out.println("5. Logout");
            System.out.print("Select an option: ");

            int choice = Integer.parseInt(scanner.nextLine());
            
            switch (choice) {
                case 1:
                    displayVehicles(system.getAvailableVehicles());
                    break;
                case 2:
                    makeBooking();
                    break;
                case 3:
                    displayBookings(system.getUserBookings());
                    break;
                case 4:
                    returnVehicle();
                    break;
                case 5:
                    system.logout();
                    break;
                default:
                    System.out.println("Invalid option.");
            }
        }

        private void displayVehicles(List<Vehicle> vehicles) {
            if (vehicles.isEmpty()) {
                System.out.println("No vehicles found.");
                return;
            }
            System.out.println("\n=== VEHICLES ===");
            vehicles.forEach(System.out::println);
        }

        private void displayBookings(List<Booking> bookings) {
            if (bookings.isEmpty()) {
                System.out.println("No bookings found.");
                return;
            }
            System.out.println("\n=== BOOKINGS ===");
            bookings.forEach(System.out::println);
        }

        private void addVehicle() {
            System.out.println("\nAdd New Vehicle:");
            
            System.out.print("ID: ");
            String id = scanner.nextLine();
            
            System.out.print("Brand: ");
            String brand = scanner.nextLine();
            
            System.out.print("Model: ");
            String model = scanner.nextLine();
            
            System.out.print("Type (CAR/VAN/MOTORCYCLE): ");
            VehicleType type = VehicleType.valueOf(scanner.nextLine().toUpperCase());
            
            System.out.print("Capacity: ");
            int capacity = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Daily Rate: ");
            double rate = Double.parseDouble(scanner.nextLine());
            
            Vehicle vehicle = new Vehicle(id, brand, model, type, capacity, rate);
            if (system.addVehicle(vehicle)) {
                System.out.println("Vehicle added successfully!");
            } else {
                System.out.println("Failed to add vehicle. You may not have permission.");
            }
        }

        private void updateVehicleStatus() {
            System.out.println("\nUpdate Vehicle Status:");
            displayVehicles(system.getAllVehicles());
            
            System.out.print("Enter vehicle ID: ");
            String id = scanner.nextLine();
            
            System.out.print("New status (AVAILABLE/BOOKED/MAINTENANCE): ");
            VehicleStatus status = VehicleStatus.valueOf(scanner.nextLine().toUpperCase());
            
            if (system.updateVehicleStatus(id, status)) {
                System.out.println("Vehicle status updated!");
            } else {
                System.out.println("Failed to update status. Vehicle not found or no permission.");
            }
        }

        private void makeBooking() {
            System.out.println("\nMake a Booking:");
            displayVehicles(system.getAvailableVehicles());
            
            System.out.print("Enter vehicle ID: ");
            String vehicleId = scanner.nextLine();
            
            System.out.print("Start date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(scanner.nextLine());
            
            System.out.print("End date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(scanner.nextLine());
            
            if (system.makeBooking(vehicleId, startDate, endDate)) {
                System.out.println("Booking successful!");
            } else {
                System.out.println("Booking failed. Vehicle may not be available for those dates.");
            }
        }

        private void returnVehicle() {
            System.out.println("\nReturn a Vehicle:");
            List<Booking> userBookings = system.getUserBookings().stream()
                    .filter(b -> b.getStatus() == BookingStatus.ACTIVE)
                    .collect(Collectors.toList());
            
            if (userBookings.isEmpty()) {
                System.out.println("You have no active bookings.");
                return;
            }
            
            displayBookings(userBookings);
            System.out.print("Enter booking ID to return: ");
            String bookingId = scanner.nextLine();
            
            if (system.returnVehicle(bookingId)) {
                System.out.println("Vehicle returned successfully!");
            } else {
                System.out.println("Failed to return vehicle. Booking not found.");
            }
        }
    }

    // Main method
    public static void main(String[] args) {
        new ConsoleUI().run();
    }
}