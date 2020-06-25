package functions.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import functions.dynamo.DynamoDBManager;
import functions.dynamo.PhotoItem;
import functions.dynamo.VODItem;
import functions.s3.S3Manager;
import functions.vo.FileVO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

/**
 * Handler for requests to Lambda function.
 */
public class UploadApp implements RequestStreamHandler {

    private S3Manager s3Manager = S3Manager.instance();
    private DynamoDBManager dbManager = DynamoDBManager.instance();

    public void handleRequest(final InputStream inputStream, final OutputStream outputStream, final Context context) throws IOException {

        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        JSONObject responseBody = new JSONObject();

        String uid = "";
        String sid = "";
        String name = "";
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
                if (qps.get("name") != null) {
                    name = (String)qps.get("name");
                }
                if (qps.get("type") != null) {
                    type = (String)qps.get("type");
                }

                System.out.println("uid:"+uid+",sid:"+sid+",type="+type);
            }

            String objectId = s3Manager.generateObjectId();

            String url;
            Object item;
            if (type.equals("vod")) {
                url = s3Manager
                        .generateUploadSignedUrl(
                            S3Manager.VOD_BUCKET,
                            s3Manager.generateVODObjectKey(uid, sid, objectId))
                        .toString();
                item = VODItem.make(uid, sid, objectId, name);
            } else {
                url = s3Manager
                        .generateUploadSignedUrl(
                            S3Manager.PHOTO_BUCKET,
                            s3Manager.generateObjectKey(uid, sid, objectId))
                        .toString();
                item = PhotoItem.make(uid, sid, objectId, name);
            }

            FileVO fileVO = new FileVO(url, objectId);
            responseBody.put("body", fileVO);
            responseJson.put("statusCode", 200);

            JSONObject headerJson = new JSONObject();
            headerJson.put("Content-Type", "application/json");
            headerJson.put("X-Custom-Header", "application/json");

            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());

            Subsegment subsegment = AWSXRay.beginSubsegment("Starting Dynamo Add");
            try {
                // write to DynamoDB
                dbManager.add(item);
            } catch (Exception e) {
                subsegment.addException(e);
                throw e;
            } finally {
                AWSXRay.endSubsegment();
            }

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
