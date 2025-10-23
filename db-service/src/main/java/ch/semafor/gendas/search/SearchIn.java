package ch.semafor.gendas.search;

import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import ch.semafor.gendas.model.PropertyValue_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Arrays;

public class SearchIn<T> implements SearchOp {
    private final T[] obj;

    public SearchIn(T[] obj) {
        this.obj = obj;
    }

    @Override
    public Criteria setCriteria(Criteria crit) {
        return crit.in(Arrays.asList(obj));
    }

    public T[] getIDs() {
        return obj;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(obj);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SearchIn other = (SearchIn) obj;
        return Arrays.equals(this.obj, other.obj);
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder cb, ListJoin<PropertyValueList, PropertyValue> v) {
        if (obj instanceof String[]) {
            return v.get(PropertyValue_.svalue).in((String[]) obj);
        }
        if (obj instanceof java.util.Date[]) {
            return v.get(PropertyValue_.dateValue).in((java.util.Date[]) (obj));
        }
        if (obj instanceof Double[]) {
            return v.get(PropertyValue_.dvalue).in((Double[]) (obj));
        }
        if (obj instanceof Long[]) {
            return v.get(PropertyValue_.lvalue).in((Long[]) obj);
        }
        return v.get(PropertyValue_.ivalue).in((Integer[]) obj);
    }
}

