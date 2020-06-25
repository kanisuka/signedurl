package functions.dynamo;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@DynamoDBTable(tableName="PhotoTable")
public class PhotoItem {

    private String uid;
    private long timestamp;
    private String oid;
    private String sid;
    private String name;
    private long ttl;

    @DynamoDBHashKey
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    @DynamoDBRangeKey
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDBAttribute
    public String getOid() {
        return oid;
    }
    public void setOid(String oid) {
        this.oid = oid;
    }

    @DynamoDBAttribute
    public String getSid() {
        return sid;
    }
    public void setSid(String sid) {
        this.sid = sid;
    }

    @DynamoDBAttribute
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @DynamoDBAttribute
    public long getTtl() {
        return ttl;
    }
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public String toString() {
        String output = String.format("{ \"uid\": \"%s\", \"oid\": \"%s\", \"sid\": \"%s\", \"name\": \"%s\", \"ttl\": \"%s\" }", uid, oid, sid, name, ttl);
        System.out.println("output:"+output);
        return output;
    }

    public static PhotoItem make(String uid, String sid, String oid, String name) {
        PhotoItem item = new PhotoItem();
        item.setUid(uid);
        item.setTimestamp(Instant.now().toEpochMilli());
        item.setSid(sid);
        item.setOid(oid);
        item.setName(name);
        item.setTtl(Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond());
        System.out.println("[PHOTO] uid:"+uid+", sid:"+sid+", oid:"+oid);
        return item;
    }

}
