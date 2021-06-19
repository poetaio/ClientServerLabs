package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.http.impl.client.HttpClientBuilder;

import java.awt.*;
import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerTest {
    private static Server server;
    private static String userToken;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String url;

    static {
        try {
            server = new Server();
            url = "http://localhost:" + server.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void shouldCleanAllUsersRegisterAndLoginUser() throws IOException {
        // clear all users
        HttpUriRequest requestClearAll = RequestBuilder.create("DELETE")
                .setUri(url + "/users")
                .build();

        HttpResponse httpResponseClearAll = HttpClientBuilder.create().build().execute( requestClearAll );

        assertEquals(HttpStatus.SC_OK, httpResponseClearAll.getStatusLine().getStatusCode());

        // register user
        HttpUriRequest requestRegister = RequestBuilder.create("POST")
                .setUri(url + "/register")
                .setEntity(new StringEntity("{\"login\":\"login\",\"passwordBase64\":\"cGFzc3dvcmRtag==\"}", ContentType.APPLICATION_JSON))
                .build();

        HttpResponse httpResponseRegister = HttpClientBuilder.create().build().execute( requestRegister );

        assertEquals(HttpStatus.SC_OK, httpResponseRegister.getStatusLine().getStatusCode());

//        // login user
//        HttpUriRequest requestLogin = RequestBuilder.create("POST")
//                .setUri(url + "/login")
//                .addParameter("login", "login")
//                .addParameter("password","cGFzc3dvcmRtag==")
//                .build();
//
//        HttpResponse responseLogin = HttpClientBuilder.create().build().execute( requestLogin );
//
//        assertEquals(HttpStatus.SC_OK, responseLogin.getStatusLine().getStatusCode());
//
//        userToken = responseLogin.getEntity().toString();
//        System.out.println(userToken);
    }

    @Test
    public void serverIsUp()
            throws ClientProtocolException, IOException {
        HttpUriRequest request = new HttpGet(url);

        HttpResponse httpResponse = HttpClientBuilder.create().build().execute( request );

        assertEquals(httpResponse.getStatusLine().getStatusCode(),
                HttpStatus.SC_OK);
    }
}
