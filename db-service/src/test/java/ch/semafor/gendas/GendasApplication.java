package ch.semafor.gendas;

import ch.semafor.gendas.search.SearchIn;
import ch.semafor.gendas.service.ElementService;
import ch.semafor.gendas.service.ElementServiceJpa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@SpringBootApplication
public class GendasApplication implements CommandLineRunner{
    Logger logger = LoggerFactory.getLogger(GendasApplication.class);
    @Autowired
    private ElementService elementService;

    public static void main(String[] args) {
        SpringApplication.run(GendasApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
      List<String> fields = List.of(new String[]{
              "name", "rev", "created", "owner", "ownername", "group", "approval"});
      List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        String type = "Magnet";
        String ownername = null;
        Map<String, Object> search = new HashMap<String, Object>();
        search.put("approval", new SearchIn<String>(
                new String[]{"experimental", "inPreparation", "approved", "tested", "tendered"}));
        Map<String, Map<String, Object>> childsearch = new HashMap<String, Map<String, Object>>();
        int page = -1;
        int pagesize = -1;
        boolean latestRevision = true;
        Map<String, Integer> sortmap = null;
        logger.info("query");
        l = elementService.findByType(
                type, ownername, fields, search, childsearch,
                page, pagesize, sortmap, latestRevision
        );
    }
}

