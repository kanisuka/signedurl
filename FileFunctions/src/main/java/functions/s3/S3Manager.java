package functions.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudfront.CloudFrontCookieSigner;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.util.SignerUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.util.DateUtils;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.handlers.TracingHandler;

import java.io.*;
import java.net.URL;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class S3Manager {

    public final static String COOKIE_DOMAIN = ".cloudfront.net";
    public final static String CF_DIST_DOMAIN = "[File Download CF Domain].cloudfront.net";
    public final static String CF_THUMB_DIST_DOMAIN = "[Thumbnail CF Domain]].cloudfront.net";
    public final static String PHOTO_BUCKET = "signed-img-myid";
    public final static String VOD_BUCKET = "signed-video-myid";
    public final static String KEY_PAIR_ID = "APKAI62VDI2QSRI3XQQA";

    private final static String PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MII...\n" +
        "[Input CloudFront Private Key]\n" +
        "...1Y\n" +
        "-----END RSA PRIVATE KEY-----\n";

    private static volatile S3Manager instance;
    private static AmazonS3 client;
    private File privateFileKey;

    private S3Manager() {

        client = AmazonS3ClientBuilder
                .standard()
                .withRegion(Regions.AP_NORTHEAST_2)
                .withRequestHandlers(new TracingHandler(AWSXRay.getGlobalRecorder()))
                .build();

        try {
            convertToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        this.privateKey = convert();
//        log.info("created file service");
    }

    public static S3Manager instance() {
        if (instance == null) {
            synchronized (S3Manager.class) {
                if (instance == null) {
                    instance = new S3Manager();
                }
            }
        }

        return instance;
    }

    public String generateObjectKey(String uid, String sid, String objectId) {
        return sid + "/" + uid + "/" + objectId;
    }

    public String generateVODObjectKey(String uid, String sid, String objectId) {
        return "input/" + sid + "/" + uid + "/" + objectId;
    }

    public String generateVODThumbObjectKey(String uid, String sid, String objectId) {
        return "assets/input/" + sid + "/" + uid + "/" + "thumb/" + objectId + ".0000000.jpg";
    }

    public String generateVODStreamingObjectKey(String uid, String sid, String objectId) {
        return "assets/input/" + sid + "/" + uid + "/" + "hls/" + objectId + ".m3u8";
    }

    public String generateVODStreamingHlsKey(String uid, String sid, String objectId) {
        return "assets/input/" + sid + "/" + uid + "/" + "hls/*";
    }

    public String generateObjectId() {
        return UUID.randomUUID().toString();
    }

    public URL generateUploadSignedUrl(String bucket, String objectKey) {

        Duration duration = Duration.ofMinutes(3);
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.of("UTC")).toInstant().plus(duration));

        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, objectKey)
                .withMethod(HttpMethod.PUT)
                .withExpiration(date);

        URL url = null;
        Subsegment subsegment = AWSXRay.beginSubsegment("S3 pre-signedURL");
        try {
            /*client.setBucketAccelerateConfiguration(
                    new SetBucketAccelerateConfigurationRequest(bucketName,
                            new BucketAccelerateConfiguration(
                                    BucketAccelerateStatus.Enabled)));*/
            url = client.generatePresignedUrl(req);
        } catch (Exception e) {
            subsegment.addException(e);
        } finally {
            AWSXRay.endSubsegment();
        }

        return url;
    }

    public URL generateDownloadSignedUrl(String bucket, String objectKey) {
        Duration duration = Duration.ofMinutes(5);
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.of("UTC")).toInstant().plus(duration));

        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(bucket, objectKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(date);

        return client.generatePresignedUrl(req);
    }

    public List<String> generateCfDownloadSignedCookies(String objectKey) throws InvalidKeySpecException, IOException {

        Date dateLessThan = DateUtils.parseISO8601Date(Instant.now().plus(Duration.ofMinutes(15)).toString());

        CloudFrontCookieSigner.CookiesForCannedPolicy cookiesPolicy =
                CloudFrontCookieSigner.getCookiesForCannedPolicy(
                        SignerUtils.Protocol.https, CF_DIST_DOMAIN, privateFileKey,
                        objectKey, KEY_PAIR_ID, dateLessThan);

        List<String> cookies = new ArrayList<>();
        cookies.add(
                cookiesPolicy.getKeyPairId().getKey() + "=" + cookiesPolicy.getKeyPairId().getValue() + "; "
                + "Domain=" + COOKIE_DOMAIN + "; "
                + "Path=/;");
        cookies.add(
                cookiesPolicy.getSignature().getKey() + "=" + cookiesPolicy.getSignature().getValue() + "; "
                + "Domain=" + COOKIE_DOMAIN + "; "
                + "Path=/;");
        cookies.add(
                cookiesPolicy.getExpires().getKey() + "=" + cookiesPolicy.getExpires().getValue() + "; "
                + "Domain=" + COOKIE_DOMAIN + "; "
                + "Path=/;");

        System.out.println("[CF][Streaming] cookie: " + cookies);
        return cookies;
    }

    public Map<String, String> generateSignedCookies(String objectKey) throws InvalidKeySpecException, IOException {

        Map<String, String> result = new HashMap<>();

        Date dateLessThan = DateUtils.parseISO8601Date(Instant.now().plus(Duration.ofMinutes(15)).toString());

        /*CloudFrontCookieSigner.CookiesForCannedPolicy cookiesPolicy =
                CloudFrontCookieSigner.getCookiesForCannedPolicy(
                        protocol, CF_DIST_DOMAIN, privateFileKey,
                        objectKey, KEY_PAIR_ID, dateLessThan);*/

        CloudFrontCookieSigner.CookiesForCustomPolicy cookiesPolicy =
                CloudFrontCookieSigner.getCookiesForCustomPolicy(
                        SignerUtils.Protocol.https,
                        CF_DIST_DOMAIN, privateFileKey,
                        objectKey,
                        KEY_PAIR_ID,
                        dateLessThan,
                        null,
                        null);

        result.put(cookiesPolicy.getKeyPairId().getKey(), cookiesPolicy.getKeyPairId().getValue());
        result.put(cookiesPolicy.getSignature().getKey(), cookiesPolicy.getSignature().getValue());
        result.put(cookiesPolicy.getPolicy().getKey(), cookiesPolicy.getPolicy().getValue());

        System.out.println("[PATH] resource: " + objectKey);

        return result;
    }

    public String generateCfDownloadRawUrl(String objectKey) {
        return "https://"+CF_DIST_DOMAIN+"/"+objectKey;
    }

    public String generateCfDownloadSignedUrl(String objectKey) throws InvalidKeySpecException, IOException {
        Date dateLessThan = DateUtils.parseISO8601Date(Instant.now().plus(Duration.ofMinutes(15)).toString());
        return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                SignerUtils.Protocol.https, CF_DIST_DOMAIN, privateFileKey,
                objectKey, KEY_PAIR_ID, dateLessThan);
    }

    public String generateCfThumbDownloadSignedUrl(String objectKey) throws InvalidKeySpecException, IOException {
        Date dateLessThan = DateUtils.parseISO8601Date(Instant.now().plus(Duration.ofMinutes(15)).toString());
        return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                SignerUtils.Protocol.https, CF_THUMB_DIST_DOMAIN, privateFileKey,
                objectKey, KEY_PAIR_ID, dateLessThan);
    }

    private void convertToFile() throws IOException {
        privateFileKey = File.createTempFile("myprivate", ".pem");
        FileWriter writer = new FileWriter(privateFileKey);
        writer.write(PRIVATE_KEY);
        writer.close();

        BufferedReader reader = new BufferedReader(new FileReader(privateFileKey));
        reader.close();
    }

}
