package functions.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import functions.dynamo.DynamoDBManager;
import functions.dynamo.PhotoItem;
import functions.dynamo.VODItem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.List;

/**
 * Handler for requests to Lambda function.
 */
public class ListApp implements RequestStreamHandler {

    private DynamoDBManager dbManager = DynamoDBManager.instance();

    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context) throws IOException {

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        JSONObject responseBody = new JSONObject();

        String uid = "";
        String sid = "";
        String type = "";

        try {
            JSONObject event = (JSONObject) parser.parse(reader);
            System.out.println("event:"+event.toJSONString());

            if (event.get("queryStringParameters") != null) {
                JSONObject qps = (JSONObject) event.get("queryStringParameters");
                if (qps.get("uid") != null) {
                    uid = (String)qps.get("uid");
                }
                if (qps.get("sid") != null) {
                    sid = (String)qps.get("sid");
                }
                if (qps.get("type") != null) {
                    type = (String)qps.get("type");
                }

                System.out.println("uid:"+uid+",sid:"+sid);
            }

            String photosStr;
            if (type.equals("vod")) {
                List<VODItem> photos = dbManager.listVOD(uid);
                Gson gson = new Gson();
                photosStr = gson.toJson(photos);
            } else {
                List<PhotoItem> photos = dbManager.list(uid);
                Gson gson = new Gson();
                photosStr = gson.toJson(photos);
            }

//            System.out.println("photosStr --> " + photosStr);
//            responseBody.put("photos", photosStr);

            responseJson.put("statusCode", 200);

            JSONObject headerJson = new JSONObject();
            headerJson.put("Content-Type", "application/json");
            headerJson.put("X-Custom-Header", "application/json");

            responseJson.put("headers", headerJson);
            responseJson.put("body", photosStr);
            System.out.println("body toString --> " + photosStr);
//            System.out.println("body toJSONString --> " + responseBody.toJSONString());

        } catch (ParseException pex) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", pex);
        }

        System.out.println(responseJson.toString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();

    }

}
