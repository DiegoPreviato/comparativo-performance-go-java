import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static long fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        // Serve OpenAPI definition and a small Swagger UI wrapper
        server.createContext("/openapi.json", new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] content = null;
                try {
                    content = Files.readAllBytes(Paths.get("openapi.json"));
                } catch (IOException e) {
                    // fallback to classpath resource if file not found or not accessible
                    InputStream ris = Main.class.getResourceAsStream("/openapi.json");
                    if (ris != null) {
                        try (InputStream is = ris) {
                            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) != -1) {
                                tmp.write(buf, 0, r);
                            }
                            content = tmp.toByteArray();
                        } catch (IOException ex) {
                            content = null;
                        }
                    }
                }

                if (content == null) {
                    String msg = "openapi.json not found (looked on filesystem and classpath)";
                    System.out.println(msg);
                    byte[] err = msg.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(500, err.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(err);
                    }
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            }
        });

        server.createContext("/swagger", new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] content = null;
                try {
                    content = Files.readAllBytes(Paths.get("swagger.html"));
                } catch (IOException e) {
                    InputStream ris = Main.class.getResourceAsStream("/swagger.html");
                    if (ris != null) {
                        try (InputStream is = ris) {
                            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                            byte[] buf = new byte[8192];
                            int r;
                            while ((r = is.read(buf)) != -1) {
                                tmp.write(buf, 0, r);
                            }
                            content = tmp.toByteArray();
                        } catch (IOException ex) {
                            content = null;
                        }
                    }
                }

                if (content == null) {
                    String msg = "swagger.html not found (looked on filesystem and classpath)";
                    System.out.println(msg);
                    byte[] err = msg.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(500, err.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(err);
                    }
                    return;
                }

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            }
        });
        server.createContext("/fibonacci", new FibonacciHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Java server listening on port 8081");
    }

    static class FibonacciHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            // Parse request body (Java 8 compatible)
            InputStream is = exchange.getRequestBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            String body = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            System.out.println("Received body: " + body);

            // Robust JSON parsing with regex
            int iterations = 0;
            int n = 0;
            try {
                Pattern iterationsPattern = Pattern.compile("\"iterations\"\\s*:\\s*(\\d+)");
                Matcher iterationsMatcher = iterationsPattern.matcher(body);
                if (iterationsMatcher.find()) {
                    iterations = Integer.parseInt(iterationsMatcher.group(1));
                }

                Pattern nPattern = Pattern.compile("\"n\"\\s*:\\s*(\\d+)");
                Matcher nMatcher = nPattern.matcher(body);
                if (nMatcher.find()) {
                    n = Integer.parseInt(nMatcher.group(1));
                }
            } catch (Exception e) {
                System.out.println("Error parsing JSON: " + e.getMessage());
                exchange.sendResponseHeaders(400, -1); // Bad Request
                return;
            }
            
            System.out.println("Parsed values: iterations = " + iterations + ", n = " + n);

            if (iterations == 0 || n == 0) {
                System.out.println("Error: iterations or n is 0, check if JSON is parsed correctly.");
                String errorResponse = "{\"error\":\"Failed to parse iterations or n from request body.\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, errorResponse.length());
                OutputStream os = exchange.getResponseBody();
                os.write(errorResponse.getBytes());
                os.close();
                return;
            }

            Instant start = Instant.now();
            long result = 0;
            for (int i = 0; i < iterations; i++) {
                result = fibonacci(n);
            }
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            String response = String.format(
                "{\"result\":%d,\"execution_time_ms\":%d}",
                result,
                duration.toMillis()
            );

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
