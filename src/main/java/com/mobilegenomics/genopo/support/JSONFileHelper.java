package com.mobilegenomics.genopo.support;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.util.Scanner;

public class JSONFileHelper {

    private static String readJsonAsString(String jsonFile) {
        //1 Takes your JSON file from the raw folder
        InputStream inputStream = JSONFileHelper.class.getClassLoader().getResourceAsStream("raw/" + jsonFile);
        //2 This reads your JSON file
        String jsonString = new Scanner(inputStream).useDelimiter("\\A").next();
        return jsonString;
    }

    public static JsonObject rawtoJsonObject(String jsonFile) {
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(readJsonAsString(jsonFile));
    }

}
