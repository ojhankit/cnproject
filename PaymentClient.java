// PaymentClient.java
import java.io.*;
import java.net.*;
import java.util.*;

public class PaymentClient {
    private static int port;
    private static double balance;
    private static DatagramSocket socket;
    private static final String CLIENTS_FILE = "clients.txt";
    private static final String TRANSACTION_FILE = "transactions.txt";
    private static final String LOGGER_IP = "localhost";
    private static final int LOGGER_PORT = 7000;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // Clear files if first run
        clearFiles();

        System.out.print("Enter your port: ");
        port = Integer.parseInt(sc.nextLine());
        System.out.print("Enter starting balance: ");
        balance = Double.parseDouble(sc.nextLine());

        socket = new DatagramSocket(port);
        addClientToFile(port);

        logToServer("Client started on port: " + port + " with balance: " + balance);

        Thread receiver = new Thread(() -> {
            try {
                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    handleIncomingMessage(message);
                }
            } catch (Exception e) {
                System.out.println("Receiver closed.");
            }
        });
        receiver.start();

        while (true) {
            System.out.println("\n1. Send Payment\n2. Check Balance\n3. Disconnect");
            System.out.print(">> ");
            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 1) {
                System.out.print("Enter receiver port: ");
                int recvPort = Integer.parseInt(sc.nextLine());
                System.out.print("Enter amount: ");
                double amount = Double.parseDouble(sc.nextLine());
                if (balance < amount) {
                    System.out.println("Insufficient balance.");
                    continue;
                }
                String message = port + ":" + recvPort + ":" + amount;
                sendMessage(message, recvPort);
                logToServer("Sent payment request: " + message);
            } else if (choice == 2) {
                System.out.println("Current Balance: " + balance);
            } else if (choice == 3) {
                logToServer("Client on port " + port + " disconnected.");
                socket.close();
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    private static void handleIncomingMessage(String message) throws Exception {
        if (message.startsWith("ACK:")) {
            System.out.println(message);
            return;
        }

        String[] parts = message.split(":");
        if (parts.length != 3) return;
        int senderPort = Integer.parseInt(parts[0]);
        int recvPort = Integer.parseInt(parts[1]);
        double amount = Double.parseDouble(parts[2]);

        if (recvPort != port) return;

        System.out.println("\n? Incoming Payment Request ?\nFrom: " + senderPort + " Amount: " + amount);
        System.out.print(">> Accept (yes/no): ");
        Scanner sc = new Scanner(System.in);
        String decision = sc.nextLine();

        if (decision.equalsIgnoreCase("yes")) {
            balance += amount;
            logToServer("Accepted payment of " + amount + " from " + senderPort);
            addTransaction(senderPort, port, amount);
            sendMessage("ACK: Payment of " + amount + " accepted by " + port, senderPort);
        } else {
            logToServer("Declined payment of " + amount + " from " + senderPort);
        }
    }

    private static void sendMessage(String msg, int targetPort) throws IOException {
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), targetPort);
        socket.send(packet);
    }

    private static void logToServer(String log) throws IOException {
        byte[] data = log.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(LOGGER_IP), LOGGER_PORT);
        socket.send(packet);
    }

    private static void addClientToFile(int port) throws IOException {
        FileWriter writer = new FileWriter(CLIENTS_FILE, true);
        writer.write("Client Port: " + port + "\n");
        writer.close();
    }

    private static void addTransaction(int from, int to, double amt) throws IOException {
        FileWriter writer = new FileWriter(TRANSACTION_FILE, true);
        writer.write("From: " + from + " To: " + to + " Amount: " + amt + "\n");
        writer.close();
    }

    private static void clearFiles() throws IOException {
        new FileWriter(CLIENTS_FILE, false).close();
        new FileWriter(TRANSACTION_FILE, false).close();
        new FileWriter("server_log.txt", false).close();
    }
}
