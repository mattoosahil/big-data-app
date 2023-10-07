package com.bigdata.app.services;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.InputStream;


@Service
public class JsonSchemaService {

    final static String SCHEMA_PATH = "/schemas/schema.json";

    public boolean validateSchema(JSONObject data) throws FileNotFoundException {
        InputStream inputStream = getClass().getResourceAsStream(SCHEMA_PATH);
        JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));
        Schema jsonSchema = SchemaLoader.load(jsonObject);
        try {
            jsonSchema.validate(data);
            return true;
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        return false;
    }
}
