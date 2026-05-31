package campusconnect.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import campusconnect.common.BillingRequest;
import campusconnect.common.BillingResponse;
import campusconnect.common.ServiceItem;
import campusconnect.common.Student;

// 1. Main class to initialize and run the Server
public class BillingServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("CampusConnect billing server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (Exception exception) {
            System.out.println("Server error: " + exception.getMessage());
        }
    }
}

// 2. ClientHandler class to handle multiple client connections (Multithreading)
class ClientHandler extends Thread {
    private final Socket socket;
    private final DatabaseManager databaseManager;
    private final BillingCalculator billingCalculator;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.databaseManager = new DatabaseManager();
        this.billingCalculator = new BillingCalculator();
    }

    @Override
    public void run() {
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {

            BillingRequest request = (BillingRequest) input.readObject();
            BillingResponse response = processRequest(request);

            output.writeObject(response);
            output.flush();
        } catch (Exception exception) {
            System.out.println("Server client-handler error: " + exception.getMessage());
        } finally {
            try {
                socket.close();
            } catch (Exception exception) {
                System.out.println("Unable to close socket: " + exception.getMessage());
            }
        }
    }

    private BillingResponse processRequest(BillingRequest request) {
        try {
            Student student = databaseManager.findStudent(request.getStudentId());
            if (student == null) {
                return BillingResponse.error("Student id not found.");
            }

            ServiceItem service = databaseManager.findService(request.getServiceCode());
            if (service == null) {
                return BillingResponse.error("Service code not found.");
            }

            return billingCalculator.calculate(student, service, request.getPriorityType());
        } catch (Exception exception) {
            return BillingResponse.error("Unable to calculate bill: " + exception.getMessage());
        }
    }
}

// 3. DatabaseManager class to handle JDBC connections and queries
class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/campusconnect_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public Student findStudent(int id) throws SQLException {
        String sql = "SELECT id, name, age, scholarship_type FROM Student WHERE id = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Student(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("age"),
                            resultSet.getString("scholarship_type"));
                }
            }
        }
        return null;
    }

    public ServiceItem findService(String serviceCode) throws SQLException {
        String sql = "SELECT service_code, description, base_fee FROM Service WHERE service_code = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serviceCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new ServiceItem(
                            resultSet.getString("service_code"),
                            resultSet.getString("description"),
                            resultSet.getDouble("base_fee"));
                }
            }
        }
        return null;
    }
}

// 4. BillingCalculator class to apply business rules and calculate final amounts
class BillingCalculator {
    public BillingResponse calculate(Student student, ServiceItem service, String priorityType) {
        double baseFee = service.getBaseFee();
        double scholarshipDiscount = baseFee * getScholarshipRate(student.getScholarshipType());
        double amountAfterScholarship = baseFee - scholarshipDiscount;
        double ageAdjustment = amountAfterScholarship * getAgeRate(student.getAge());
        double amountAfterAge = amountAfterScholarship + ageAdjustment;
        double priorityCharge = amountAfterAge * getPriorityRate(priorityType);
        double finalAmount = amountAfterAge + priorityCharge;
        String deliveryTime = priorityType.equalsIgnoreCase("Express") ? "15-20 minutes" : "30-45 minutes";

        return new BillingResponse(true, "Billing calculated successfully", student, service,
                scholarshipDiscount, ageAdjustment, priorityCharge, finalAmount, deliveryTime);
    }

    private double getScholarshipRate(String scholarshipType) {
        if (scholarshipType.equalsIgnoreCase("Dean's List")) {
            return 0.15;
        }
        if (scholarshipType.equalsIgnoreCase("Economically Weak")) {
            return 0.20;
        }
        if (scholarshipType.equalsIgnoreCase("Employee Special Support")) {
            return 0.10;
        }
        return 0.0;
    }

    private double getAgeRate(int age) {
        if (age <= 18) {
            return -0.05;
        }
        if (age > 23) {
            return 0.05;
        }
        return 0.0;
    }

    private double getPriorityRate(String priorityType) {
        if (priorityType.equalsIgnoreCase("Express")) {
            return 0.10;
        }
        return 0.0;
    }
}
