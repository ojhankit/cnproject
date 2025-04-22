import java.io.*;
import java.net.*;
import java.util.*;

public class PaymentClient {
    private static final int BUFFER_SIZE = 1024;
    private static final int ACK_TIMEOUT = 3000; // milliseconds
    private static final int MAX_RETRIES = 3;

    private DatagramSocket socket;
    private int port;
    private double balance;
    private String walletId;
    private int sequenceNumber = 0;
    private Scanner scanner;

    public PaymentClient(int port, double initialBalance) throws SocketException {
        this.port = port;
        this.balance = initialBalance;
        this.walletId = UUID.randomUUID().toString().substring(0, 8); // Simulated wallet ID
        this.socket = new DatagramSocket(port);
        this.scanner = new Scanner(System.in);

        // Start listener in a separate thread
        new Thread(this::listen).start();
    }

    private void listen() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                if (received.startsWith("PAY:")) {
                    processPayment(packet, received);
                } else if (received.startsWith("ACK:")) {
                    processAck(received);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processPayment(DatagramPacket packet, String message) throws IOException {
        String[] parts = message.split(":");
        int seq = Integer.parseInt(parts[1]);
        String fromWallet = parts[2];
        double amount = Double.parseDouble(parts[3]);

        balance += amount;

        System.out.printf("\nReceived payment of $%.2f from %s | New Balance: $%.2f\n", amount, fromWallet, balance);

        // Send ACK
        String ack = "ACK:" + seq;
        DatagramPacket ackPacket = new DatagramPacket(
                ack.getBytes(), ack.length(),
                packet.getAddress(), packet.getPort());
        socket.send(ackPacket);
    }

    private void processAck(String message) {
        System.out.println("ACK received: " + message);
    }

    private boolean sendPayment(String targetIP, int targetPort, double amount) throws IOException {
        if (amount > balance) {
            System.out.println("Insufficient balance.");
            return false;
        }

        String paymentMsg = "PAY:" + sequenceNumber + ":" + walletId + ":" + amount;
        InetAddress address = InetAddress.getByName(targetIP);
        DatagramPacket packet = new DatagramPacket(paymentMsg.getBytes(), paymentMsg.length(), address, targetPort);

        int attempts = 0;
        socket.setSoTimeout(ACK_TIMEOUT);
        boolean acknowledged = false;

        while (attempts < MAX_RETRIES && !acknowledged) {
            socket.send(packet);
            try {
                byte[] ackBuffer = new byte[BUFFER_SIZE];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                socket.receive(ackPacket);

                String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if (ack.equals("ACK:" + sequenceNumber)) {
                    acknowledged = true;
                    balance -= amount;
                    System.out.printf("Payment of $%.2f to %s:%d confirmed.\n", amount, targetIP, targetPort);
                    sequenceNumber++;
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout. Retrying...");
                attempts++;
            }
        }

        if (!acknowledged) {
            System.out.println("Payment failed after retries.");
        }

        socket.setSoTimeout(0); // remove timeout
        return acknowledged;
    }

    public void start() {
        System.out.println("Your wallet ID: " + walletId);
        System.out.printf("Listening on port %d | Current balance: $%.2f\n", port, balance);

        while (true) {
            System.out.println("\n1. Send Payment\n2. Check Balance\n3. Exit");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();  // Read choice
            scanner.nextLine();  // Consume the newline character left by nextInt()

            try {
                if (choice == 1) {
                    System.out.print("Enter target IP: ");
                    String ip = scanner.nextLine();
                    System.out.print("Enter target port: ");
                    int tgtPort = scanner.nextInt();
                    System.out.print("Enter amount: ");
                    double amount = scanner.nextDouble();
                    scanner.nextLine(); // Consume the newline

                    sendPayment(ip, tgtPort, amount);

                } else if (choice == 2) {
                    System.out.printf("Your balance: $%.2f\n", balance);
                } else if (choice == 3) {
                    System.out.println("Exiting.");
                    socket.close();
                    break;  // Break the loop to exit
                } else {
                    System.out.println("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                scanner.nextLine(); // Clear the input buffer in case of exception
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter your port: ");
        int port = input.nextInt();
        System.out.print("Enter starting balance: ");
        double balance = input.nextDouble();

        PaymentClient client = new PaymentClient(port, balance);
        client.start();
    }
}
