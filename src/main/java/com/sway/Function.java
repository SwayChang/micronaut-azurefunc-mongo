package com.sway;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.internal.MongoClientImpl;
import io.micronaut.azure.function.AzureFunction;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function extends AzureFunction {
//    private static final Logger logger
//            = LoggerFactory.getLogger(Function.class);

    private long mongo_init = 0L;
    private long mongo_query = 0L;
    private static final String AZURE_IP = "xxx";
    private static final String DEV_IP = "xxx";
    private static final boolean DEV_MODE = false;
    public String echo(
        @HttpTrigger(name = "req", methods = HttpMethod.GET, authLevel = AuthorizationLevel.ANONYMOUS)
        String req,
        ExecutionContext context) {
        if (context != null) {
            context.getLogger().info("Executing Function: " + getClass().getName());
        }
        return String.format(req);
    }

    private com.mongodb.MongoClient initServer(final ExecutionContext context){
        long startTime=System.currentTimeMillis();
//        logger.debug("mongodb init start");
        List<MongoCredential> credentials = new ArrayList<>();
        credentials.add(MongoCredential.createCredential("username", "database","password".toCharArray()));
        com.mongodb.MongoClient client = null;
        if(DEV_MODE){
            client = new MongoClient(new ServerAddress(DEV_IP, 27019), credentials);
        }else {
            client = new MongoClient(new ServerAddress(AZURE_IP, 27019), credentials);
        }
//        logger.debug("mongodb init end");
        long endTime=System.currentTimeMillis();
        context.getLogger().info("[ExecutionContext] Mongo init time spends = "+(endTime-startTime));
        this.mongo_init = (endTime-startTime);
//        logger.debug("[Logger] Mongo init time spends = "+(endTime-startTime));
        return client;
    }

    private String queryProducts(com.mongodb.MongoClient client, String id, final ExecutionContext context){
        long startTime=System.currentTimeMillis();
        MongoDatabase db = client.getDatabase("TableName");
        MongoCollection<Document> collection = db.getCollection("Products");
        Bson filter = Filters.eq("product_id", id);
        Document document = collection.find(filter).first();
        String productName = document.get("product_name").toString();
        long endTime=System.currentTimeMillis();
        context.getLogger().info("[ExecutionContext] Mongo query time spends = "+(endTime-startTime));
        this.mongo_query = (endTime-startTime);
//        logger.debug("[Logger] Mongo query time spends = "+(endTime-startTime));
        return productName;
    }

    @FunctionName("call")
    public HttpResponseMessage  run(
            @HttpTrigger(name="req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<String>> request, final ExecutionContext context){
        long startTime=System.currentTimeMillis();
//        logger.debug("Get http call");
        context.getLogger().info("Java HTTP trigger processed a request.");
        final String query = request.getQueryParameters().get("id");
        final String id = request.getBody().orElse(query);

        if (id == null) {
            long endTime=System.currentTimeMillis();
            long time = (startTime-endTime);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body"+ "-- time -- "+time+" s ").build();
        } else {

            com.mongodb.MongoClient client = initServer(context);
            String productName = queryProducts(client, id, context);
            long endTime=System.currentTimeMillis();
            long time = (endTime-startTime);
            String body = String.format("[Product_Name = %s] \nTimestame,%d,%d,%d", productName, this.mongo_init, this.mongo_query, time);
            return request.createResponseBuilder(HttpStatus.OK).body(body).build();
        }
    }
}
