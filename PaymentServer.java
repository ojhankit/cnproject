import java.io.*;
import java.net.*;
import java.util.*;

public class PaymentServer {
    private static final int BUFFER_SIZE = 1024;
    private DatagramSocket socket;
    private int port;

    public PaymentServer(int port) throws SocketException {
        this.port = port;
        this.socket = new DatagramSocket(port);
        System.out.println("Server listening on port " + port);
    }

    public void start() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received);

                if (received.startsWith("PAY:")) {
                    handlePayment(packet, received);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handlePayment(DatagramPacket packet, String message) throws IOException {
        String[] parts = message.split(":");
        int seq = Integer.parseInt(parts[1]);
        String fromWallet = parts[2];
        double amount = Double.parseDouble(parts[3]);

        System.out.printf("Payment of $%.2f received from %s | Processing payment...\n", amount, fromWallet);

        // Acknowledge the payment
        String ack = "ACK:" + seq;
        DatagramPacket ackPacket = new DatagramPacket(
                ack.getBytes(), ack.length(),
                packet.getAddress(), packet.getPort());
        socket.send(ackPacket);
        System.out.println("Sent acknowledgment for sequence " + seq);
    }

    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter server port: ");
        int port = input.nextInt();

        PaymentServer server = new PaymentServer(port);
        server.start();
    }
}
