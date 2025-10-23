package ch.semafor.gendas.dao.mongo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

public class RepositoryMongoTest {
  @Container
  static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4-focal");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
  }
}
