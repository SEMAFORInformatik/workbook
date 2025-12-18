package ch.semafor.intens.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;

@EntityScan(basePackages = "ch.semafor.gendas.model")
@SpringBootApplication(scanBasePackages ={"ch.semafor.intens.ws", "ch.semafor.gendas"}, exclude = {
  MongoAutoConfiguration.class, 
  DataMongoAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
    }
}
