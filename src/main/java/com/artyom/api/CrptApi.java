package com.artyom.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private static final String AUTH_URL = "https://ismp.crpt.ru/api/v3/auth/cert/key";
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final TimeUnit timeUnit;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    public static void main(String[] args) {
        Service service = new Service();
        Token token = service.getTokenByUrl(AUTH_URL);
        System.out.println(token);
    }
}

class Service {
    public Token getTokenByUrl(String url) {
        Token token = null;
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Content-type", "application/json");
            int code = urlConnection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                BufferedReader buff = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                while ((line = buff.readLine()) != null)
                    token = new ObjectMapper().readValue(line, Token.class);
                buff.close();
            } else {
                System.out.println("Another code: " + code);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return token;
    }
}

@Getter
@Setter
class Token {
    @JsonProperty("uuid")
    private UUID uuid;
    @JsonProperty("data")
    private String data;

    @Override
    public String toString() {
        return "Token{" +
                "uuid=" + uuid +
                ", data='" + data + '\'' +
                '}';
    }
}