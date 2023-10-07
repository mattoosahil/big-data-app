package com.bigdata.app.services;

import lombok.SneakyThrows;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class DataService {

    private final static String PLAN_CONST = "plan";

    private Jedis jedis;
    private ETagService eTagService;

    public DataService(Jedis jedis, ETagService eTagService) {
        this.jedis = jedis;
        this.eTagService = eTagService;
    }

    public boolean ifKeyIsPresent(String key) {
        return jedis.exists(key);
    }

    public String updateEtag(String eTagKey, JSONObject eTagValue) {
        String eTag = eTagService.eTagGen(eTagValue);
        jedis.hset(eTagKey, "eTag", eTag);
        return eTag;
    }

    @SneakyThrows
    public String saveData(JSONObject planObject, String planKey) throws JSONException {
        mapHelper(planObject);
        return this.updateEtag(planKey, planObject);
    }

    public List getAllData() {
        List valueList = new ArrayList<>();
        Set<String> keys = jedis.keys("plan:*").stream().
                filter(s -> s.lastIndexOf(":") == s.indexOf(":")).collect(Collectors.toSet());
        for (String key : keys) {
            Map<String, Object> outputMap = new HashMap<>();
            getOrDeleteData(key, outputMap, false);
            valueList.add(outputMap.values());
        }
        return valueList;
    }


    private Map<String, Object> getOrDeleteData(String redisKeyVal, Map<String, Object> result, boolean delFlag) {
        Set<String> keySet = jedis.keys(redisKeyVal + ":*");
        keySet.add(redisKeyVal);
        jedis.close();
        for (String keyVal : keySet) {
            if (keyVal.equals(redisKeyVal)) {
                if (delFlag) {
                    jedis.del(new String[]{keyVal});
                    jedis.close();
                } else {
                    Map<String, String> keyMap = jedis.hgetAll(keyVal);
                    jedis.close();
                    for (String key : keyMap.keySet()) {
                        if (!key.equalsIgnoreCase("eTag")) {
                            result.put(key,
                                    isInteger(keyMap.get(key)) ? Integer.parseInt(keyMap.get(key)) : keyMap.get(key));
                        }
                    }
                }

            } else {
                String newKey = keyVal.substring((redisKeyVal + ":").length());
                Set<String> setMembers = jedis.smembers(keyVal);
                jedis.close();
                if (setMembers.size() > 1 || newKey.equals("linkedPlanServices")) {
                    List<Object> resultList = new ArrayList<Object>();
                    for (String keyMember : setMembers) {
                        if (delFlag) {
                            getOrDeleteData(keyMember, null, true);
                        } else {
                            Map<String, Object> objectMap = new HashMap<String, Object>();
                            resultList.add(getOrDeleteData(keyMember, objectMap, false));

                        }
                    }
                    if (delFlag) {
                        jedis.del(new String[]{keyVal});
                        jedis.close();
                    } else {
                        result.put(newKey, resultList);
                    }

                } else {
                    if (delFlag) {
                        jedis.del(new String[]{setMembers.iterator().next(), keyVal});
                        jedis.close();
                    } else {
                        Map<String, String> val = jedis.hgetAll(setMembers.iterator().next());
                        jedis.close();
                        Map<String, Object> newMap = new HashMap<String, Object>();
                        for (String name : val.keySet()) {
                            newMap.put(name,
                                    isInteger(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
                        }
                        result.put(newKey, newMap);
                    }
                }
            }
        }
        return result;
    }

    public Map<String, Object> getPlanById(String keyId) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        getOrDeleteData(keyId, resultMap, false);
        return resultMap;
    }

    public String fetchEtag(String eTagKey) {
        return jedis.hget(eTagKey, "eTag");
    }

    public void deleteData(String id) {
        getOrDeleteData(id, null, true);
    }

    public Map<String, Map<String, Object>> mapHelper(JSONObject jsonObject) throws JSONException {

        Map<String, Map<String, Object>> objectMap = new HashMap<>();
        Map<String, Object> jsonValueMap = new HashMap<>();
        Iterator<String> jsonObjectKeyIterator = jsonObject.keys();
        while (jsonObjectKeyIterator.hasNext()) {
            String redisKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
            String objectKey = jsonObjectKeyIterator.next();
            Object objectValue = jsonObject.get(objectKey);

            if (objectValue instanceof JSONObject) {
                addObject(redisKey, objectKey, objectValue);

            } else if (objectValue instanceof JSONArray) {
                addArray(redisKey, objectKey, objectValue);

            } else {
                updateObjectMap(redisKey, objectKey, objectValue, jsonValueMap, objectMap);
            }
        }
        return objectMap;
    }

    private List<Object> listHelper(JSONArray jsonArray) throws JSONException {
        List<Object> data = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object objectValue = jsonArray.get(i);
            if (objectValue instanceof JSONArray) {
                objectValue = listHelper((JSONArray) objectValue);
            } else if (objectValue instanceof JSONObject) {
                objectValue = mapHelper((JSONObject) objectValue);
            }
            data.add(objectValue);
        }
        return data;
    }

    private void addObject(String redisKey,String objectKey, Object objectValue ){
        objectValue = mapHelper((JSONObject) objectValue);
        HashMap<String, Map<String, Object>> objectValueMap = (HashMap<String, Map<String, Object>>) objectValue;
        jedis.sadd(redisKey + ":" + objectKey, objectValueMap.entrySet().iterator().next().getKey());
        System.out.println("Inside Object ::::::: redisKey :::::: " + redisKey + ":" + objectKey + ":" + " :::::: " + objectValueMap.entrySet().iterator().next().getKey());
        jedis.close();
    }

    private void addArray(String redisKey,String objectKey, Object objectValue ){
        objectValue = listHelper((JSONArray) objectValue);
        for (HashMap<String, HashMap<String, Object>> entryMap : (List<HashMap<String, HashMap<String, Object>>>) objectValue) {
            for (String listKeyValue : entryMap.keySet()) {
                jedis.sadd(redisKey + ":" + objectKey, listKeyValue);
                jedis.close();
            }
        }
    }

    private void updateObjectMap(String redisKey,String objectKey, Object objectValue, Map<String, Object> jsonValueMap, Map<String, Map<String, Object>> objectMap ){
        jedis.hset(redisKey, objectKey, objectValue.toString());
        System.out.println("Redis Key:::: " + redisKey + " Object Key:::: " + objectKey + " Value:::: " + objectValue.toString());
        jedis.close();
        jsonValueMap.put(objectKey, objectValue);
        objectMap.put(redisKey, jsonValueMap);
    }

    private boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public HttpHeaders generateHeaderFromCache(String etagFromCache) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("ETag", etagFromCache);
        httpHeaders.add("Accept","application/json");
        httpHeaders.add("Content-Type","application/json");
        return httpHeaders;
    }
}
