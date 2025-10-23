/*
 * Copyright 2010-2025 Semafor Informatik & Energie AG, Basel, Switzerland
 *
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
@Table(name = "elementrefs_lists")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ElementRefList implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ElementRefList.class);
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elem_ref_list_seq_gen")
    @SequenceGenerator(name = "elem_ref_list_seq_gen", sequenceName = "ELEM_REF_LIST_SEQ", allocationSize = 50)
    private Long id;

    @Column(nullable = false)
    private Long revision;

    @Column(nullable = false)
    private Long nextRevision;

    @ManyToOne
    @JoinColumn(name = "elementrefs_id")
    private ElementRefs parent;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    //CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    @JoinTable(name = "elementrefs_elements", joinColumns = @JoinColumn(name = "ref_id"),
            inverseJoinColumns = @JoinColumn(name = "element_id"))
    private List<Element> elementlist = null;

    // Constructor
    public ElementRefList() {
        super();
    }

    // Constructor
    public ElementRefList(final ElementRefs refs) throws CoreException {
        super();
        // ToDo PMD: NullAssignment, code smell
        this.id = null;
        this.parent = refs;
        this.revision = getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        this.parent.addElementRefList(this);
    }

    // Constructor
    public ElementRefList(final ElementRefList reflist, final ElementRefs refs)
            throws CoreException {
        super();
        // ToDo PMD: NullAssignment, code smell
        this.id = null;
        this.parent = refs;
        this.revision = getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        reflist.setNextRevision(this.revision);
        this.parent.addElementRefList(this);
    }

    // getter and setter of Id
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getRevision() {
        return this.revision;
    }

    // getter and setter of Revision
    public void setRevision(final long rev) {
        this.revision = rev;
    }

    public Long getNextRevision() {
        return this.nextRevision;
    }

    // getter and setter of NextRevision
    public void setNextRevision(final long rev) {
        this.nextRevision = rev;
    }

    public ElementRefs getElementRefs() {
        return this.parent;
    }

    // getter and setter of Property
    public void setElementRefs(final ElementRefs refs) {
        this.parent = refs;
    }

    public List<Element> getElementList() {
        if (this.elementlist == null) {
            this.elementlist = new ArrayList<Element>();
        }
        return this.elementlist;
    }

    // getter and setter of ElementList
    public void setElementList(final List<Element> ellist) {
        this.elementlist = ellist;
    }

    public boolean isTransient() {
        return this.id == null;
    }

    public boolean isPersistent() {
        return this.id != null;
    }

    public boolean isInRevision(final long rev) {
        if (this.revision == null) return false;
        if (rev == TableModification.MaxRevision) {
            return this.nextRevision == rev;
        }
        return rev >= this.revision && rev < this.nextRevision;
    }

    public boolean isLastRevision() {
        return this.nextRevision == TableModification.MaxRevision;
    }

    // New Revision for current Transaction
    private long getNewRevision() throws CoreException {
        return this.parent.getNewRevision();
    }

    public void createNewRevision() throws CoreException {
        setNextRevision(this.parent.getNewRevision());
    }

    public boolean isEmpty() {
        return this.elementlist.isEmpty();
    }

    public boolean isntEmpty() {
        return !this.elementlist.isEmpty();
    }

    public void addElement(Element el) throws CoreException {
        if (el == null) {
            throw new CoreException("Element pointer is null");
        }
        if (this.elementlist == null) {
            this.elementlist = new ArrayList<Element>();
        }
        final Long entityId = el.getId();
        if (entityId != null) {
            logger.debug("Checking elementlist of {}", entityId);
            for (Element e : this.elementlist) {
                logger.debug(" {}", e.getId());
                if (e.getId().equals(entityId)) {
                    logger.warn(
                            "Element #{} (type {}) already included in ElementRefList",
                            entityId, el.getElementType().getName());
                    /*
                     * throw new CoreException("Element #"+entityId+
                     * " (type "+el.getElementType
                     * ().getName()+") already included in ElementRefList");
                     */
                    return;
                }
            }
        }
        this.elementlist.add(el);
    }

    public void assign(final ElementRefList from, List<Element> knownElements1,
                       final ElementRepositoryJpa elementRepository) throws CoreException {
        logger.debug("begin of ElementRefList::assign()");

        logger.debug("Known Elements: {}", knownElements1.size());
        logger.debug("New Elements: {}", from.elementlist.size());

        final List<Element> toElementsNew = new ArrayList<Element>();
        List<Element> knownElements = new ArrayList<Element>();
        boolean changed = false;
        for (Element fromElement : from.elementlist) {
            Element toElement = getMatchingElement(knownElements, fromElement);
            if (toElement != null) {
                logger.debug(
                        "getMatchingElement(): Element {} #{} found isLoaded {}",
                        toElement.getTypeName(), toElement.getId(),
                        toElement.isLoaded());

                if (fromElement.isPersistent() && !toElement.isLoaded()) {
                    toElement = elementRepository.findById(fromElement.getId()).orElse(null);
                    if (toElement != null) toElement.setLoaded(); // to prevent another load

                    if (logger.isDebugEnabled()) {
                        fromElement.print(10,
                                "ElementRefList: get matching persistent element "
                                        + fromElement.getTypeName());
                    }
                }
                // toElement.assign(fromElement, knownElements, elementDao);
                toElementsNew.add(toElement);
            } else {
                logger.debug("getMatchingElement(): No match: new Element added");
                if (fromElement.isPersistent()) {
                    final Element existing = elementRepository.findById(fromElement.getId()).orElse(null);
                    if (existing != null) {
                        if (logger.isDebugEnabled()) {
                            existing.print(10, "ElementRefList: get new persistent element "
                                    + existing.getTypeName());
                        }
                        fromElement = existing;
                    } else {
                        logger.debug("not yet saved");
                    }
                } else {
                    // ToDo PMD: AvoidInstantiatingObjectsInLoops
                    final Element new_el = new Element(fromElement.getElementType());
                    knownElements.add(new_el);
                    new_el.assign(fromElement, knownElements, elementRepository);
                    fromElement = new_el;
                }
                toElementsNew.add(fromElement);
                changed = true;
            }
        }
        if (knownElements.size() > 0) {
            logger.debug("remaining old elements: {}", knownElements.size());
            changed = true;
        }
        ElementRefList dest_reflist = this;
        if (changed) {
            if (isPersistent()) {
                logger.debug("Change detected: create new ElementRefs from #{}",
                        this.getId());
                dest_reflist = new ElementRefList(this, this.parent);
            }
        }
        dest_reflist.elementlist = toElementsNew;
        logger.debug("end of ElementRefs::assign()");
    }

    private Element getMatchingElement(final List<Element> elementlist,
                                       final Element src_el) {
        logger.debug("getMatchingElement: {}  Elements in list", elementlist.size());
        for (Element e : elementlist) {
            if (e.getElementType().equals(src_el.getElementType())) {
                 if (src_el.getId() != null) {
                     if (src_el.getId().equals(e.getId())) {
                         return e;
                     }
                 }
                 // we cannot decide:
                 return null;

            } else {
                logger.debug("getMatchingElement: not of same type");
            }
        }
        return null;
    }

    public boolean crunch() throws CoreException {
        logger.debug("begin of ElementRefList::crunch()");

        // Elements
        if (this.elementlist != null) {
            Iterator<Element> ei = this.elementlist.iterator();
            // 9/20/10 1:30 PM
            while (ei.hasNext()) {
                Element el = ei.next();
                if (el.crunch()) {
                    ei.remove();
                }
            }
        }
        logger.debug("end of ElementRefList::crunch()");
        if (this.elementlist != null) {
            return isEmpty();
        }
        return true; // no list
    }

    // marks a list as invalid
    public void delete() throws CoreException {
        if (isTransient()) {
            throw new CoreException(
                    "it is not allowed to delete a new ElementRefList");
        }
        if (getNextRevision() != TableModification.MaxRevision) {
            throw new CoreException(
                    "it is not allowed to delete an already deleted ElementRefList");
        }
        createNewRevision();
    }

    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this,
                // 1:30 PM
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("revision", revision + "-" + nextRevision);
        sb.append("size", this.elementlist.size());
        for (Element e : this.elementlist) {
            sb.append(e.getTypeName());
        }
        return sb.toString();
    }

    private void indent(final int ind) {
        this.parent.indent(ind);
    }

    public void print(final int ind) {
        ToStringBuilder sb = new ToStringBuilder(this,
                // 1:30 PM
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("revision", revision + "-" + nextRevision);
        if (this.elementlist == null) {
            sb.append("elementlist is null");
        } else {
            sb.append("size", this.elementlist.size());
        }
        indent(ind);
        // ToDo really needed, PMD: SystemPrintln
        System.out.println(sb);
        if (this.elementlist != null) {
            if (this.elementlist.size() > 0) {
                int index = 0;
                for (Element e : this.elementlist) {
                    index++;
                    e.print(ind + 1, "Reference #" + index);
                }
            }
        }
    }

    // compares two objects
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElementRefList reflist)) {
            return false;
        }
        return equalsElementList(reflist.getElementList());
    }

    /**
     * check if lists are equal
     *
     * @param other list to be compared
     * @return true if lists are equal
     */
    public boolean equalsElementList(final List<Element> other) {
        final List<Element> this_ellist = this.elementlist;

        if (this_ellist == null) {
            return other == null;
        } else {
            if (other == null) {
                return false;
            }
        }
        if (this_ellist.size() != other.size()) {
            return false;
        }
        final Iterator<Element> this_it = this_ellist.iterator();
        final Iterator<Element> that_it = other.iterator();

        while (this_it.hasNext()) {
            final Element this_el = this_it.next();
            final Element that_el = that_it.next();
            if (!this_el.equals(that_el)) {
                return false;
            }
        }
        return true;
    }

    // get hash-code
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (this.elementlist != null ? elementlist.hashCode() : 0);
        return result;
    }

    // for debugging
    public boolean checkParent(final ElementRefs refs) {
        return this.parent != refs;
    }

    public void setLoaded() {
        if (elementlist == null) {
            return;
        }
        for (Element e : elementlist) {
            e.setLoaded();
        }
    }

    public void resetPrinting() {
        for (Element e : elementlist) {
            e.resetPrinting();
        }
    }

    public ElementRefs getParent() {
        return this.parent;
    }
}
