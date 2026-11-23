package com.travel.telemetry;

import com.mongodb.MongoCommandException;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static com.mongodb.client.model.Filters.*;

@Component
public class MongoExecutionStore {
    private final MongoClient client;
    private final String database;
    private final int retentionDays;
    private final Clock clock;
    private boolean initialized;
    public MongoExecutionStore(ObjectProvider<MongoClient> clients,@Value("${telemetry.database}")String database,
            @Value("${telemetry.retention-days:90}")int days,Clock clock) {
        this.client=clients.getIfAvailable();this.database=database;this.clock=clock;
        if(days<1||days>3650)throw new IllegalArgumentException("Telemetry retention must be 1..3650 days");
        this.retentionDays=days;
    }
    public boolean enabled(){return client!=null;}
    public int retentionDays(){return retentionDays;}
    public synchronized MongoCollection<Document> collection() {
        if(client==null)throw new IllegalStateException("Telemetry disabled");
        var db=client.getDatabase(database);
        if(!initialized) {
            var schema=Document.parse("""
              {"$jsonSchema":{"bsonType":"object","required":["_id","draftId","schemaVersion","scope","startedAt","finishedAt","expiresAt","durationMs","status","stages"],
                "properties":{"_id":{"bsonType":"string"},"draftId":{"bsonType":"string"},"schemaVersion":{"enum":[1]},"scope":{"enum":["TRIP_DRAFT"]},
                "startedAt":{"bsonType":"date"},"finishedAt":{"bsonType":"date"},"expiresAt":{"bsonType":"date"},
                "durationMs":{"bsonType":["int","long"],"minimum":0},"status":{"bsonType":"string","maxLength":64},"stages":{"bsonType":"array","maxItems":16,"items":{"bsonType":"object"}}}}}
              """);
            try { db.createCollection("ai_execution_runs",new CreateCollectionOptions().validationOptions(new ValidationOptions().validator(schema))); }
            catch(MongoCommandException e){if(e.getErrorCode()!=48)throw e;}
            db.runCommand(new Document("collMod","ai_execution_runs").append("validator",schema).append("validationLevel","strict").append("validationAction","error"));
            var c=db.getCollection("ai_execution_runs");
            c.createIndex(Indexes.ascending("expiresAt"),new IndexOptions().expireAfter(0L,TimeUnit.SECONDS));
            c.createIndex(Indexes.descending("startedAt"));
            c.createIndex(Indexes.compoundIndex(Indexes.ascending("status"),Indexes.descending("startedAt")));
            initialized=true;
        }
        return db.getCollection("ai_execution_runs");
    }
    public void publish(String json) {
        Document document=Document.parse(json);
        for(String field:List.of("startedAt","finishedAt"))document.put(field,new Date(((Number)document.get(field)).longValue()));
        Date expires=Date.from(document.getDate("finishedAt").toInstant().plus(Duration.ofDays(retentionDays)));
        if(!expires.toInstant().isAfter(clock.instant()))return;
        document.put("expiresAt",expires);
        var c=collection();
        try { c.updateOne(eq("_id",document.getString("_id")),new Document("$setOnInsert",document),new UpdateOptions().upsert(true)); }
        catch(com.mongodb.MongoWriteException e){if(e.getError().getCode()!=11000)throw e;}
    }
    public record Point(String name,long value){}
    public record Run(String id,String status,String mode,Instant startedAt,long durationMs,List<Document> stages,String failureCode){}
    public record Data(long total,Double p50Ms,Double p95Ms,List<Point> statuses,List<Run> runs){}
    public Data read(int days,int page) {
        var c=collection();Instant now=clock.instant();
        Instant since=now.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().minusDays(days-1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        var match=and(gte("startedAt",Date.from(since)),lte("startedAt",Date.from(now)),gt("expiresAt",Date.from(now)));
        var status=c.aggregate(List.of(Aggregates.match(match),Aggregates.group("$status",Accumulators.sum("count",1)),Aggregates.sort(Sorts.ascending("_id")))).into(new ArrayList<>());
        var points=status.stream().map(d->new Point(d.getString("_id"),((Number)d.get("count")).longValue())).toList();
        var quantiles=c.aggregate(List.of(Aggregates.match(match),Document.parse("""
            {"$group":{"_id":null,"values":{"$percentile":{"input":"$durationMs","p":[0.5,0.95],"method":"approximate"}}}}
            """))).first();
        List<?> percentiles=quantiles==null?List.of():quantiles.getList("values",Object.class);
        var runs=c.find(match).sort(Sorts.orderBy(Sorts.descending("startedAt"),Sorts.ascending("_id"))).skip(page*20).limit(20).into(new ArrayList<>()).stream()
            .map(d->new Run(d.getString("_id"),d.getString("status"),d.getString("overallMode"),d.getDate("startedAt").toInstant(),((Number)d.get("durationMs")).longValue(),d.getList("stages",Document.class),d.getString("failureCode"))).toList();
        return new Data(points.stream().mapToLong(Point::value).sum(),percentiles.isEmpty()?null:((Number)percentiles.get(0)).doubleValue(),percentiles.isEmpty()?null:((Number)percentiles.get(1)).doubleValue(),points,runs);
    }
}
