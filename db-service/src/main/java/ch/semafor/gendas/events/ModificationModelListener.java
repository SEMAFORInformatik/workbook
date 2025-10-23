package ch.semafor.gendas.events;

import ch.semafor.gendas.model.MapModification;
import ch.semafor.gendas.service.SequenceGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Profile("mongo")
@Component
public class ModificationModelListener extends AbstractMongoEventListener<MapModification> {

    @Autowired
    private SequenceGeneratorService sequenceGenerator;

    @Override
    public void onBeforeConvert(BeforeConvertEvent<MapModification> event) {
        event.getSource().setId(sequenceGenerator.createId(MapModification.SEQUENCE_NAME));
    }
}
