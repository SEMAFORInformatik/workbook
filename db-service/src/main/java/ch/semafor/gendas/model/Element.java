/*
 * Copyright 2010 Semafor Informatik & Energie AG, Basel, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 */
package ch.semafor.gendas.model;

import ch.semafor.gendas.dao.jpa.ElementRepositoryJpa;
import ch.semafor.gendas.exceptions.CoreException;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

@Entity
@Table(name = "elements")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Element implements Serializable {
    @org.springframework.data.annotation.Transient
    public static final String SEQUENCE_NAME = "elements";
    private static final Logger logger = LoggerFactory.getLogger(Element.class);
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "element", cascade = {CascadeType.ALL})
    //CascadeType.MERGE, CascadeType.REMOVE}) //, cascade = CascadeType.ALL)
    @org.hibernate.annotations.OrderBy(clause = "id asc")
    private final List<TableModification> modifications = new ArrayList<TableModification>();
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
    //CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    private final List<ElementRefs> references = new ArrayList<ElementRefs>();
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_seq_gen")
    @SequenceGenerator(name = "element_seq_gen", sequenceName = "ELEMENT_SEQ", allocationSize = 50)
    // must use this to have an independent id for elements. On oracle AUTO uses a single sequence
    private Long id;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_type_id", nullable = false)
    private ElementType elementType;
    @OneToMany(mappedBy = "element", cascade = {CascadeType.ALL})
    //	CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    private List<Property> properties = new ArrayList<Property>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    private Owner owner; // reference to username of Owner

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ogroup")
    private Group ogroup; // reference to group name

    // flag to prevent multiple loads
    @Transient
    // ToDo new name, PMD: AvoidFieldNameMatchingMethodName
    private boolean isLoaded = false;

    // prevent stack overflow when printing
    @Transient
    private boolean printing = false;

    // prevent stack overflow when crunching
    @Transient
    private boolean crunched = false;

    @Transient
    private String refName = "";

    // Default Constructor
    public Element() throws CoreException {
        addModification();
    }

    // Constructor
    public Element(final ElementType type) throws CoreException {
        super();
        this.elementType = type;
        addModification();
    }

    public List<Property> getProperties() {
        return this.properties;
    }

    public void setProperties(final List<Property> properties) {
        this.properties = properties;
    }

    /**
     * extract list of properties with given property names
     *
     * @param pnames list of property names to be extracted
     * @return extracted list of properties
     */
    private List<Property> getProperties(List<String> pnames) {
        List<Property> pl = new ArrayList<Property>();
        for (Property p : this.properties) {
            if (pnames.contains(p.getName())) {
                pl.add(p);
            }
        }
        return pl;
    }

    // search in ref elements if name is dotted
    private Map<Element, List<String>> getProperties(List<String> pnames, Long rev) {
        if (rev == null) rev = TableModification.MaxRevision;
        Map<Element, List<String>> sub = new HashMap<Element, List<String>>();
        for (String n : pnames) {
            int s = n.indexOf('.');
            if (s > 0) {
                String refnam = n.substring(0, s);
                ElementRefs r = getElementRefs(refnam);
                if (r == null) continue;
                List<Element> el = r.getListOfElements(rev);
                for (Element e : el) {
                    if (sub.containsKey(e)) {
                        sub.get(e).add(n.substring(s + 1));
                    } else {
                        List<String> sl = new ArrayList<String>();
                        sl.add(n.substring(s + 1));
                        e.setRefName(refnam);
                        sub.put(e, sl);
                    }
                }
            } else {
                if (sub.containsKey(this)) {
                    sub.get(this).add(n);
                } else {
                    List<String> sl = new ArrayList<String>();
                    sl.add(n);
                    sub.put(this, sl);
                }
            }
        }
        return sub;
    }

    private String getRefName() {
        return this.refName;
    }

    private void setRefName(String refname) {
        this.refName = refname;
    }

    public List<ElementRefs> getReferences() {
        return this.references;
    }

    // getter and setter of Id
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public boolean isTransient() {
        return this.id == null;
    }

    public boolean isPersistent() {
        return this.id != null;
    }

    public Owner getOwner() {
        return this.owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    private TableModification addModification() throws CoreException {
        final TableModification mod = new TableModification(this, getLastModification());
        this.modifications.add(mod);
        return mod;
    }

    public List<TableModification> getModifications() {
        return modifications;
    }

    // Get the last modification
    public TableModification getLastModification() throws CoreException {
        if (modifications.isEmpty()) {
            return null;
        }
        final int inx = modifications.size() - 1;
        final TableModification hist = modifications.get(inx);
        if (hist.getNextRevision() != TableModification.MaxRevision) {
            for (TableModification mod : modifications) {
                logger.error("Modification id {}, rev {}, nextRev {}",
                        mod.getId(), mod.getRevision(), mod.getNextRevision());
            }
            throw new CoreException(
                    "Database Error: Last modification (rev " + hist.getRevision() + ") has next rev " +
                            hist.getNextRevision() + " ( not MaxRevision as expected)");
        }
        return hist;
    }

    // Get the new modification or create one if it does not exist.
    private TableModification getNewModification() throws CoreException {
        TableModification last_mod = getLastModification();
        if (last_mod != null) {
            if (last_mod.isTransient()) {
                return last_mod;
            }
        }
        final TableModification new_mod = addModification();
        if (last_mod != null) {
            last_mod.setNextRevision(new_mod.getRevision());
            logger.debug("Last rev " + last_mod.getRevision() + " next " +
                    last_mod.getNextRevision());
        }
        return new_mod;
    }

    public TableModification getModification(final long rev) {
        int inx = modifications.size() - 1;
        TableModification hist;
        while (inx >= 0) {
            hist = modifications.get(inx);
            if (hist.getRevision() == rev) {
                return hist;
            }
            inx--;
        }
        return null;
    }

    public TableModification getModification(final Date dat) {
        int inx = modifications.size() - 1;
        TableModification hist;
        while (inx >= 0) {
            hist = modifications.get(inx);
            if (dat.compareTo(hist.getTimestamp()) >= 0) {
                return hist;
            }
            inx--;
        }
        return null;
    }

    // Get the last Revisionnumber
    public long getLastRevision() throws CoreException {
        final TableModification last_mod = getLastModification();
        if (last_mod == null) {
            throw new CoreException(
                    "An Element without a modification is not possible");
        }
        return last_mod.getRevision();
    }

    // Get the new Revisionnumber and create a transient new modification
    // if it does not exist.
    public long getNewRevision() throws CoreException {
        final TableModification hist = getNewModification();
        if (hist == null) {
            throw new CoreException("New modification not created");
        }
        return hist.getRevision();
    }

    // getter and setter of ElementType
    public ElementType getElementType() {
        return this.elementType;
    }

    public void setElementType(final ElementType elementType) {
        this.elementType = elementType;
    }

    // get Property by type
    public Property getProperty(PropertyType t) {
        // 1:40 PM
        for (Property p : this.properties) {
            if (p != null && t.equals(p.getType())) {
                return p;
            }
        }
        return null;
    }

    // get Property by name
    public Property getProperty(final String name) {
        for (Property p : this.properties) {
            if (name.equals(p.getType().getName())) {
                return p;
            }
        }
        return null;
    }

    // add a new Property
    public void addProperty(final Property prop) throws CoreException {
        if (!this.equals(prop.getElement())) {
            throw new CoreException("Property not attached to this Element");
        }
        if (properties.contains(prop)) {
            throw new CoreException("Property already added to Element");
        }
        this.properties.add(prop);
    }

    // add a new ElementRefs
    public void addElementRefs(ElementRefs r) throws CoreException {
        if (getElementRefs(r.getRefName()) != null) {
            throw new CoreException("ElementRefs '" + r.getRefName()
                    + "' already exists");
        }
        r.setParent(this);
        r.setRevision(getNewRevision());
        r.setNextRevision(TableModification.MaxRevision);
        r.crunch();
        this.references.add(r);
    }

    // get ElementRefs by name
    public ElementRefs getElementRefs(final String key) {
        for (ElementRefs r : this.references) {
            if (r.getRefName().equals(key)) {
                return r;
            }
        }
        return null;
    }

    // get a list of all Elementrefs
    public List<ElementRefs> getListOfElementRefs() {
        return this.references;
    }

    public ElementRefList getElementRefList(final String key, final long rev) {
        final ElementRefs r = getElementRefs(key);
        if (r != null) {
            if (r.isInRevision(rev)) {
                return r.getElementRefList(rev);
            }
        }
        return null;
    }

    public ElementRefList getLastElementRefList(final String key) {
        return getElementRefList(key, TableModification.MaxRevision);
    }

    public void setListOfElements(final String key, final List<Element> ellist)
            throws CoreException {
        logger.debug("Element::setListOfElements(): key={}", key);
        ElementRefs r = getElementRefs(key);
        if (r != null) {
            r.setListOfElements(ellist);
            return;
        }
        // element ref does not yet exist
        if (ellist == null || ellist.size() == 0) {
            // on 9/20/10 1:44 PM
            return;
        }
        r = new ElementRefs(this, key);
        r.setListOfElements(ellist);
    }

    public void addElement(String key, Element el) throws CoreException {
        logger.debug("Element::addReference(): key= {}", key);
        ElementRefs r = getElementRefs(key);
        if (r != null) {
            r.addElement(el);
            return;
        }
        // Not yet created
        r = new ElementRefs(this, key);
        r.addElement(el);
    }

    public List<Element> getListOfElements(final String key)
            throws CoreException {
        final ElementRefs r = getElementRefs(key);
        // 1:42 PM
        if (r == null) {
            return null;
        }
        return r.getLastListOfElements();
    }

    // Comparison of two Elements
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Element e)) {
            return false;
        }
        if (this.elementType != e.elementType) {
            return false;
        }
		if (this.id != null) {
			if (this.id.equals(e.id)) {
				return true;
			} else {
				return false;
			}
		}
        // TODO: check name (and rev?)
        //Property name = this.getProperty("name");
        //if (name != null){
        //   if name.equals(e.getProperty("name")){
        //     return true;
        //   return false;
        //

        if (properties.size() != e.properties.size()) {
            return false;
        }
        for (Property p : properties) {
            if (!p.equals(e.getProperty(p.getType()))) {
                //logger.warn("Properties values not equal");
                return false;
            }
        }
        if (this.references.size() != e.references.size()) {
            logger.warn(" refs size {} != {}", references.size(), e.references.size());
            return false;
        }
        /*
         * Iterator<ElementRefs> i1 = this.references.iterator();
         * Iterator<ElementRefs> i2 = e.references.iterator(); while
         * (i1.hasNext()) { ElementRefs r1 = i1.next(); ElementRefs r2 =
         * i2.next(); if (!r1.equals(r2)) { return false; } }
         */
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (Property p : properties) {
            result = prime * result + p.hashCode();
        }
        /*
         * watch out for recursion ! for (ElementRefs r : this.references) {
         * result = prime * result + r.hashCode(); }
         */
        return result;
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this,
                // 9/20/10 1:45
                // PM
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("type", this.elementType.getName());
        sb.append("properties [");
        for (Property p : properties) {
            sb.append(p.toString());
        }
        sb.append("]");
        sb.append("refs [");
        for (ElementRefs er : references) {
            sb.append(er.getRefName());
        }
        sb.append("]");
        return sb.toString();
    }

    public void fillElementMap(Element elem, List<String> propertyList, Long rev,
                               Map<String, Object> mapElem) {
        for (Property p : elem.getProperties(propertyList)) {
            PropertyValueList plElem = p.getValueList(rev);
            if (plElem == null)
                continue;
            if (plElem.getDims() != null && !plElem.getDims().isEmpty()) {
                List<Object> l = new ArrayList<Object>();
                int last = plElem.getDims().size() - 1;
                Integer d = 0;
                setMatrix(plElem.getDims().get(last), d, plElem.getDims(), plElem.getValues().iterator(), l);
                logger.debug("Property {} refname {}  is vector  value {}",
                        p.getName(), p.getElement().getRefName(), plElem.getValue(0));
                mapElem.put(p.getName(), l);
            } else {
                logger.debug("Property {} is scalar  value {} id: {}", p.getName(),
                        plElem.getValue(0), elem.getId());
                mapElem.put(p.getName(), plElem.getValue(0));
            }
        }
    }

    /**
     * convert this element to a map
     *
     * @param pnames list of property names to be converted
     *               all if null (including type/modcomment/changed/changer/changername)
     *               all if empty (excluding type/modcomment/changed/changer/changername)
     * @param rev    id of modification (last if null)
     * @return map containing this elements properties and references
     */
    public Map<String, Object> toMap(List<String> pnames, Long rev) {
        Map<String, Object> map = new HashMap<String, Object>();
        logger.debug("toMap  pnames: {}", pnames);
        if (rev == null)
            rev = TableModification.MaxRevision;
        if (getId() != null && elementType.getBeanId() != null) {
            map.put(elementType.getBeanId(), getId());
        }
        if (getVersion() != null && elementType.getBeanVersionId() != null) {
            map.put(elementType.getBeanVersionId(), getVersion());
        }
        if (pnames == null || pnames.contains("type")){
            map.put("type", elementType.getName());
        }

        // last modification parameters
        TableModification lastMod = null;
        try {
            lastMod = getLastModification();
        } catch (CoreException e) {}
        if ((pnames == null ||
             pnames.contains("modcomment") ||
             pnames.contains("changed") ||
             pnames.contains("changer") ||
             pnames.contains("changername")) &&
            lastMod != null) {
            /* modification attribute comment */
            if (pnames == null || pnames.contains("modcomment")) {
                if (lastMod.getComment() != null){
                    map.put("modcomment", lastMod.getComment());
                }
            }
            /* modification attribute timestamp */
            if (pnames == null || pnames.contains("changed")) {
                map.put("changed", lastMod.getTimestamp());
            }
            /* modification attribute user (username) */
            if (pnames == null || pnames.contains("changer")) {
                if (lastMod.getUser() != null) {
                    map.put("changer", lastMod.getUser().getUsername());
                }
            }
            /* modification attribute user (full name) */
            if (pnames == null || pnames.contains("changername")) {
                if (lastMod.getUser() != null) {
                    map.put("changername", lastMod.getUser().getFullName());
                }
            }
        }

        if (pnames != null && !pnames.isEmpty()) { // extract selected values only
            if (pnames.contains("owner") && getOwner() != null) {
                map.put("owner", getOwner().getUsername());
            }
            if (pnames.contains("ownername") && getOwner() != null) {
                map.put("ownername", getOwner().getFullName());
            }
            if (pnames.contains("group") && getGroup() != null) {
                map.put("group", getGroup().getName());
            }
            logger.debug("extracted elements {}", getProperties(pnames).size());
            Map<Element, List<String>> elemPropList = getProperties(pnames, rev);
            for (Entry<Element, List<String>> e : elemPropList.entrySet()) {
                logger.debug("Element {}   PropertyValue {}", e.getKey().getTypeName(), e.getValue());
                Map<String, Object> mapElem = new HashMap<String, Object>();

                // projections contains '.' _projection=(magn_temp,results.machine.tq,results.machine.eff...
                if (e.getValue().toString().contains(".")) {

                    Map<String, Object> mapElemChildRoot = new HashMap<String, Object>();
                    for (String propNames : e.getValue()) {
                        Map<String, Object> mapElemChild = mapElemChildRoot;
                        Element eChild = e.getKey();
                        List<String> sList = Arrays.asList(propNames.split("\\."));
                        String propName = sList.get(sList.size() - 1);
                        for (int i = 0; i < sList.size() - 1; ++i) {
                            String key = sList.get(i);
                            eChild = eChild.getElementRefList(key, rev).getElementList().get(0);
                            if (!mapElemChild.containsKey(key)) {
                                logger.debug("new child key: {}", key);
                                mapElemChild.put(key, new HashMap<String, Object>());
                            }
                            mapElemChild = (Map<String, Object>) mapElemChild.get(key);
                        }
                        ArrayList<String> pcname = new ArrayList<String>();
                        pcname.add(propName);
                        this.fillElementMap(eChild, pcname, rev, mapElemChild);
                    }
                    mapElem.putAll(mapElemChildRoot);
                } else {
                    // simple projection
                    this.fillElementMap(e.getKey(), e.getValue(), rev, mapElem);
                }

                if (e.getKey().getRefName().isEmpty()) {
                    map.putAll(mapElem);
                } else {
                    map.put(e.getKey().getRefName(), mapElem);
                }
            }
        } else { // extract all values
            if (getOwner() != null) {
                map.put("owner", getOwner().getUsername());
                map.put("ownername", getOwner().getFullName());
            }
            if (getGroup() != null) {
                map.put("group", getGroup().getName());
            }
            for (Property p : getProperties()) {
                PropertyValueList pl = p.getValueList(rev);
                if (pl == null)
                    continue;
                if (pl.getDims() != null && !pl.getDims().isEmpty()) {
                    logger.debug("Property {} is matrix or list dims {}",
                            p.getName(), pl.getDims().toString());
                    List<Object> l = new ArrayList<Object>();
                    Integer d = 0;
                    int last = pl.getDims().size() - 1;
                    setMatrix(pl.getDims().get(last), d, pl.getDims(), pl.getValues().iterator(), l);
                    map.put(p.getName(), l);
                } else {
                    logger.debug("Property {} is scalar", p.getName());
                    map.put(p.getName(), pl.getValue(0));
                }
            }
            for (ElementRefs r : this.getReferences()) {
                List<Element> el = r.getListOfElements(rev);
                if (el == null || el.size() == 0)
                    continue;
                List<Map<String, ?>> lm = new ArrayList<Map<String, ?>>();
                for (Element e : el) {
                    List<String> pnames_2 = new ArrayList<>();  // todo: reuse pnames?
                    if (e != null) lm.add(e.toMap(pnames_2, rev));
                }
                map.put(r.getRefName(), lm);
            }
        }
        return map;
    }

    public Map<String, Object> toMap() {
        return toMap(null, null);
    }

    public Map<String, Object> toMap(List<String> pnames) {
        return toMap(pnames, null);
    }

    public Map<String, Object> toMap(Long rev) {
        return toMap(null, rev);
    }

    private void setMatrix(int n, Integer d, List<Integer> dims,
                           Iterator<PropertyValue> valiter, List<Object> l) {

        if (d < dims.size() - 1) {
            for (int i = 0; i < dims.get(d); i++) {
                logger.debug("new list {}", d);
                List<Object> childlist = new ArrayList<Object>();
                setMatrix(n, d + 1, dims, valiter, childlist);
                l.add(childlist);
            }
            return;
        }
        logger.debug("adding {}", n);
        List<Object> dlist = new ArrayList<Object>();
        for (int i = 0; i < n; i++) {
            if (valiter.hasNext()) {
                dlist.add(valiter.next().getValue());
            }
        }
        // find last not null object
        int e = n;
        if (e < 1 || e > dlist.size()) {
            l.add(dlist);
            return;
        }
        for (; dlist.get(e - 1) == null; e--) {
        }
        l.addAll(dlist.subList(0, e));
    }

    public void indent(final int ind) {
        System.out.print(ind + ">");
        for (int i = 0; i < ind; i++) {
            System.out.print("...");
        }
    }

    public void resetPrinting() {
        if (!printing) {
            return;
        }
        printing = false;
        for (ElementRefs r : this.references) {
            r.resetPrinting();
        }
    }

    public void print(final int ind, final String comment) {
        if (ind == 0) {
            resetPrinting();
        }
        indent(ind);
        System.out.println("BEGIN: =========> " + comment + " <=========");
        indent(ind);
        final ToStringBuilder sb = new ToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("serialVersionUID", serialVersionUID);
        sb.append("version", version);
        sb.append("type", this.elementType.toString());
        System.out.println(sb);
        if (!printing) {
            printing = true;
            for (TableModification h : this.modifications) {
                h.print(ind + 1);
            }

            for (Property p : this.properties) {
                p.print(ind + 1);
            }

            for (ElementRefs r : this.references) {
                r.print(ind + 1);
            }
        }
        indent(ind);
        System.out.println("END: =========> " + comment + " <=========");
    }

    /**
     * assign element e to this element
     *
     * @param from              source element
     * @param knownElements     list of already known elements
     * @param elementRepository element repository
     * @throws CoreException in case of error
     */
    // from = newElement knowElemnts -> List with element from DB or new created Element (not found in db)
    public void assign(Element from, List<Element> knownElements,
                       ElementRepositoryJpa elementRepository) throws CoreException {
        logger.debug("begin of Element::assign()");

        assert (elementType != null && from.elementType != null);
        assert (elementType.equals(from.elementType));

        setOwner(from.getOwner());
        setGroup(from.getGroup());

        // assign properties
        for (Property propFrom : from.properties) {
            logger.debug("Assign property {}", propFrom.getName());
            final Property propTo = getProperty(propFrom.getType());
            if (propTo != null) {

                if (logger.isDebugEnabled() && !propTo.equals(propFrom)) {
                    logger.debug("assign modified Property {}", propFrom.getName());
                }
                propTo.assign(propFrom);
            } else {
                if (propFrom.isValid()) {
                    propFrom.initRevision(getNewRevision());
                    propFrom.setElement(this);
                    logger.debug("add new Property {}", propFrom.getName());
                    this.properties.add(propFrom);
                }
            }
        }
        // remove properties not in from element:
        Iterator<Property> piter = this.properties.iterator();
        while (piter.hasNext()) {
            Property propTo = piter.next();
            if (!from.properties.contains(propTo)) {
                propTo.delete();
                //piter.remove();
            }
        }
        final List<ElementRefs> fromRefsList = new ArrayList<ElementRefs>(
                from.references);

        // assign refs
        for (ElementRefs fromRef : fromRefsList) {
            ElementRefs toRefs = getElementRefs(fromRef.getRefName());
            if (toRefs == null) {
                logger.debug("ElementRefs '{}' not available. Add new one",
                        fromRef.getRefName());
                toRefs = new ElementRefs(this, fromRef);
            }
        }
        for (ElementRefs fromRef : fromRefsList) {
            ElementRefs toRefs = getElementRefs(fromRef.getRefName());
            toRefs.assign(fromRef, knownElements, elementRepository);
        }
        // search for refs missing in from
        final Iterator<ElementRefs> ri = this.references.iterator();
        while (ri.hasNext()) {
            final ElementRefs r = ri.next();
            if (from.getElementRefs(r.getRefName()) == null) {
                r.assign(null, knownElements, elementRepository);
            }
            //ri.remove();
        }
    }

    /**
     * eliminates all invalid Properties and Elementrefs of this element
     *
     * @return true if all properties and references are empty
     * @throws CoreException
     */
    public boolean crunch() throws CoreException {
        logger.debug("begin of Element::crunch() {}", getTypeName());
        if (!crunched) {
            crunched = true;
            // Properties
            final Iterator<Property> pi = this.properties.iterator();
            while (pi.hasNext()) {
                final Property p = pi.next();
                if (p.isntValid()) {
                    logger.debug("remove invalid property {}", p.getType()
                            .getName());
                    pi.remove();
                }
            }
            // Element-References
            final Iterator<ElementRefs> ri = this.references.iterator();
            while (ri.hasNext()) {
                final ElementRefs refs = ri.next();
                if (refs.crunch()) {
                    ri.remove();
                }
            }
            logger.debug("end of crunch()");
        }
        return this.properties.isEmpty() && this.references.isEmpty();
    }

    // for debugging
    public boolean checkParent() {
        boolean wrong = false;
        for (Property p : this.properties) {
            if (p.checkParent(this)) {
                logger.debug("Property {} with wrong Parent", p.getName());
                wrong = true;
            }
        }
        for (ElementRefs r : this.references) {
            if (r.checkParent(this)) {
                logger.debug("Elementrefs {} with wrong Parent", r.getRefName());
                wrong = true;
            }
        }
        return wrong;
    }

    /**
     * get name of element type
     *
     * @return name of element type
     */
    public String getTypeName() {
        return elementType.getName();
    }

    /**
     * get version (used for optimistic locking
     *
     * @return version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * set version (used for optimistic locking)
     */
    public void setVersion(final Long version) {
        this.version = version;
    }

    /**
     * mark this element as deleted
     *
     * @throws CoreException
     */
    public void setStateDeleted() throws CoreException {
        TableModification m = getNewModification();
        m.setComment("deleted");
        m.setUser(getOwner()); // TODO: set username of invoker
        m.setNextRevision(m.getRevision());
    }

    void setLoaded() {
        if (!isLoaded) {
            this.isLoaded = true;
            for (ElementRefs r : references) {
                r.setLoaded();
                logger.debug("loaded {}", r.getRefName());
            }
        }
    }

    boolean isLoaded() {
        return isLoaded;
    }

    public Group getGroup() {
        return this.ogroup;
    }

    public void setGroup(Group group) {
        this.ogroup = group;
    }

} // End of Element
