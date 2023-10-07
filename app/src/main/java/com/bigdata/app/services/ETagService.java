package com.bigdata.app.services;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


@Service
public class ETagService {

    public String eTagGen(JSONObject json) {
        String encodedETag = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(json.toString().getBytes(StandardCharsets.UTF_8));
            encodedETag = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException noSuchEx) {
            noSuchEx.printStackTrace();
        }
        return "\"" + encodedETag + "\"";
    }

}
