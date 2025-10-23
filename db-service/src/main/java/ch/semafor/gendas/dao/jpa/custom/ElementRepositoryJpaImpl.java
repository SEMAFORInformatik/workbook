package ch.semafor.gendas.dao.jpa.custom;

import ch.semafor.gendas.model.*;
import ch.semafor.gendas.search.SearchIn;
import ch.semafor.gendas.search.SearchOp;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class ElementRepositoryJpaImpl implements ElementRepositoryJpaCustom {

    private static final Logger logger = LoggerFactory.getLogger(ElementRepositoryJpaImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private CriteriaQuery<Element> createCriteriaQuery(
            CriteriaBuilder cb, Class clazz) {
        return getCriteriaBuilder().createQuery(clazz);
    }

    private CriteriaBuilder getCriteriaBuilder() {
        return entityManager.getCriteriaBuilder();
    }

    public Statistics getStatistics() {
        return ((Session) entityManager.getDelegate()).getSessionFactory().getStatistics();
    }

    @Override
    public List<Element> findByGroup(Group group) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Element> cq = createCriteriaQuery(cb, Element.class);
        Root<Element> root = cq.from(Element.class);
        ParameterExpression<Group> paramgroup = cb.parameter(Group.class);
        cq.where(cb.equal(root.get(Element_.ogroup), paramgroup));

        TypedQuery<Element> q = entityManager.createQuery(cq);
        q.setParameter(paramgroup, group);
        return q.getResultList();
    }

    public List<Element> getAllActive() {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Element> cq = createCriteriaQuery(cb, Element.class);
        Root<Element> root = cq.from(Element.class);
        ListJoin<Element, TableModification> mods = root.join(Element_.modifications);
        cq.where(cb.equal(mods.get(TableModification_.nextRevision),
                TableModification.MaxRevision));
        return entityManager.createQuery(cq).getResultList();
    }

    public List<Element> findElementsByArgs(final String etype, final Owner owner,
                                            final Map<String, Object> args, final Map<String, Map<String, Object>> childargs,
                                            Pageable pageable, boolean latestRevision, Date changedSince, boolean canDBPage) {
        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Element> cq = createCriteriaQuery(cb, Element.class);
        Root<Element> root = cq.from(Element.class);
        Join<Element, TableModification> mods = root.join(Element_.modifications);
        Join<Element, ElementType> elemtype = root.join(Element_.elementType);
        List<Predicate> predicates = new ArrayList<Predicate>();
        predicates.add(cb.equal(mods.get(TableModification_.nextRevision),
                TableModification.MaxRevision));
        if (etype != null && etype.length() > 0) {
            predicates.add(cb.equal(elemtype.get(ElementType_.name), etype));
        }

        if (owner != null) { // Search by owner
            Join<Element, Owner> owner_ = root.join(Element_.owner);
            predicates.add(cb.equal(owner_, owner));
        }

        if (changedSince != null) {
            // compare if the latest modification has a date newer than changedSince
            predicates.add(cb.greaterThan(mods.<Date>get(TableModification_.timestamp), changedSince));
        }

        if (args != null && args.size() > 0) { // check own properties
            for (String key : args.keySet()) {
                if (key.equals("id")) {
                	try {
                		var ids = ((SearchIn<Long>) args.get(key)).getIDs();
                		predicates.add(root.get("id").in((Object[]) ids));
                		continue;
                	}catch(ClassCastException ex) {}
                }
                SearchOp op = (SearchOp) args.get(key);
                var not = false;
                if (key.startsWith("_not_")) {
                    key = key.replaceFirst("^_not_", "");
                    not = true;
                }
                ListJoin<Element, Property> props = root.join(Element_.properties);
                Join<Property, PropertyType> ptype = props.join(Property_.type);
                ListJoin<Property, PropertyValueList> vlist = props.join(Property_.valuelist);
                ListJoin<PropertyValueList, PropertyValue> v = vlist.join(PropertyValueList_.values);

                predicates.add(cb.equal(ptype.get(PropertyType_.name), key));
                predicates.add(cb.equal(vlist.get(PropertyValueList_.nextRevision),
                        TableModification.MaxRevision));

                if (not) {
                    predicates.add(op.getPredicate(cb, v).not());
                } else {
                    predicates.add(op.getPredicate(cb, v));
                }
            }
        }
        // Childargs
        if (childargs != null && childargs.size() > 0) {
            for (String type : childargs.keySet()) {
                Map<String, Object> subargs = childargs.get(type);
                logger.debug("child type {}", type);
                for (String key : subargs.keySet()) {
                    logger.debug("Key {}", key);
                    SearchOp op = (SearchOp) subargs.get(key);
                    logger.debug("Search op {}", op);
                    ListJoin<Element, ElementRefs> refs = root.join(Element_.references);
                    ListJoin<ElementRefs, ElementRefList> reflist = refs.join(ElementRefs_.reflist);
                    ListJoin<ElementRefList, Element> elmnts = reflist.join(ElementRefList_.elementlist);
                    ListJoin<Element, Property> props = elmnts.join(Element_.properties);
                    Join<Property, PropertyType> ptype = props.join(Property_.type);
                    ListJoin<Property, PropertyValueList> vlist = props.join(Property_.valuelist);
                    ListJoin<PropertyValueList, PropertyValue> v = vlist.join(PropertyValueList_.values);
                    // Predicates
                    predicates.add(cb.equal(ptype.get(PropertyType_.name), key));
                    predicates.add(cb.equal(refs.get(ElementRefs_.refname), type));
                    predicates.add(cb.equal(vlist.get(PropertyValueList_.nextRevision),
                            TableModification.MaxRevision));
                    predicates.add(op.getPredicate(cb, v));
                    // Set restriction for only latest revision
                    // This should be always true except for deleting an element.
                    if (latestRevision) {
                        predicates.add(cb.equal(reflist.get(ElementRefList_.nextRevision),
                                TableModification.MaxRevision));
                    }
                }
            }
        }

        if (pageable != null) {
            int firstResult = pageable.getPageNumber() * pageable.getPageSize();
            var orders = new ArrayList<Order>();
            for (var s : pageable.getSort()) {
                switch (s.getProperty()) {
                    case "changed":
                        if (s.getDirection().equals(Sort.Direction.DESC)) {
                            orders.add(cb.desc(mods.<Date>get(TableModification_.timestamp)));
                        } else {
                            orders.add(cb.asc(mods.<Date>get(TableModification_.timestamp)));
                        }
                        break;
                    case "id":
                        if (s.getDirection().equals(Sort.Direction.DESC)) {
                            orders.add(cb.desc(root.get(Element_.id)));
                        } else {
                            orders.add(cb.asc(root.get(Element_.id)));
                        }
                        break;
                    case "owner":
                        if (s.getDirection().equals(Sort.Direction.DESC)) {
                            orders.add(cb.desc(root.get(Element_.owner)));
                        } else {
                            orders.add(cb.asc(root.get(Element_.owner)));
                        }
                        break;
                    case "ownername":
                        Join<Element,Owner> owners = root.join(Element_.owner);
                        if (s.getDirection().equals(Sort.Direction.DESC)) {
                            orders.add(cb.desc(owners.get(Owner_.firstName)));
                        } else {
                            orders.add(cb.asc(owners.get(Owner_.firstName)));
                        }
                        break;
                    case "index":
                    break;
                    default:
                        ListJoin<Element,Property> props = root.join(Element_.properties);
                        Join<Property, PropertyType> ptype = props.join(Property_.type);
                        ListJoin<Property, PropertyValueList> vlist = props.join(Property_.valuelist);
                        ListJoin<PropertyValueList, PropertyValue> v = vlist.join(PropertyValueList_.values);
                        predicates.add(cb.equal(ptype.get(PropertyType_.name), s.getProperty()));
                        predicates.add(cb.equal(vlist.get(PropertyValueList_.nextRevision), TableModification.MaxRevision));
                        if (s.getDirection().equals(Sort.Direction.DESC)) {
                            orders.add(cb.desc(v.get(PropertyValue_.dateValue)));
                            orders.add(cb.desc(v.get(PropertyValue_.decimalValue)));
                            orders.add(cb.desc(v.get(PropertyValue_.dvalue)));
                            orders.add(cb.desc(v.get(PropertyValue_.ivalue)));
                            orders.add(cb.desc(v.get(PropertyValue_.lvalue)));
                            orders.add(cb.desc(v.get(PropertyValue_.svalue)));
                            orders.add(cb.desc(v.get(PropertyValue_.text)));
                        } else {
                            orders.add(cb.asc(v.get(PropertyValue_.dateValue)));
                            orders.add(cb.asc(v.get(PropertyValue_.decimalValue)));
                            orders.add(cb.asc(v.get(PropertyValue_.dvalue)));
                            orders.add(cb.asc(v.get(PropertyValue_.ivalue)));
                            orders.add(cb.asc(v.get(PropertyValue_.lvalue)));
                            orders.add(cb.asc(v.get(PropertyValue_.svalue)));
                            orders.add(cb.asc(v.get(PropertyValue_.text)));
                        }
                }
            }
            cq.orderBy(orders);

            cq.select(root).where(predicates.toArray(new Predicate[]{}));

            TypedQuery<Element> q = entityManager.createQuery(cq);
            if (canDBPage) {
                q.setFirstResult(firstResult);
                q.setMaxResults(pageable.getPageSize());
            }
            return q.getResultList();
        }

        cq.select(root).where(predicates.toArray(new Predicate[]{}));
        return entityManager.createQuery(cq).getResultList();
    }
}
