package ch.semafor.gendas.search;

import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import ch.semafor.gendas.model.PropertyValue_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;

public class SearchEq<T> implements SearchOp {
    private static final Logger logger = LoggerFactory.getLogger(SearchEq.class);
    private final T obj;
    boolean ignorecase = false;
    boolean not_equal=false;

    public SearchEq(T obj) {
        if (obj == null) throw new IllegalArgumentException("obj must not be null");
        this.obj = obj;
    }

    public SearchEq(T obj, boolean ignorecase) {
        if (obj == null) throw new IllegalArgumentException("obj must not be null");
        this.obj = obj;
        this.ignorecase = ignorecase;
    }
    public void setNotEqual() {
    	this.not_equal = true;
    }
    @Override
    public Criteria setCriteria(Criteria crit) {
        if (obj instanceof String) {
            if (((String) obj).contains("%")) {
                if (ignorecase) {
                    return crit.regex(((String) obj).replace("%", ".*"), "i");
                } else {
                    return crit.regex(((String) obj).replace("%", ".*"));
                }
            }
        }
        return crit.is(obj);
    }

    private Predicate _getPredicate(CriteriaBuilder cb,
    		ListJoin<PropertyValueList,
    		PropertyValue> v) {
        if (obj instanceof String) { //by using like is casesensitive on, use ilike to ingorecase or add a parameter .ignorecase()
            logger.debug("eq svalue {}", obj);
            if (ignorecase) {
                return cb.like(cb.lower(v.get(PropertyValue_.svalue)), ((String) obj).toLowerCase());
            } else {
                return cb.like(v.get(PropertyValue_.svalue), (String) obj);
            }
        }
        if (obj instanceof Long) {
            logger.debug("eq lvalue {}", obj);
            return cb.equal(v.get(PropertyValue_.lvalue), obj);
        }
        if (obj instanceof Double) {
            return cb.equal(v.get(PropertyValue_.dvalue), obj);
        }
        if (obj instanceof java.util.Date) {
            return cb.equal(v.get(PropertyValue_.dateValue), obj);
        }
        if (obj instanceof Integer) {
            return cb.equal(v.get(PropertyValue_.ivalue), obj);
        }
        if (obj instanceof BigDecimal) {
            return cb.equal(v.get(PropertyValue_.decimalValue), obj);
        }
        // must be bool
        return cb.equal(v.get(PropertyValue_.bool), obj);
    }
    
    @Override
    public Predicate getPredicate(CriteriaBuilder cb,
    		ListJoin<PropertyValueList,
    		PropertyValue> v) {
    	if(not_equal) {
    		return _getPredicate(cb, v).not();
    	}
		return _getPredicate(cb, v);
    }
    
    @Override
    public String toString() {
        return obj.toString();
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
        SearchEq other = (SearchEq) obj;
        if (this.obj == null) {
            return other.obj == null;
        } else
            return this.obj.equals(other.obj);
    }
}
