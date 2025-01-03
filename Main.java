import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Configure scanner
            int threads = Runtime.getRuntime().availableProcessors() * 4;  // Optimize thread count
            int timeout = 200;  // Milliseconds
            PortScanner scanner = new PortScanner(threads, timeout);

            // Define targets
            List<String> hosts = Arrays.asList(
                "example.com",
                "localhost"
                // Add more hosts as needed
            );

            // Perform scan
            System.out.println("Starting scan...");
            List<ScanResult> results = scanner.scan(hosts, 1, 1000);

            // Print results
            System.out.println("Scan completed. Found " + results.size() + " open ports:");
            results.forEach(System.out::println);

            // Cleanup
            scanner.shutdown();
        } catch (Exception e) {
            System.err.println("Scan failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
