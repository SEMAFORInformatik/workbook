package ch.semafor.intens.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@EntityScan(basePackages = "ch.semafor.gendas.model")
@SpringBootApplication(scanBasePackages ={"ch.semafor.intens.ws", "ch.semafor.gendas"}, exclude = {
  MongoAutoConfiguration.class, 
  MongoDataAutoConfiguration.class
})
public class Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.run(args);
    }
}
