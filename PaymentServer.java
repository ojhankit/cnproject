import java.net.*;
import java.io.*;
import java.util.*;

public class PaymentServer {

    private static final String CLIENT_FILE = "clients.txt"; // Clients record file
    private static final String TRANSACTION_FILE = "transactions.txt"; // Transactions record file

    public static void main(String[] args) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(5000); // Listening on port 5000
            System.out.println("Server is running on port 5000");

            // Create or open the clients.txt file to store client information
            FileWriter clientWriter = new FileWriter(CLIENT_FILE, true);
            // Create or open the transactions.txt file to store transaction logs
            FileWriter transactionWriter = new FileWriter(TRANSACTION_FILE, true);

            // Set for unique client ports
            Set<Integer> clientPorts = new HashSet<>();

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                // Get the incoming message from the client
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("Received message from client: " + message);

                // Store client information if new client
                if (!clientPorts.contains(clientPort)) {
                    clientPorts.add(clientPort);
                    clientWriter.write("Client Port: " + clientPort + ", Balance: 100.00\n");
                    System.out.println("New client added: Port " + clientPort);
                }

                // Process message and forward it to the recipient client
                String[] parts = message.split(":");
                if (parts.length == 3) {
                    String senderPort = parts[0];
                    String receiverPort = parts[1];
                    String amount = parts[2];

                    // Now forward this message to the receiver client
                    InetAddress receiverAddress = InetAddress.getByName("localhost");
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(),
                            receiverAddress, Integer.parseInt(receiverPort));
                    serverSocket.send(sendPacket);
                    System.out.println("Forwarded payment request to client on port " + receiverPort);

                    // Log the transaction in the transactions.txt file
                    transactionWriter.write("Sender Port: " + senderPort + ", Receiver Port: " + receiverPort + ", Amount: " + amount + "\n");
                    System.out.println("Transaction logged.");
                } else {
                    System.out.println("Invalid message format received.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
