package ch.semafor.gendas.dao.mongo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.mongodb.MongoDBContainer;

public class RepositoryMongoTest {
  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.2");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    mongoDBContainer.start();
    registry.add("spring.mongodb.host", mongoDBContainer::getHost);
    registry.add("spring.mongodb.port", mongoDBContainer::getFirstMappedPort);
  }
}
