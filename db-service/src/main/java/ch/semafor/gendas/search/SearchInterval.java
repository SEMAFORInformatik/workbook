package ch.semafor.gendas.search;

import ch.semafor.gendas.model.PropertyValue;
import ch.semafor.gendas.model.PropertyValueList;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.mongodb.core.query.Criteria;

public class SearchInterval<T> implements SearchOp {
    private final T lower;
    private final T upper;
    private Bounds bounds = Bounds.Open;
    public SearchInterval(T lower, T upper, Bounds b) {
        if (lower == null && upper == null) {
            throw new IllegalArgumentException("either lower or upper must not be null");
        }
        this.lower = lower;
        this.upper = upper;
        this.bounds = b;
    }

    @Override
    public Criteria setCriteria(Criteria crit) {
        switch (bounds) {
            case Bounded:
                if (lower != null && upper != null)
                    return crit.gte(lower).lte(upper);
                if (upper != null)
                    return crit.lte(upper);
                return crit.gte(lower);

            case LeftOpen:
                if (lower != null && upper != null)
                    return crit.gt(lower).lte(upper);
                if (upper != null)
                    return crit.lte(upper);
                return crit.gt(lower);
            case RightOpen:
                if (lower != null && upper != null)
                    return crit.gte(lower).lt(upper);
                if (upper != null)
                    return crit.lt(upper);
                return crit.gte(lower);
        }
        // default is open
        if (lower != null && upper != null)
            return crit.gt(lower).lt(upper);
        if (lower != null)
            return crit.gt(lower);
        return crit.lt(upper);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bounds == null) ? 0 : bounds.hashCode());
        result = prime * result + ((lower == null) ? 0 : lower.hashCode());
        result = prime * result + ((upper == null) ? 0 : upper.hashCode());
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
        SearchInterval other = (SearchInterval) obj;
        if (bounds != other.bounds)
            return false;
        if (lower == null) {
            if (other.lower != null)
                return false;
        } else if (!lower.equals(other.lower))
            return false;
        if (upper == null) {
            return other.upper == null;
        } else
            return upper.equals(other.upper);
    }

    @Override
    public Predicate getPredicate(CriteriaBuilder cb, ListJoin<PropertyValueList, PropertyValue> v) {
        String valname = "ivalue";
        Number numLower = null, numUpper = null;
        if (lower instanceof java.util.Date) {
            valname = "dateValue";
            if(lower != null) {
                numLower = ((java.util.Date) lower).getTime();
            }
            if(upper != null) {
                numUpper = ((java.util.Date) upper).getTime();
            }
        } else {
            if (lower instanceof Long) {
                valname = "lvalue";
            } else if (lower instanceof Double) {
                valname = "dvalue";
            }
            numLower = (Number) lower;
            numUpper = (Number) upper;
        }

        switch (bounds) {
            case Bounded:
                if (lower != null && upper != null)
                    return cb.and(cb.ge(v.get(valname), numLower), cb.le(v.get(valname), numUpper));
                if (lower == null)
                    return cb.le(v.get(valname), numUpper);
                return cb.ge(v.get(valname), numLower);

            case LeftOpen:
                if (lower != null && upper != null)
                    return cb.and(cb.gt(v.get(valname), numLower), cb.le(v.get(valname), numUpper));
                if (lower == null)
                    return cb.le(v.get(valname), numUpper);
                return cb.gt(v.get(valname), numLower);

            case RightOpen:
                if (lower != null && upper != null)
                    return cb.and(cb.ge(v.get(valname), numLower), cb.lt(v.get(valname), numUpper));
                if (lower == null)
                    return cb.lt(v.get(valname), numUpper);
                return cb.ge(v.get(valname), numLower);
        }
        // default is open
        if (lower != null && upper != null)
            return cb.and(cb.gt(v.get(valname), numLower), cb.lt(v.get(valname), numUpper));
        if (lower == null)
            return cb.lt(v.get(valname), numUpper);
        return cb.gt(v.get(valname), numLower);
    }

    // An open interval does not include its endpoints
    public enum Bounds {Open, Bounded, LeftOpen, RightOpen}

}
