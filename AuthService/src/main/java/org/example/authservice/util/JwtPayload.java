package org.example.authservice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class JwtPayload {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String getSub(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(payloadJson);
            return node.get("sub").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse JWT payload", e);
        }
    }
}
