package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClientBuilder;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServerTest {
    private static Server server;
    private static String userToken;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String url;

    static {
        try {
            server = new Server();
            url = "localhost:" + server.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void shouldCleanAllUsersRegisterAndLoginUser() throws IOException, URISyntaxException {
        // clear all users
        HttpUriRequest requestClearAll = RequestBuilder.create("DELETE")
                .setUri("http://" + url + "/users")
                .build();

        HttpResponse httpResponseClearAll = HttpClientBuilder.create().build().execute( requestClearAll );

        assertEquals(HttpStatus.SC_OK, httpResponseClearAll.getStatusLine().getStatusCode());

        // register user
        HttpUriRequest requestRegister = RequestBuilder.create("POST")
                .setUri("http://" + url + "/register")
                .setEntity(new StringEntity("{\"login\":\"login\",\"passwordBase64\":\"cGFzc3dvcmRtag==\"}", ContentType.APPLICATION_JSON))
                .build();

        HttpResponse httpResponseRegister = HttpClientBuilder.create().build().execute( requestRegister );

        assertEquals(HttpStatus.SC_OK, httpResponseRegister.getStatusLine().getStatusCode());

        // login user
        URIBuilder builder = new URIBuilder();
        builder.setScheme("http").setHost(url).setPath("/login")
                .setParameter("login", "login")
                .setParameter("password", "cGFzc3dvcmRtag==");
        URI uri = builder.build();

        HttpUriRequest requestLogin = RequestBuilder.create("POST")
                .setUri(uri)
                .setHeader("Content-Length", "0")
                .build();

        HttpResponse responseLogin = HttpClientBuilder.create().build().execute( requestLogin );

        assertEquals(HttpStatus.SC_OK, responseLogin.getStatusLine().getStatusCode());

        userToken = Arrays.stream(responseLogin.getAllHeaders()).filter(e -> e.getName().equals("Authorization")).findFirst().get().getValue();
    }

    @Test
    public void serverIsUp()
            throws ClientProtocolException, IOException {
        HttpUriRequest request = new HttpGet("http://" + url);

        HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request );

        assertEquals(httpResponse.getStatusLine().getStatusCode(),
                HttpStatus.SC_OK);
    }
}
