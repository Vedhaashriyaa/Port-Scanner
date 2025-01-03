import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class PortScanner {
    private static final Logger LOGGER = Logger.getLogger(PortScanner.class.getName());
    private final ExecutorService executorService;
    private final int timeout;

    public PortScanner(int threads, int timeout) {
        this.executorService = Executors.newFixedThreadPool(threads);
        this.timeout = timeout;
        setupLogger();
    }

    private void setupLogger() {
        try {
            String logFileName = "port_scan_" +
                LocalDateTime.now().toString().replace(":", "-")
                    .replace(".", "-") + ".log";
            FileHandler fh = new FileHandler(logFileName);
            LOGGER.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e) {
            LOGGER.severe("Failed to setup logger: " + e.getMessage());
        }
    }

    public List<ScanResult> scan(List<String> hosts, int startPort, int endPort) {
        if (hosts == null || hosts.isEmpty()) {
            throw new IllegalArgumentException("Hosts list cannot be null or empty");
        }
        if (startPort < 1 || endPort > 65535 || startPort > endPort) {
            throw new IllegalArgumentException("Invalid port range");
        }

        List<Future<ScanResult>> futures = new ArrayList<>();
        List<ScanResult> results = new ArrayList<>();

        for (String host : hosts) {
            if (host == null || host.trim().isEmpty()) {
                LOGGER.warning("Skipping null or empty host");
                continue;
            }

            for (int port = startPort; port <= endPort; port++) {
                final int currentPort = port;
                futures.add(executorService.submit(() -> scanPort(host.trim(), currentPort)));
            }
        }

        for (Future<ScanResult> future : futures) {
            try {
                ScanResult result = future.get(timeout * 2, TimeUnit.MILLISECONDS);
                if (result.isOpen()) {
                    results.add(result);
                    LOGGER.info(result.toString());
                }
            } catch (TimeoutException e) {
                LOGGER.warning("Scan timed out: " + e.getMessage());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Scan failed", e);
            }
        }

        return results;
    }

    private ScanResult scanPort(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return new ScanResult(host, port, true);
        } catch (IOException e) {
            return new ScanResult(host, port, false);
        }
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
