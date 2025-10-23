package ch.semafor.gendas.events;


import ch.semafor.gendas.dao.OwnerRepository;
import ch.semafor.gendas.model.Element;
import ch.semafor.gendas.service.SequenceGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Profile("mongo")
@Component
public class MapModelListener extends AbstractMongoEventListener<Map> {

    private static final Logger logger = LoggerFactory.getLogger(MapModelListener.class);

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<Map> event) {
        Map map = event.getSource();

        // id -> _id
        if (map.containsKey("id")) {
            map.put("_id", map.get("id"));
            map.remove("id");
        } else {
            map.put("_id", sequenceGenerator.createId(Element.SEQUENCE_NAME));
        }

        // remove ownername
        map.remove("ownername");
    }

    @Override
    public void onAfterConvert(AfterConvertEvent<Map> event) {
        Map map = event.getSource();

        if (map.get("_id").getClass().equals(String.class)) {
            map.put("_id", Long.valueOf((String) map.get("_id")));
        }
        // _id -> id
        if (map.containsKey("_id")) {
            map.put("id", map.get("_id"));
            map.remove("_id");
        }

        // add ownername if owner is present
        String username = (String) map.get("owner");
        if (username != null) {
            map.put("ownername", ownerRepository.findByUsername(username).getFullName());
        }

    }
}
