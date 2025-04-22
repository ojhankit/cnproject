import java.net.*;
import java.util.*;
import java.io.*;

public class PaymentClient {

    private static final String CLIENT_FILE = "clients.txt"; // Clients record file

    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 5000;

            // Get the unique port number for each client (can be assigned dynamically)
            System.out.print("Enter your port: ");
            int clientPort = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            // Add client info to the clients.txt file
            addClientToFile(clientPort);

            while (true) {
                System.out.println("\n1. Send Payment");
                System.out.println("2. Check Balance");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();

                if (choice == 1) {
                    System.out.print("Enter recipient's port: ");
                    int recipientPort = scanner.nextInt();
                    System.out.print("Enter amount to send: ");
                    double amount = scanner.nextDouble();
                    scanner.nextLine(); // Consume newline

                    String message = clientPort + ":" + recipientPort + ":" + amount;
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), serverAddress, serverPort);
                    clientSocket.send(sendPacket);
                    System.out.println("Sent payment request to server...");

                    // Receive response from server (confirmation of sending)
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    clientSocket.receive(receivePacket);
                    String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    System.out.println("Server response: " + serverResponse);
                } else if (choice == 2) {
                    System.out.println("Your current balance: 100.00"); // Static balance for now
                } else if (choice == 3) {
                    System.out.println("Exiting...");
                    break;
                } else {
                    System.out.println("Invalid option, try again.");
                }
            }

            clientSocket.close();
            scanner.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to add client details to clients.txt
    private static void addClientToFile(int clientPort) throws IOException {
        FileWriter clientWriter = new FileWriter(CLIENT_FILE, true);
        clientWriter.write("Client Port: " + clientPort + ", Balance: 100.00\n");
        clientWriter.close();
        System.out.println("Client added to " + CLIENT_FILE);
    }
}
