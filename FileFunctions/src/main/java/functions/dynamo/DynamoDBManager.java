package functions.dynamo;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;

import java.util.List;

public class DynamoDBManager {

    private static volatile DynamoDBManager instance;

    private static DynamoDBMapper mapper;

    private DynamoDBManager() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();

        mapper = new DynamoDBMapper(client);
    }

    public static DynamoDBManager instance() {
        if (instance == null) {
            synchronized (DynamoDBManager.class) {
                if (instance == null) {
                    instance = new DynamoDBManager();
                }
            }
        }

        return instance;
    }

    public void add(Object item) {
        mapper.save(item);
    }

    public List<PhotoItem> list(String uid) {
        PhotoItem pk = new PhotoItem();
        pk.setUid(uid);

        DynamoDBQueryExpression<PhotoItem> queryExpression = new DynamoDBQueryExpression<PhotoItem>()
                .withHashKeyValues(pk);

        return mapper.query(PhotoItem.class, queryExpression);
    }

    public List<VODItem> listVOD(String uid) {
        VODItem pk = new VODItem();
        pk.setUid(uid);

        DynamoDBQueryExpression<VODItem> queryExpression = new DynamoDBQueryExpression<VODItem>()
                .withHashKeyValues(pk);

        return mapper.query(VODItem.class, queryExpression);
    }



}
