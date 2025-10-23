package ch.semafor.gendas.search;

import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import ch.semafor.gendas.model.PropertyValue_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.mongodb.core.query.Criteria;

public class SearchGt<T> implements SearchOp {
    private final T obj;

    public SearchGt(T obj) {
        if (obj == null) throw new IllegalArgumentException("obj must not be null");
        this.obj = obj;
    }

    @Override
    public Criteria setCriteria(Criteria crit) {
        return crit.gt(obj);
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder cb, ListJoin<PropertyValueList, PropertyValue> v) {
        if (obj instanceof java.util.Date) {
            return cb.greaterThan(v.get(PropertyValue_.dateValue), (java.util.Date) obj);
        }
        return cb.gt(v.get(PropertyValue_.lvalue), (Number) obj);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((obj == null) ? 0 : obj.hashCode());
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
        SearchGt other = (SearchGt) obj;
        if (this.obj == null) {
            return other.obj == null;
        } else
            return this.obj.equals(other.obj);
    }

}
