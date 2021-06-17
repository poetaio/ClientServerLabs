package server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MyServer {

    private static final byte[] JWT_SECRET = "my-long-secret-for-jwt-token-more-secret-fusfkjsflkjsfkljsflksfl".getBytes(StandardCharsets.UTF_8);
    private static final int PORT = 8888;
    private static int nextID = 1000;

    private static final HashMap<Integer, Product> PRODUCT_MAP = new HashMap<Integer, Product>() { {
        put(1, new Product(1, "pr1"));
        put(74, new Product(74,"pr2"));
        put(10, new Product(10,"pr3"));
    }};

    private static final Pattern GOOD_ID_URI_PATTERN = Pattern.compile("/api/good/[\\d]+", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Authenticator authenticator = new Authenticator() {
            @Override
            public Result authenticate(HttpExchange httpExchange) {
                String token = httpExchange.getRequestHeaders().getFirst(("Authorization"));
                if (token != null) {
                    String userLogin = getLoginFromToken(token);
                    User user = new User("log", "pass");
                    if (user.getLogin().equals(userLogin)) {
                        return new Success(new HttpPrincipal(userLogin, "dummy"));
                    }
                }
                return new Failure(401);
            }
        };

        server.createContext("/", exchange -> {
            if (exchange.getRequestURI().toString().equals("/")) {
                byte[] response = "{\"status\": \"OK\"}".getBytes();
                exchange.getResponseHeaders()
                        .set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
            exchange.close();
        });

        server.createContext("/login", exchange -> {
            switch (exchange.getRequestMethod()) {
                case "POST":
                    User user = objectMapper.readValue(exchange.getRequestBody(), User.class);
//                  server.User fromDb = sqlTest.getUserByLogin(user.getLogin());
                    User fromDb = new User("log", "pass");

                    if (fromDb.getLogin().equals(user.getLogin()) && fromDb.getPassword().equals(user.getPassword())) {
                        exchange.getResponseHeaders().set("Authorization", createJWT(user.getLogin()));
                        exchange.sendResponseHeaders(200, 0);
                    } else
                        exchange.sendResponseHeaders(401, 0);

                    exchange.getRequestBody();
                    exchange.sendResponseHeaders(200, 0);
                    break;
                case "GET":
                    exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        }).setAuthenticator(authenticator);

        server.createContext("/api/good", exchange -> {
            if (exchange.getRequestURI().toString().equals("/api/good") && exchange.getRequestMethod().equals("PUT")) {
                try {
                    Product newProduct = objectMapper.readValue(exchange.getRequestBody(), Product.class);

                    PRODUCT_MAP.put(newProduct.getId(), newProduct);
                    exchange.sendResponseHeaders(200, 0);

                    } catch (IOException e) {
                        byte[] response = "Bad JSON".getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(409, response.length);
                        exchange.getResponseBody().write(response);
                        e.printStackTrace();
                    }
            } else if (GOOD_ID_URI_PATTERN.matcher(exchange.getRequestURI().toString()).matches()) {
                int id = Integer.parseInt(exchange.getRequestURI().toString().split("/")[3]);
                Product dbProduct = PRODUCT_MAP.get(id);

                if (dbProduct != null) {
                    switch (exchange.getRequestMethod()) {
                        case "GET":
                            byte[] response = objectMapper.writeValueAsBytes(dbProduct);
                            exchange.getResponseHeaders()
                                    .set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, response.length);
                            exchange.getResponseBody().write(response);
                            break;
                        case "POST":
                            try {
                                Product updateProduct = objectMapper.readValue(exchange.getRequestBody(), Product.class);
                                if (updateProduct.getName() != null)
                                    dbProduct.setName(updateProduct.getName());

                                exchange.sendResponseHeaders(200, 0);

                            } catch (IOException e) {
                                byte[] responseUpdate = "Bad JSON".getBytes(StandardCharsets.UTF_8);
                                exchange.sendResponseHeaders(409, responseUpdate.length);
                                exchange.getResponseBody().write(responseUpdate);
                                e.printStackTrace();
                            }
                            // todo: 409 error on validation

//                            List<String> changes = new LinkedList<>();
//                            if (updateProduct.getName() != null)
//                                changes.add((changes.size() > 0 ? "," : "") + " name = " + updateProduct.getName() + " ");

                            break;
                        case "DELETE":
                            PRODUCT_MAP.remove(id);
                            exchange.sendResponseHeaders(200, 0);
                            break;
                        default:
                            exchange.sendResponseHeaders(404, 0);
                    }
                } else {
                    exchange.sendResponseHeaders(404, 0);
                }
            } else {
                exchange.sendResponseHeaders(404, 0);
            }
            exchange.close();
        }).setAuthenticator(authenticator);

        server.createContext("/api/goods", exchange -> {
            switch (exchange.getRequestMethod()) {
                case "GET":
                    byte[] response = objectMapper.writeValueAsBytes(PRODUCT_MAP);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    break;
                case "DELETE":
                    // todo: delete all goods
                    break;
            }
            exchange.close();
        }).setAuthenticator(authenticator);
        server.start();
    }

    private static String createJWT(String userLogin) {

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        Key signingKey = new SecretKeySpec(JWT_SECRET, signatureAlgorithm.getJcaName());

        return Jwts.builder()
                .setIssuedAt(now)
                .setSubject(userLogin)
                .setExpiration(new Date(now.getTime() + TimeUnit.HOURS.toMillis(10)))
                .signWith(signingKey, signatureAlgorithm)
                .compact();
    }

    private static String getLoginFromToken(String jwt) {
        Claims claims = Jwts.parser()
                .setSigningKey(new SecretKeySpec(JWT_SECRET, SignatureAlgorithm.HS256.getJcaName()))
                .parseClaimsJws(jwt).getBody();
        return claims.getSubject();
    }
}
