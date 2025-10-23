package ch.semafor.gendas.service;

import ch.semafor.gendas.model.DatabaseSequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Profile("mongo")
@Service
public class SequenceGeneratorService {

    @Autowired
    private MongoOperations mongoOperations;

    public Long createId(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(query(where("_id").is(seqName)),
                new Update().inc("seq", 1), options().returnNew(true).upsert(true),
                DatabaseSequence.class);
        return Long.valueOf(!Objects.isNull(counter) ? counter.getSeq() : 1);
    }
}
