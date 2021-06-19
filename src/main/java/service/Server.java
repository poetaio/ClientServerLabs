package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.*;
import db.ProductDBManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import models.AuthUser;
import models.NewProduct;
import models.Product;
import models.User;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Server {

    private static final byte[] JWT_SECRET = "my-long-secret-for-jwt-token-more-secret-fusfkjsflkjsfkljsflksfl".getBytes(StandardCharsets.UTF_8);
    private static final int PORT = 8080;

    private final ProductDBManager dbManager;
    private HttpServer server;

    private static final Pattern GOOD_ID_URI_PATTERN = Pattern.compile("/api/good/[\\d]+", Pattern.CASE_INSENSITIVE);

    public Server() throws IOException {
        dbManager = new ProductDBManager();
        dbManager.initialization("productDB");

        createServer();
    }

    private void createServer() throws IOException {
        InetSocketAddress address = new InetSocketAddress(PORT);
        while (true) {
            try {
                server = HttpServer.create(address, 0);
                break;
            } catch (BindException e) {
                address = new InetSocketAddress(address.getPort() + 1);
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Authenticator authenticator = new Authenticator() {
            @Override
            public Result authenticate(HttpExchange httpExchange) {
                String token = httpExchange.getRequestHeaders().getFirst(("Authorization"));
                if (token != null) {
                    String userLogin = getLoginFromToken(token);
                    User dbUser;
                    try {
                        dbUser = dbManager.getUserByLogin(userLogin);
                    } catch (IllegalArgumentException e) {
                        return new Failure(401);
                    }

                    if (dbUser.getLogin().equals(userLogin)) {
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

        server.createContext("/register", exchange -> {
           if (exchange.getRequestMethod().equals("POST")) {
               try {
                   AuthUser newUser = objectMapper.readValue(exchange.getRequestBody(), AuthUser.class);
                   User user = new User(newUser.getLogin(), base64ToBytes(newUser.getPasswordBase64()));

                   dbManager.insertUser(user);
                   byte[] response = "User successfully registered".getBytes(StandardCharsets.UTF_8);
                   exchange.sendResponseHeaders(200, response.length);
                   exchange.getResponseBody().write(response);
               } catch (IllegalArgumentException e) {
                   byte[] response = e.getMessage().getBytes(StandardCharsets.UTF_8);
                   exchange.sendResponseHeaders(409, response.length);
                   exchange.getResponseBody().write(response);
               } catch (IOException e) {
                   byte[] response = "Bad json".getBytes(StandardCharsets.UTF_8);
                   exchange.sendResponseHeaders(400, response.length);
                   exchange.getResponseBody().write(response);
                   e.printStackTrace();
               }
           } else {
               exchange.sendResponseHeaders(404, 0);
           }
           exchange.close();
        });

        server.createContext("/login", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());

                byte[] response;

                if (!params.containsKey("login") || !params.containsKey("password")) {
                    response = "No login or password provided".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                    return;
                }
                User dbUser;
                try {
                    dbUser = dbManager.getUserByLogin(params.get("login"));
                } catch (IllegalArgumentException e) {
                    exchange.sendResponseHeaders(401, 0);
                    exchange.close();
                    return;
                }

                if (dbUser.getLogin().equals(params.get("login")) && Arrays.equals(dbUser.getPassword(), base64ToBytes(params.get("password")))) {
                    exchange.getResponseHeaders().set("Authorization", createJWT(dbUser.getLogin()));
                    exchange.sendResponseHeaders(200, 0);
                } else {
                    exchange.sendResponseHeaders(401, 0);
                }
            } else {
                    exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        });

        server.createContext("/users", exchange -> {
            if (exchange.getRequestMethod().equals("DELETE")) {
                dbManager.deleteAllUsers();
                exchange.sendResponseHeaders(200, 0);
            } else {
                exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        });

        server.createContext("/api/good", exchange -> {
            if (exchange.getRequestURI().toString().equals("/api/good") && exchange.getRequestMethod().equals("PUT")) {
                byte[] response;
                try {
                    NewProduct newProduct = objectMapper.readValue(exchange.getRequestBody(), NewProduct.class);
                    Product newProductToDb = new Product(null, newProduct.getName(), newProduct.getDescription(), newProduct.getManufacturer(), newProduct.getQuantity(), newProduct.getPrice());
                    Product dbProduct = dbManager.insertProduct(newProductToDb);

                    response = objectMapper.writeValueAsString(dbProduct).getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);

                } catch (IllegalArgumentException e) {
                    response = e.getMessage().getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(409, response.length);
                } catch (IOException e) {
                    response = "Bad JSON".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, response.length);
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, 0);
                }
                exchange.getResponseBody().write(response);
            } else if (GOOD_ID_URI_PATTERN.matcher(exchange.getRequestURI().toString()).matches()) {
                int id = Integer.parseInt(exchange.getRequestURI().toString().split("/")[3]);
                Product dbProduct;
                try {
                     dbProduct = dbManager.getProductById(id);
                } catch (IllegalArgumentException e) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                if (dbProduct != null) {
                    byte[] response;
                    switch (exchange.getRequestMethod()) {
                        case "GET":
                            response = objectMapper.writeValueAsBytes(dbProduct);
                            exchange.getResponseHeaders()
                                    .set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, response.length);
                            exchange.getResponseBody().write(response);
                            break;
                        case "POST":
                            try {
                                NewProduct updateProduct = objectMapper.readValue(exchange.getRequestBody(), NewProduct.class);
                                Product updateProductToDb = new Product(updateProduct.getName(), updateProduct.getDescription(), updateProduct.getManufacturer(), updateProduct.getQuantity(), updateProduct.getPrice());
                                dbManager.updateProductById(id, updateProductToDb);
                                exchange.sendResponseHeaders(200, 0);
                            } catch (IllegalArgumentException e) {
                                response = e.getMessage().getBytes(StandardCharsets.UTF_8);
                                exchange.sendResponseHeaders(409, response.length);
                                exchange.getResponseBody().write(response);
                            } catch (IOException e) {
                                response = "Bad JSON".getBytes(StandardCharsets.UTF_8);
                                exchange.sendResponseHeaders(400, response.length);
                                exchange.getResponseBody().write(response);
                            }
                            break;
                        case "DELETE":
                            try {
                                dbManager.deleteProductById(id);
                                exchange.sendResponseHeaders(204, -1);
                            } catch (IllegalArgumentException e) {
                                exchange.sendResponseHeaders(404, 0);
                            }
                            break;
                        default:
                            exchange.sendResponseHeaders(405, 0);
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
                    byte[] response = objectMapper.writeValueAsBytes(dbManager.getAllProducts());
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    break;
                case "DELETE":
                    dbManager.deleteAllProducts();
                    exchange.sendResponseHeaders(200, 0);
                    break;
                default:
                    exchange.sendResponseHeaders(405, 0);
            }
            exchange.close();
        }).setAuthenticator(authenticator);

        server.start();
        System.out.println("Server listens on port " + address.getPort());
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

    private static Map<String, String> queryToMap(String query) {
        if(query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private static String bytesToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] base64ToBytes(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }


    public static void main(String[] args) throws IOException {
        new Server();
    }
}
