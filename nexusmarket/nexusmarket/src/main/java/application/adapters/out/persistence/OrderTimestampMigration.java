package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.OrderDocument;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("!test")
public class OrderTimestampMigration {
    private final MongoTemplate mongoTemplate;

    public OrderTimestampMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeMissingOrderTimestamps() {
        Query missingUpdatedAt = Query.query(new Criteria().orOperator(
                Criteria.where("updatedAt").exists(false),
                Criteria.where("updatedAt").is(null)));
        mongoTemplate.updateMulti(missingUpdatedAt,
                Update.update("updatedAt", Instant.now()), OrderDocument.class);
    }
}
