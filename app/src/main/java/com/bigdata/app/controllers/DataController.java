package com.bigdata.app.controllers;

import com.bigdata.app.models.Plan;
import com.bigdata.app.models.ResponseObject;
import com.bigdata.app.services.JsonSchemaService;
import com.bigdata.app.services.DataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@RestController
public class DataController {

    private final DataService dataService;
    private final JsonSchemaService jsonSchemaService;

    @GetMapping(value = "/getall")
    ResponseEntity getAllData() {
        try {

            List data = dataService.getAllData();
            if (data == null || data.isEmpty()) {
                return new ResponseEntity(new ResponseObject("No data present",  HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>()), HttpStatus.NOT_FOUND);
            }
            ResponseObject responseObject =
                    new ResponseObject("Success",  200, data);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (Exception e) {
            ResponseObject responseObject = new ResponseObject("NOT FOUND",  404, new ArrayList<>());
            return new ResponseEntity<>(responseObject, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping(value = "/create")
    ResponseEntity createData(@RequestBody String request){

        try{
            JSONObject jsonObject = new JSONObject(request);
            if (!jsonSchemaService.validateSchema(jsonObject)) {
                ResponseObject responseObject = new ResponseObject("JSON validation failed. Please provide correct input JSON",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
                return new ResponseEntity<>(responseObject, HttpStatus.BAD_REQUEST);
            }

            String key = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
            boolean ifDataExists = dataService.ifKeyIsPresent(key);
            if (!ifDataExists) {
                String eTag = dataService.saveData(jsonObject, key);
                JSONObject dataObject = new JSONObject();
                dataObject.put("ObjectId", jsonObject.get("objectId"));
                ResponseObject responseObject = new ResponseObject("Data added successfully",  HttpStatus.CREATED.value(), jsonObject.get("objectId"));
                return ResponseEntity.created(new URI("/create/" + key)).eTag(eTag)
                        .body(responseObject);
            } else {
                ResponseObject responseObject = new ResponseObject("Data already present",  HttpStatus.CONFLICT.value(), new ArrayList<>());
                return new ResponseEntity<>(responseObject, HttpStatus.CONFLICT);
            }


        } catch (Exception e) {
            ResponseObject responseObject = new ResponseObject("BAD REQUEST",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(responseObject, HttpStatus.BAD_REQUEST);
        }
    }


    @DeleteMapping(value = "/{type}/{id}")
    ResponseEntity deletePlanByTypeAndId(@PathVariable String id, @PathVariable String type, @RequestHeader HttpHeaders requestHeaders) {
        try {
            String key = type + ":" + id;
            boolean planExists = dataService.ifKeyIsPresent(key);
            if (!planExists) {
                ResponseObject r = new ResponseObject("ID couldn't be located",  HttpStatus.NOT_FOUND.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
            } else {
                String ifNoneMatchHeader;
                String ifMatchHeader;
                try {
                    ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
                    ifMatchHeader = requestHeaders.getFirst("If-Match");
                } catch (Exception e) {
                    ResponseObject r = new ResponseObject("Invalid E-Tag Value",  HttpStatus.BAD_REQUEST.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
                }
                String etagFromCache = dataService.fetchEtag(key);
                if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                    ResponseObject r = new ResponseObject("Pre-Condition Failed",  HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.PRECONDITION_FAILED);
                }

                if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                    ResponseObject r = new ResponseObject("",  HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.NOT_MODIFIED);
                }
                dataService.deleteData(key);
                ResponseObject r = new ResponseObject("Data deletion successful", HttpStatus.OK.value(), new ArrayList<>());
                return new ResponseEntity<>(r, HttpStatus.NO_CONTENT);
            }
        } catch (Exception e) {
            ResponseObject r = new ResponseObject("Plan Id not found", HttpStatus.NOT_FOUND.value(), new ArrayList<>());
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping(value = "/{type}/{planId}",produces ={})
    ResponseEntity getPlanByTypeAndId(@PathVariable String type, @PathVariable String planId, @RequestHeader HttpHeaders requestHeaders) throws Exception {
        try {
            String key = type + ":" + planId;
            Map<String, Object> data = dataService.getPlanById(key);
            if (data == null || data.isEmpty()) {
                return new ResponseEntity(new ResponseObject("Provided Wrong Plan Id!",  HttpStatus.NOT_FOUND.value(), new ArrayList<Plan>()), HttpStatus.NOT_FOUND);
            } else {
                String ifNoneMatchHeader;
                String ifMatchHeader;
                try {
                    ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
                    ifMatchHeader = requestHeaders.getFirst("If-Match");
                } catch (Exception e) {
                    ResponseObject r = new ResponseObject("Provided E-Tag is invalid. Please try again", 200, new ArrayList<Plan>());
                    return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
                }
                String etagFromCache = dataService.fetchEtag(key);
                if (ifMatchHeader != null && !(ifMatchHeader.equals(etagFromCache))) {
                    ResponseObject r = new ResponseObject("Pre Condition Failed", HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>( HttpStatus.PRECONDITION_FAILED);
                }

                if ((ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache))) {
                    ResponseObject r = new ResponseObject("", HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
                    return new ResponseEntity<>( HttpStatus.NOT_MODIFIED);
                }

                if (type.equalsIgnoreCase("plan")) {
                    ResponseObject r = new ResponseObject("Success",  HttpStatus.OK.value(), data);
                    HttpHeaders httpHeaders = dataService.generateHeaderFromCache(etagFromCache);
                    return new ResponseEntity<>(new JSONObject(data).toString(),httpHeaders, HttpStatus.OK);
                } else {
                    HttpHeaders httpHeaders = dataService.generateHeaderFromCache(etagFromCache);
                    return new ResponseEntity<>(new JSONObject(data).toString(),httpHeaders, HttpStatus.OK);
                }
            }
        } catch (Exception e) {
            ResponseObject r = new ResponseObject(e.getMessage(), 200, new ArrayList<Plan>());
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping(value = "/{type}/{planId}")
    ResponseEntity patchPlanByTypeAndId(@PathVariable String type, @PathVariable String planId, @RequestHeader HttpHeaders requestHeaders, @RequestBody String request){
        try {
        String key = type + ":" + planId;

        if (!dataService.ifKeyIsPresent(key)) {
            ResponseObject r = new ResponseObject("ID couldn't be located",  HttpStatus.NOT_FOUND.value(), new ArrayList<>());
            return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
        }
        String etagFromCache = dataService.fetchEtag(key);
        String ifMatchHeader = requestHeaders.getFirst("If-Match");

        if (ifMatchHeader != null && !ifMatchHeader.equals(etagFromCache)) {
            ResponseObject r = new ResponseObject("Pre Condition Failed", HttpStatus.PRECONDITION_FAILED.value(), new ArrayList<Plan>());
            return new ResponseEntity<>( HttpStatus.PRECONDITION_FAILED);
        }

        String ifNoneMatchHeader = requestHeaders.getFirst("If-None-Match");
        if (ifNoneMatchHeader != null && ifNoneMatchHeader.equals(etagFromCache)) {
            ResponseObject r = new ResponseObject("", HttpStatus.NOT_MODIFIED.value(), new ArrayList<Plan>());
            return new ResponseEntity<>( HttpStatus.NOT_MODIFIED);
        }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode requestBodyJson = objectMapper.readTree(request);
            Map<String, Object> existingPlanMap = dataService.getPlanById(key);
            System.out.println("Request body: " + request);
            System.out.println("Existing plan: " + existingPlanMap);

            List<Map<String, Object>> existingLinkedPlanServices = (List<Map<String, Object>>) existingPlanMap.getOrDefault("linkedPlanServices", new ArrayList<>());
            ArrayNode newLinkedPlanServices = (ArrayNode) requestBodyJson.get("linkedPlanServices");

            for (JsonNode newLinkedPlanService : newLinkedPlanServices) {
                Map<String, Object> newLinkedPlanServiceMap = objectMapper.convertValue(newLinkedPlanService, new TypeReference<Map<String, Object>>() {});
                existingLinkedPlanServices.add(newLinkedPlanServiceMap);
            }

            existingPlanMap.put("linkedPlanServices", existingLinkedPlanServices);
            JSONObject updatedPlanJson = new JSONObject(objectMapper.writeValueAsString(existingPlanMap));
            String newEtag = dataService.saveData(updatedPlanJson, key);
            dataService.actionLogger(objectMapper.writeValueAsString(existingPlanMap), "PATCH");

            ResponseObject responseObject = new ResponseObject("Data update Successful", HttpStatus.OK.value(), existingPlanMap.get("objectId"));
            return ResponseEntity.ok().eTag(newEtag).body(responseObject);

        } catch (JsonProcessingException e) {
            ResponseObject responseObject = new ResponseObject("BAD REQUEST",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(responseObject, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            ResponseObject responseObject = new ResponseObject("INTERNAL SERVER ERROR",  HttpStatus.BAD_REQUEST.value(), new ArrayList<>());
            return new ResponseEntity<>(responseObject, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
