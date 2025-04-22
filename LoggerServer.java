import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class LoggerServer {
    private static final int PORT = 7000;
    private static final String LOG_FILE = "server_log.txt";

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("Logger Server started on port " + PORT);
            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String logMessage = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Log received: " + logMessage);
                writeLogToFile(logMessage);
            }
        } catch (IOException e) {
            System.out.println("Logger Server error: " + e.getMessage());
        }
    }

    private static void writeLogToFile(String logMessage) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(logMessage + "\n");
        } catch (IOException e) {
            System.out.println("Error writing log: " + e.getMessage());
        }
    }
}
