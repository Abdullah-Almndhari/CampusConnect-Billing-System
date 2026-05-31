package campusconnect.client;

import campusconnect.common.BillingRequest;
import campusconnect.common.BillingResponse;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

// ClientWorker class implementing Runnable for multi-threaded client execution
public class ClientWorker implements Runnable {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    @Override
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            int studentId = readStudentId(scanner);
            String serviceCode = readServiceCode(scanner);
            String priorityType = readPriorityType(scanner);

            BillingRequest request = new BillingRequest(studentId, serviceCode, priorityType);

            try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                 ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

                output.writeObject(request);
                output.flush();

                BillingResponse response = (BillingResponse) input.readObject();
                displayResponse(response);
            }
        } catch (Exception exception) {
            System.out.println("Client error: " + exception.getMessage());
        }
    }

    private int readStudentId(Scanner scanner) {
        while (true) {
            System.out.print("Enter student id: ");
            String value = scanner.nextLine().trim();
            try {
                int id = Integer.parseInt(value);
                if (id > 0) {
                    return id;
                }
                System.out.println("Student id must be a positive number.");
            } catch (NumberFormatException exception) {
                System.out.println("Invalid id. Please enter digits only.");
            }
        }
    }

    private String readServiceCode(Scanner scanner) {
        while (true) {
            System.out.print("Enter service code (FD100/TR200/PR300/EV400/ST500): ");
            String code = scanner.nextLine().trim().toUpperCase();
            if (code.matches("FD100|TR200|PR300|EV400|ST500")) {
                return code;
            }
            System.out.println("Invalid service code. Try again.");
        }
    }

    private String readPriorityType(Scanner scanner) {
        while (true) {
            System.out.print("Enter priority type (Normal/Express): ");
            String priority = scanner.nextLine().trim();
            if (priority.equalsIgnoreCase("Normal") || priority.equalsIgnoreCase("Express")) {
                return priority.substring(0, 1).toUpperCase() + priority.substring(1).toLowerCase();
            }
            System.out.println("Priority type must be Normal or Express.");
        }
    }

    private void displayResponse(BillingResponse response) {
        if (!response.isSuccess()) {
            System.out.println("Server message: " + response.getMessage());
            return;
        }

        System.out.println("\nCampusConnect Oman Billing Result");
        System.out.println("--------------------------------");
        System.out.println("Student ID: " + response.getStudent().getId());
        System.out.println("Student Name: " + response.getStudent().getName());
        System.out.println("Age: " + response.getStudent().getAge());
        System.out.println("Scholarship Type: " + response.getStudent().getScholarshipType());
        System.out.println("Service: " + response.getService().getDescription());
        System.out.printf("Service Amount: %.2f OMR%n", response.getService().getBaseFee());
        System.out.printf("Scholarship Discount: %.2f OMR%n", response.getScholarshipDiscount());
        System.out.printf("Age Adjustment: %.2f OMR%n", response.getAgeAdjustment());
        System.out.printf("Priority Extra Charge: %.2f OMR%n", response.getPriorityCharge());
        System.out.printf("Final Bill Amount: %.2f OMR%n", response.getFinalAmount());
        System.out.println("Expected Delivery Time: " + response.getExpectedDeliveryTime());
    }
}
