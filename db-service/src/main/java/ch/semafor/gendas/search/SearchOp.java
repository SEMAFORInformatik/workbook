package ch.semafor.gendas.search;

import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.mongodb.core.query.Criteria;

public interface SearchOp {
    // used by Mongo
    Criteria setCriteria(Criteria crit);

    // used by JPA
    Predicate getPredicate(CriteriaBuilder db,
                           ListJoin<PropertyValueList, PropertyValue> v);
}
