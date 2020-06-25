package functions.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import functions.s3.S3Manager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.security.spec.InvalidKeySpecException;

/**
 * Handler for requests to Lambda function.
 */
public class ThumbApp implements RequestStreamHandler {

    private S3Manager s3Manager = S3Manager.instance();

    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context) throws IOException {

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        JSONObject responseBody = new JSONObject();

        String objectId = "";
        String uid = "";
        String sid = "";
        String type = "";

        try {
            JSONObject event = (JSONObject) parser.parse(reader);
            System.out.println("event:"+event.toJSONString());

            if (event.get("pathParameters") != null) {
                JSONObject pps = (JSONObject) event.get("pathParameters");
                if (pps.get("oid") != null) {
                    objectId = (String) pps.get("oid");
                }
            }
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

            String url;
            /*if (type.equals("vod")) {
                url = s3Manager
                        .generateDownloadSignedUrl(
                                S3Manager.VOD_BUCKET,
                                s3Manager.generateVODThumbObjectKey(uid, sid, objectId))
                        .toString();
                System.out.println("[VOD][Thumb] download url: " + url);*/
            if (type.equals("vod")) {
                url = s3Manager.generateCfDownloadSignedUrl(
                        s3Manager.generateVODThumbObjectKey(uid, sid, objectId)
                );
                System.out.println("[CF][Thumbnail][VOD] download url: " + url);
            } else {
                url = s3Manager.generateCfThumbDownloadSignedUrl(
                        s3Manager.generateObjectKey(uid, sid, objectId)
                );
                System.out.println("[CF][Thumb] download url: " + url);
            }

            responseJson.put("statusCode", 302);

            JSONObject headerJson = new JSONObject();
            headerJson.put("Location", url);

            responseJson.put("headers", headerJson);
        } catch (ParseException | InvalidKeySpecException pex) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", pex);
        }

        System.out.println(responseJson.toString());

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();

    }

}
