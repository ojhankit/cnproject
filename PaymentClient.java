// PaymentClient.java
import java.net.*;
import java.util.*;
import java.io.*;

public class PaymentClient {
    private static final String CLIENT_FILE = "clients.txt";
    private static final String TRANSACTION_FILE = "transactions.txt";

    private static int clientPort;
    private static double balance;
    private static DatagramSocket sendSocket;
    private static List<String> pendingRequests = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try {
            clearFileOnStart(CLIENT_FILE);
            clearFileOnStart(TRANSACTION_FILE);

            Scanner scanner = new Scanner(System.in);
            sendSocket = new DatagramSocket();
            InetAddress localhost = InetAddress.getByName("localhost");
            
            System.out.print("Enter your port: ");
            clientPort = scanner.nextInt();

            System.out.print("Enter starting balance: ");
            balance = scanner.nextDouble();
            scanner.nextLine();

            addClientToFile();

            // Start listener thread
            new Thread(() -> listen()).start();

            while (true) {
                System.out.println("\n1. Send Payment");
                System.out.println("2. Check Balance");
                System.out.println("3. Exit");
                System.out.println("4. Respond to Pending Requests");
                System.out.print("Enter choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        System.out.print("Enter receiver port: ");
                        int receiverPort = scanner.nextInt();
                        System.out.print("Enter amount: ");
                        double amount = scanner.nextDouble();
                        scanner.nextLine();

                        String msg = "REQUEST:" + clientPort + ":" + amount;
                        sendPacket(msg, receiverPort);
                        break;

                    case 2:
                        System.out.println("Balance: ₹" + balance);
                        break;

                    case 3:
                        sendSocket.close();
                        System.out.println("Client closed.");
                        System.exit(0);
                        break;

                    case 4:
                        if (pendingRequests.isEmpty()) {
                            System.out.println("No pending requests.");
                        } else {
                            for (String req : new ArrayList<>(pendingRequests)) {
                                System.out.println("Request: " + req);
                                System.out.print("Type 'yes' to accept: ");
                                String res = scanner.nextLine();
                                if (res.equalsIgnoreCase("yes")) {
                                    String[] parts = req.split(":");
                                    int senderPort = Integer.parseInt(parts[1]);
                                    double amt = Double.parseDouble(parts[2]);
                                    balance += amt;
                                    logTransaction(senderPort, clientPort, amt);
                                    sendPacket("ACCEPTED", senderPort);
                                } else {
                                    System.out.println("Rejected.");
                                }
                                pendingRequests.remove(req);
                            }
                        }
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listen() {
        try {
            DatagramSocket receiveSocket = new DatagramSocket(clientPort);
            byte[] buf = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                receiveSocket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());

                if (msg.startsWith("REQUEST")) {
                    pendingRequests.add(msg);
                    System.out.println("\n\uD83D\uDD14 New payment request received. Choose option 4 to respond.");
                } else if (msg.equals("ACCEPTED")) {
                    System.out.println("\n\u2705 Your payment was accepted.");
                    balance -= 40; // Assume 40 for now or track sent value
                }
            }
        } catch (Exception e) {
            System.out.println("Receiver stopped.");
        }
    }

    private static void sendPacket(String msg, int port) throws IOException {
        byte[] data = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("localhost"), port);
        sendSocket.send(packet);
    }

    private static void addClientToFile() throws IOException {
        FileWriter fw = new FileWriter(CLIENT_FILE, true);
        fw.write("Client " + clientPort + " started with balance ₹" + balance + "\n");
        fw.close();
    }

    private static void logTransaction(int from, int to, double amount) throws IOException {
        FileWriter fw = new FileWriter(TRANSACTION_FILE, true);
        fw.write("From: " + from + ", To: " + to + ", Amount: ₹" + amount + "\n");
        fw.close();
    }

    private static void clearFileOnStart(String file) throws IOException {
        FileWriter fw = new FileWriter(file, false); // overwrite mode
        fw.write("");
        fw.close();
    }
}
