package lk.ijse.fleet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

// ======================= Main =======================

public class Main {
    private static final String LINE = "=".repeat(56);
    private static final int BOX_WIDTH = 54;
    private static final boolean ANIMATIONS = true; 

    private final FleetManager fleetManager;
    private final UserManager userManager;
    private final Scanner scanner;
    private SystemUser loggedInUser; 
    private boolean running = true;

    public Main() {
        fleetManager = new FleetManager();
        userManager = new UserManager();
        scanner = new Scanner(System.in);

        
        userManager.registerUser("Kulin", "1122", "Kulin");

        
        fleetManager.setRentalEventListener(agreement ->
                System.out.println("[EVENT] New rental created: " + agreement.getAgreementId()));

        
        Thread statusThread = new Thread(new VehicleStatusMonitor(fleetManager));
        statusThread.setDaemon(true);
        statusThread.start();

        Thread overdueThread = new Thread(new OverdueReminderWorker(fleetManager));
        overdueThread.setDaemon(true);
        overdueThread.start();
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        while (running) {
            if (loggedInUser == null) {
                showAuthMenu();
            } else {
                showMainMenu();
            }
        }
        System.out.println("Goodbye!");
    }

    // ---------------------------------------------------------------
    // Auth menu
    // ---------------------------------------------------------------
    public void showAuthMenu() {
        while (running && loggedInUser == null) {
            System.out.println(LINE);
            System.out.println("      FLEET RENTAL MANAGEMENT SYSTEM (FRMS)");
            System.out.println(LINE);
            System.out.println("[1] Login");
            System.out.println("[2] Register New User");
            System.out.println("[0] Exit");
            System.out.println(LINE);
            int choice = readInt("Enter choice > ");
            switch (choice) {
                case 1:
                    handleLogin();
                    break;
                case 2:
                    String ru = readText("Choose a username: ");
                    String rp = readText("Choose a password: ");
                    String rn = readText("Full name: ");
                    userManager.registerUser(ru, rp, rn);
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Login page (with animation)
    // ---------------------------------------------------------------
    private void handleLogin() {
        drawLoginBox();
        String username = readText("  Username : ");
        String password = readText("  Password : ");
        System.out.println();
        showLoadingBar("Verifying credentials");
        SystemUser user = userManager.login(username, password);
        System.out.println();
        if (user != null) {
            loggedInUser = user;
            slowPrintln("  ACCESS GRANTED. Welcome, " + user.getFullName() + "!", 25);
            sleep(700);
        } else {
            System.out.println("  ACCESS DENIED.");
            System.out.println("Invalid username or password");
        }
    }

    private void drawLoginBox() {
        String border = "+" + "-".repeat(BOX_WIDTH) + "+";
        slowPrintln(border, 3);
        printRow(center(""), 40);
        String[] rows = blockText("LOGIN");
        for (String r : rows) {
            printRow(center(r), 90);
        }
        printRow(center(""), 40);
        slowPrintln(center("Fleet Rental Management System"), 8);
        slowPrintln(border, 3);
    }

    private String center(String text) {
        int left = (BOX_WIDTH - text.length()) / 2;
        return "|" + " ".repeat(Math.max(0, left)) + text
                + " ".repeat(Math.max(0, BOX_WIDTH - left - text.length())) + "|";
    }

    private void printRow(String row, int delay) {
        System.out.println(row);
        sleep(delay);
    }

    private void showLoadingBar(String label) {
        int total = 30;
        System.out.print("  " + label + " [");
        for (int i = 0; i < total; i++) {
            System.out.print("#");
            System.out.flush();
            sleep(35);
        }
        System.out.println("] 100%");
    }

    private void slowPrintln(String text, int millisPerChar) {
        for (int i = 0; i < text.length(); i++) {
            System.out.print(text.charAt(i));
            System.out.flush();
            sleep(millisPerChar);
        }
        System.out.println();
    }

    private void sleep(int millis) {
        if (!ANIMATIONS) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String[] glyph(char c) {
        switch (c) {
            case 'L': return new String[] {"#    ", "#    ", "#    ", "#    ", "#####"};
            case 'O': return new String[] {" ### ", "#   #", "#   #", "#   #", " ### "};
            case 'G': return new String[] {" ####", "#    ", "# ###", "#   #", " ### "};
            case 'I': return new String[] {"#####", "  #  ", "  #  ", "  #  ", "#####"};
            case 'N': return new String[] {"#   #", "##  #", "# # #", "#  ##", "#   #"};
            default:  return new String[] {"     ", "     ", "     ", "     ", "     "};
        }
    }

    private String[] blockText(String word) {
        String[] rows = new String[5];
        for (int r = 0; r < 5; r++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                if (i > 0) sb.append("   ");
                sb.append(glyph(word.charAt(i))[r]);
            }
            rows[r] = sb.toString();
        }
        return rows;
    }

    // ---------------------------------------------------------------
    // Main menu
    // ---------------------------------------------------------------
    public void showMainMenu() {
        System.out.println("Welcome, " + loggedInUser.getFullName() + "! Loading fleet dashboard...");
        while (loggedInUser != null) {
            System.out.println(LINE);
            System.out.println("FRMS MAIN MENU | Logged in as: " + loggedInUser.getFullName());
            System.out.println(LINE);
            System.out.println("[1] Add Vehicle");
            System.out.println("[2] Add Customer");
            System.out.println("[3] View Available Vehicles");
            System.out.println("[4] Create Rental Agreement");
            System.out.println("[5] Return Vehicle");
            System.out.println("[6] View Revenue Report");
            System.out.println("[7] View Vehicles by Fuel Type");
            System.out.println("[8] View Combined Fleet & Customer Report");
            System.out.println("[9] View All Customers");
            System.out.println("[10] View All Rental Agreements");
            System.out.println("[0] Logout");
            System.out.println(LINE);
            switch (readInt("Enter choice > ")) {
                case 1:  addVehicle(); break;
                case 2:  addCustomer(); break;
                case 3:  printList(fleetManager.getAvailableVehicles(), "No vehicles found."); break;
                case 4:  createRental(); break;
                case 5:  fleetManager.returnVehicle(readText("Agreement ID (e.g. AGR-1001): ")); break;
                case 6:  revenueReport(); break;
                case 7:  printList(fleetManager.getVehiclesByFuelType(readFuelType()), "No vehicles found."); break;
                case 8:  fleetManager.printAllReportables(); break;
                case 9:  printList(fleetManager.getAllCustomers(), "No customers yet."); break;
                case 10: printList(fleetManager.getAllAgreements(), "No rental agreements yet."); break;
                case 0:
                    System.out.println("Logged out.");
                    loggedInUser = null;
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------
    private void addVehicle() {
        System.out.println("[1] Car  [2] Van  [3] Truck");
        int type = readIntInRange("Vehicle type: ", 1, 3);
        String reg = readText("Registration number: ");
        String brand = readText("Brand: ");
        String model = readText("Model: ");
        double rate = readPositiveDouble("Daily rate: ");
        FuelType fuel = readFuelType();
        Vehicle v;
        if (type == 1) {
            v = new Car(reg, brand, model, rate, fuel);
        } else if (type == 2) {
            v = new Van(reg, brand, model, rate, fuel, readIntInRange("Passenger capacity: ", 1, 100));
        } else {
            v = new Truck(reg, brand, model, rate, fuel, readPositiveDouble("Cargo capacity (tons): "));
        }
        fleetManager.addVehicle(v);
        System.out.println("Vehicle added: " + v.getVehicleId());
    }

    private void addCustomer() {
        System.out.println("[1] Individual  [2] Corporate");
        int type = readIntInRange("Customer type: ", 1, 2);
        String name = readText("Full name / company name: ");
        String contact = readText("Contact number: ");
        Customer c;
        if (type == 1) {
            c = new IndividualCustomer(name, contact, readIntInRange("Loyalty points: ", 0, 1_000_000));
        } else {
            c = new CorporateCustomer(name, contact, readPositiveDouble("Credit limit: "));
        }
        fleetManager.addCustomer(c);
        System.out.println("Customer added: " + c.getCustomerId());
    }

    private void createRental() {
        String vehicleId = readText("Vehicle ID (e.g. VEH-1001): ");
        String customerId = readText("Customer ID (e.g. CUS-1001): ");
        int days = readIntInRange("Rental days: ", 1, 3650);
        try {
            RentalAgreement agreement = fleetManager.createRentalAgreement(vehicleId, customerId, days);
            if (agreement != null) {
                System.out.println(agreement);
            }
        } catch (VehicleNotAvailableException | InsufficientCreditException e) {
            System.out.println("Rental failed: " + e.getMessage());
        }
    }

    private void revenueReport() {
        System.out.println("=".repeat(64));
        System.out.println("FLEET REVENUE REPORT");
        System.out.println("=".repeat(64));
        System.out.printf(Locale.US, "Total Revenue: %,.2f%n", fleetManager.generateRevenueReport());
        System.out.println("-".repeat(64));
    }

  
private <T> void printList(List<T> list, String emptyMessage) {
    if (list.isEmpty()) {
        System.out.println(emptyMessage);
        return;
    }

    if (list.get(0) instanceof Vehicle) {
        printVehicleTable(list);
    } else if (list.get(0) instanceof Customer) {
        printCustomerTable(list);
    } else if (list.get(0) instanceof RentalAgreement) {
        printAgreementTable(list);
    }
}

private void printVehicleTable(List<?> list) {
    System.out.println("--------------------------------------------------------------------------------");
    System.out.printf("%-12s %-12s %-12s %-12s %-12s %-12s %-12s%n",
            "Vehicle ID", "Reg No", "Brand", "Model",
            "Rate/Day", "Fuel", "Status");
    System.out.println("--------------------------------------------------------------------------------");

    for (Object obj : list) {
        Vehicle v = (Vehicle) obj;
        System.out.printf("%-12s %-12s %-12s %-12s %-12.2f %-12s %-12s%n",
                v.getVehicleId(),
                v.getRegistrationNumber(),
                v.getBrand(),
                v.getModel(),
                v.getDailyRate(),
                v.getFuelType(),
                v.getStatus());
    }

    System.out.println("--------------------------------------------------------------------------------");
}

private void printCustomerTable(List<?> list) {
    System.out.println("--------------------------------------------------------------------------------");
    System.out.printf("%-12s %-20s %-15s %-15s%n",
            "Customer ID", "Name", "Contact", "Type");
    System.out.println("--------------------------------------------------------------------------------");

    for (Object obj : list) {
        Customer c = (Customer) obj;
        System.out.printf("%-12s %-20s %-15s %-15s%n",
                c.getCustomerId(),
                c.getFullName(),
                c.getContactNumber(),
                c.getTypeName());
    }

    System.out.println("--------------------------------------------------------------------------------");
}

private void printAgreementTable(List<?> list) {
    System.out.println("--------------------------------------------------------------------------------");
    System.out.printf("%-12s %-12s %-20s %-12s %-12s%n",
            "Agreement", "Vehicle ID", "Customer", "Days", "Total Cost");
    System.out.println("--------------------------------------------------------------------------------");

    for (Object obj : list) {
        RentalAgreement a = (RentalAgreement) obj;
        System.out.printf("%-12s %-12s %-20s %-12d %-12.2f%n",
                a.getAgreementId(),
                a.getVehicle().getVehicleId(),
                a.getCustomer().getFullName(),
                a.getRentalDays(),
                a.getTotalCost());
    }

    System.out.println("--------------------------------------------------------------------------------");
}

    // ---------------------------------------------------------------
    // Input helpers (validation - never crash on bad input)
    // ---------------------------------------------------------------
    private String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Input cannot be empty.");
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
            return -1;
        }
    }

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int v = Integer.parseInt(scanner.nextLine().trim());
                if (v >= min && v <= max) return v;
                System.out.println("Enter a value between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double v = Double.parseDouble(scanner.nextLine().trim());
                if (v > 0 && !Double.isInfinite(v)) return v;
                System.out.println("Value must be greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private FuelType readFuelType() {
        FuelType[] types = FuelType.values();
        for (int i = 0; i < types.length; i++) {
            System.out.println("[" + (i + 1) + "] " + types[i]);
        }
        return types[readIntInRange("Fuel type: ", 1, types.length) - 1];
    }
}

// ======================= FuelType =======================

enum FuelType {
    PETROL(1.00), DIESEL(0.90), ELECTRIC(0.75), HYBRID(0.85);

    private final double costMultiplier;

    FuelType(double costMultiplier) {
        this.costMultiplier = costMultiplier;
    }

    public double getCostMultiplier() {
        return costMultiplier;
    }
}

// ======================= RentalStatus =======================

enum RentalStatus {
    AVAILABLE, RENTED
}

// ======================= SystemUser =======================

class SystemUser {
    private String username;
    private String password; 
    private String fullName;

    public SystemUser(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        return "SystemUser{username='" + username + "', fullName='" + fullName + "'}";
    }
}

// ======================= UserManager =======================

class UserManager {
    private final List<SystemUser> users = new ArrayList<>();

    public SystemUser registerUser(String username, String password, String fullName) {
        if (findByUsername(username) != null) {
            System.out.println("Username already taken.");
            return null;
        }
        SystemUser user = new SystemUser(username, password, fullName);
        users.add(user);
        System.out.println("User '" + username + "' registered successfully.");
        return user;
    }

    public SystemUser login(String username, String password) {
        for (SystemUser u : users) {
            if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    public SystemUser findByUsername(String username) {
        for (SystemUser u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }
}

// ======================= Reportable =======================

interface Reportable {
    String getSummary();
    String getReportCategory();
}

// ======================= RentalEventListener =======================

@FunctionalInterface
interface RentalEventListener {
    void onRentalCreated(RentalAgreement agreement);
}

// ======================= Exceptions =======================

class VehicleNotAvailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public VehicleNotAvailableException(String message) {
        super(message);
    }
}

class InsufficientCreditException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientCreditException(String message) {
        super(message);
    }
}

// ======================= Vehicle =======================

abstract class Vehicle implements Reportable {
    private static int counter = 1000;

    private final String vehicleId;
    private String registrationNumber;
    private String brand;
    private String model;
    private double dailyRate;
    private FuelType fuelType;
    private RentalStatus status;

    public Vehicle(String registrationNumber, String brand, String model,
                   double dailyRate, FuelType fuelType) {
        this.vehicleId = "VEH-" + (++counter);
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.dailyRate = dailyRate;
        this.fuelType = fuelType;
        this.status = RentalStatus.AVAILABLE;
    }

    public String getVehicleId() { return vehicleId; } 

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }

    public String getTypeName() { return "Vehicle"; }

    
    public abstract double calculateRentalCost(int rentalDays);

    @Override
    public String getSummary() {
        return vehicleId + " " + brand + " " + model + " " + status;
    }

    @Override
    public String getReportCategory() {
        return "VEHICLE";
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s | Reg: %s | %s %s | Rate: %,.2f/day | Fuel: %s | Status: %s",
                vehicleId, registrationNumber, brand, model, dailyRate, fuelType, status);
    }
}

// ======================= Car =======================

class Car extends Vehicle {
    public Car(String registrationNumber, String brand, String model,
               double dailyRate, FuelType fuelType) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
    }

    @Override
    public String getTypeName() { return "Car"; }

    @Override
    public double calculateRentalCost(int rentalDays) {
        return getDailyRate() * rentalDays * getFuelType().getCostMultiplier();
    }
}

// ======================= Van =======================
class Van extends Vehicle {
    private int passengerCapacity;

    public Van(String registrationNumber, String brand, String model,
               double dailyRate, FuelType fuelType, int passengerCapacity) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
        this.passengerCapacity = passengerCapacity;
    }

    public int getPassengerCapacity() { return passengerCapacity; }
    public void setPassengerCapacity(int passengerCapacity) { this.passengerCapacity = passengerCapacity; }

    @Override
    public String getTypeName() { return "Van"; }

    @Override
    public double calculateRentalCost(int rentalDays) {
        
        return (getDailyRate() * rentalDays * getFuelType().getCostMultiplier())
                + (getDailyRate() * 0.20);
    }

    @Override
    public String toString() {
        return super.toString() + " | Passengers: " + passengerCapacity;
    }
}

// ======================= Truck =======================
class Truck extends Vehicle {
    private double cargoCapacityTons;

    public Truck(String registrationNumber, String brand, String model,
                 double dailyRate, FuelType fuelType, double cargoCapacityTons) {
        super(registrationNumber, brand, model, dailyRate, fuelType);
        this.cargoCapacityTons = cargoCapacityTons;
    }

    public double getCargoCapacityTons() { return cargoCapacityTons; }
    public void setCargoCapacityTons(double cargoCapacityTons) { this.cargoCapacityTons = cargoCapacityTons; }

    @Override
    public String getTypeName() { return "Truck"; }

    @Override
    public double calculateRentalCost(int rentalDays) {
       
        return (getDailyRate() * rentalDays * getFuelType().getCostMultiplier())
                + (cargoCapacityTons * 10.00 * rentalDays);
    }

    @Override
    public String toString() {
        return super.toString() + " | Cargo: " + cargoCapacityTons + "t";
    }
}

// ======================= Customer =======================

abstract class Customer implements Reportable {
    private static int counter = 1000;

    private final String customerId;
    private String fullName;
    private String contactNumber;

    public Customer(String fullName, String contactNumber) {
        this.customerId = "CUS-" + (++counter);
        this.fullName = fullName;
        this.contactNumber = contactNumber;
    }

    public String getCustomerId() { return customerId; } 

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getTypeName() { return "Customer"; }

   
    public abstract double applyDiscount(double subtotal);

    @Override
    public String getSummary() {
        return customerId + " " + fullName;
    }

    @Override
    public String getReportCategory() {
        return "CUSTOMER";
    }

    @Override
    public String toString() {
        return customerId + " | " + fullName + " | " + contactNumber;
    }
}

// ======================= IndividualCustomer =======================
class IndividualCustomer extends Customer {
    private int loyaltyPoints;

    public IndividualCustomer(String fullName, String contactNumber, int loyaltyPoints) {
        super(fullName, contactNumber);
        this.loyaltyPoints = loyaltyPoints;
    }

    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    @Override
    public String getTypeName() { return "Individual"; }

    @Override
    public double applyDiscount(double subtotal) {
        double discountRate;
        if (loyaltyPoints >= 300) {
            discountRate = 0.10;
        } else if (loyaltyPoints >= 100) {
            discountRate = 0.05;
        } else {
            discountRate = 0.00;
        }
        return subtotal * (1 - discountRate);
    }

    @Override
    public String toString() {
        return super.toString() + " | Individual | Points: " + loyaltyPoints;
    }
}

// ======================= CorporateCustomer =======================
class CorporateCustomer extends Customer {
    private double creditLimit;
    private double outstandingBalance; 

    public CorporateCustomer(String fullName, String contactNumber, double creditLimit) {
        super(fullName, contactNumber);
        this.creditLimit = creditLimit;
    }

    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double creditLimit) { this.creditLimit = creditLimit; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    @Override
    public String getTypeName() { return "Corporate"; }

    @Override
    public double applyDiscount(double subtotal) {
        return subtotal * 0.85; 
    }

    public boolean hasSufficientCredit(double cost) {
        return (outstandingBalance + cost) <= creditLimit;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s | Corporate | Limit: %,.2f | Outstanding: %,.2f",
                super.toString(), creditLimit, outstandingBalance);
    }
}

// ======================= RentalAgreement =======================

class RentalAgreement {
    private static int counter = 1000;

    private final String agreementId;
    private final Vehicle vehicle;
    private final Customer customer;
    private final int rentalDays;
    private final double totalCost;
    private int daysElapsed;

    public RentalAgreement(Vehicle vehicle, Customer customer, int rentalDays) {
        double subtotal = vehicle.calculateRentalCost(rentalDays); 
        this.totalCost = customer.applyDiscount(subtotal);         
        this.agreementId = "AGR-" + (++counter);
        this.vehicle = vehicle;
        this.customer = customer;
        this.rentalDays = rentalDays;
        this.daysElapsed = 0;
    }

    public String getAgreementId() { return agreementId; }
    public Vehicle getVehicle() { return vehicle; }
    public Customer getCustomer() { return customer; }
    public int getRentalDays() { return rentalDays; }
    public double getTotalCost() { return totalCost; }
    public int getDaysElapsed() { return daysElapsed; }
    public void setDaysElapsed(int daysElapsed) { this.daysElapsed = daysElapsed; }

    @Override
    public String toString() {
        String line = "=".repeat(64);
        String nl = System.lineSeparator();
        return line + nl
                + "RENTAL AGREEMENT RECEIPT" + nl
                + line + nl
                + "Agreement ID : " + agreementId + nl
                + "Vehicle      : " + vehicle.getVehicleId() + " - " + vehicle.getBrand() + " "
                + vehicle.getModel() + " (" + vehicle.getTypeName() + ")" + nl
                + "Customer     : " + customer.getCustomerId() + " - " + customer.getFullName()
                + " (" + customer.getTypeName() + ")" + nl
                + "Rental Days  : " + rentalDays + nl
                + "Total Cost   : " + String.format(Locale.US, "%,.2f", totalCost) + nl
                + line;
    }
}

// ======================= FleetManager =======================

class FleetManager {
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<RentalAgreement> agreements = new ArrayList<>();
    private RentalEventListener rentalEventListener; 

    public synchronized void addVehicle(Vehicle v) { vehicles.add(v); }

    public synchronized void addCustomer(Customer c) { customers.add(c); }

    public synchronized Vehicle findVehicleById(String id) {
        for (Vehicle v : vehicles) {
            if (v.getVehicleId().equalsIgnoreCase(id)) return v;
        }
        return null;
    }

    public synchronized Customer findCustomerById(String id) {
        for (Customer c : customers) {
            if (c.getCustomerId().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    public void setRentalEventListener(RentalEventListener listener) {
        this.rentalEventListener = listener;
    }

    public synchronized List<Vehicle> getAvailableVehicles() {
        return vehicles.stream()
                .filter(v -> v.getStatus() == RentalStatus.AVAILABLE)
                .collect(Collectors.toList());
    }

    public synchronized List<Customer> getAllCustomers() {
        return new ArrayList<>(customers);
    }

    public synchronized List<RentalAgreement> getAllAgreements() {
        return new ArrayList<>(agreements);
    }

    // Follows the flow diagram in Section 9.21
    public synchronized RentalAgreement createRentalAgreement(String vehicleId, String customerId, int rentalDays)
            throws VehicleNotAvailableException, InsufficientCreditException {

        Vehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) {
            System.out.println("No vehicle found with ID: " + vehicleId);
            return null;
        }
        if (vehicle.getStatus() != RentalStatus.AVAILABLE) {
            throw new VehicleNotAvailableException(
                    "Vehicle " + vehicle.getVehicleId() + " is not available (status: " + vehicle.getStatus() + ").");
        }
        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            System.out.println("No customer found with ID: " + customerId);
            return null;
        }

        double subtotal = vehicle.calculateRentalCost(rentalDays);
        double total = customer.applyDiscount(subtotal);

        if (customer instanceof CorporateCustomer) {
            CorporateCustomer corp = (CorporateCustomer) customer;
            if (!corp.hasSufficientCredit(total)) {
                throw new InsufficientCreditException(String.format(Locale.US,
                        "%s does not have enough credit. Cost: %,.2f, Limit: %,.2f, Outstanding: %,.2f",
                        corp.getFullName(), total, corp.getCreditLimit(), corp.getOutstandingBalance()));
            }
            corp.setOutstandingBalance(corp.getOutstandingBalance() + total);
        }

        RentalAgreement agreement = new RentalAgreement(vehicle, customer, rentalDays);
        vehicle.setStatus(RentalStatus.RENTED);
        agreements.add(agreement);

        if (rentalEventListener != null) {
            rentalEventListener.onRentalCreated(agreement); 
        }
        return agreement;
    }

    public synchronized void returnVehicle(String agreementId) {
        for (RentalAgreement a : agreements) {
            if (a.getAgreementId().equalsIgnoreCase(agreementId)) {
                if (a.getVehicle().getStatus() == RentalStatus.AVAILABLE) {
                    System.out.println("Agreement " + a.getAgreementId() + " was already returned.");
                    return;
                }
                a.getVehicle().setStatus(RentalStatus.AVAILABLE);
                if (a.getCustomer() instanceof CorporateCustomer) {
                    CorporateCustomer corp = (CorporateCustomer) a.getCustomer();
                    corp.setOutstandingBalance(Math.max(0, corp.getOutstandingBalance() - a.getTotalCost()));
                }
                System.out.println("Vehicle " + a.getVehicle().getVehicleId() + " returned successfully.");
                return;
            }
        }
        System.out.println("No rental agreement found with ID: " + agreementId);
    }

    // ---------- Mission 3 ----------
    public FleetSnapshot generateSnapshot() {
        return new FleetSnapshot();
    }

    public synchronized void printFleetStatus() {
        generateSnapshot().printSummary();
    }

    public synchronized void checkOverdueAgreements() {
        for (RentalAgreement a : agreements) {
            if (a.getVehicle().getStatus() == RentalStatus.AVAILABLE) {
                continue; 
            }
            a.setDaysElapsed(a.getDaysElapsed() + 1);
            if (a.getDaysElapsed() > a.getRentalDays()) {
                System.out.println("[OVERDUE ALERT] Agreement " + a.getAgreementId()
                        + " has exceeded its " + a.getRentalDays() + "-day rental period.");
            }
        }
    }

    // ---------- Mission 4 ----------
    public synchronized List<Vehicle> getVehiclesByFuelType(FuelType type) {
        return vehicles.stream()
                .filter(v -> v.getFuelType() == type)
                .collect(Collectors.toList());
    }

    public synchronized List<Customer> getCustomersSortedByName() {
        return customers.stream()
                .sorted(Comparator.comparing(c -> c.getFullName()))
                .collect(Collectors.toList());
    }

    public synchronized double generateRevenueReport() {
        return agreements.stream()
                .mapToDouble(a -> a.getTotalCost())
                .sum();
    }

    // CONCEPT: Interface-based polymorphism through Reportable
    public synchronized void printAllReportables() {
        List<Reportable> reportables = new ArrayList<>();
        reportables.addAll(vehicles);
        reportables.addAll(customers);
        if (reportables.isEmpty()) {
            System.out.println("Nothing to report yet.");
            return;
        }
        for (Reportable item : reportables) {
            System.out.println(item.getReportCategory() + ": " + item.getSummary());
        }
    }

    // CONCEPT: NON-static inner class - reads the outer lists directly
    public class FleetSnapshot {
        private int availableCount;
        private int rentedCount;
        private double totalRevenue;

        public FleetSnapshot() {
            for (Vehicle v : vehicles) {
                if (v.getStatus() == RentalStatus.AVAILABLE) availableCount++;
                else rentedCount++;
            }
            for (RentalAgreement a : agreements) {
                totalRevenue += a.getTotalCost();
            }
        }

        public void printSummary() {
            System.out.printf(Locale.US,
                    "[FLEET STATUS] Available: %d  Rented: %d  Total Revenue So Far: %,.2f%n",
                    availableCount, rentedCount, totalRevenue);
        }
    }
}

// ======================= VehicleStatusMonitor =======================

class VehicleStatusMonitor implements Runnable {
    private final FleetManager fleetManager;

    public VehicleStatusMonitor(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                fleetManager.printFleetStatus();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

// ======================= OverdueReminderWorker =======================
class OverdueReminderWorker implements Runnable {
    private final FleetManager fleetManager;

    public OverdueReminderWorker(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                fleetManager.checkOverdueAgreements();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}