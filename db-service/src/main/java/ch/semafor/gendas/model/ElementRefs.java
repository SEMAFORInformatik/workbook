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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "elementrefs")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ElementRefs implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(ElementRefs.class);
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
    //CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    private final List<ElementRefList> reflist = new ArrayList<ElementRefList>();
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elem_ref_seq_gen")
    @SequenceGenerator(name = "elem_ref_seq_gen", sequenceName = "ELEM_REF_SEQ", allocationSize = 50)
    private Long id;
    @Column(nullable = false)
    private Long revision;
    @Column(nullable = false)
    private Long nextRevision;
    private String refname;
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Element parent;
    @ManyToOne
    @JoinColumn(name = "element_type_id", nullable = false)
    private ElementType elementType;

    // Constructor
    public ElementRefs() {
    }

    // Constructor
    public ElementRefs(final Element parent, final String name) throws CoreException {
        logger.debug("new ElementRefs {} to Element {}", name, parent.toString());
        this.refname = name;
        this.parent = parent;
        parent.addElementRefs(this);
    }

    // Constructor
    public ElementRefs(final Element parent, final ElementRefs r) throws CoreException {
        logger.debug("new ElementRefs {} to Element {}", r.getRefName(), parent.toString());
        this.refname = r.getRefName();
        this.elementType = r.getElementType();
        this.parent = parent;
        parent.addElementRefs(this);
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

    // getter and setter of Parent
    public Element getParent() {
        return this.parent;
    }

    public void setParent(Element el) {
        this.parent = el;
    }

    // getter and setter of ElementType
    public ElementType getElementType() {
        return this.elementType;
    }

    public void setElementType(final ElementType elementType) {
        this.elementType = elementType;
    }

    public Long getRevision() {
        return this.revision;
    }

    // setter and getter of Revision
    public void setRevision(final long rev) {
        this.revision = rev;
    }

    public Long getNextRevision() {
        return this.nextRevision;
    }

    // setter and getter of NextRevision
    public void setNextRevision(final long rev) {
        this.nextRevision = rev;
    }

    public boolean isInRevision(final long rev) {
        if (rev == TableModification.MaxRevision) {
            return this.nextRevision == rev;
        }
        return rev >= this.revision && rev < this.nextRevision;
    }

    public boolean isLastRevision() {
        return this.nextRevision == TableModification.MaxRevision;
    }

    // Get the latest available Revisionnumber.
    public long getLastRevision() throws CoreException {
        return this.parent.getLastRevision();
    }

    // New Revision for current Transaction
    public long getNewRevision() throws CoreException {
        return this.parent.getNewRevision();
    }

    public void addElementRefList(final ElementRefList reflist) throws CoreException {
        this.reflist.add(reflist);
    }

    public ElementRefList getElementRefList(final long rev) {
        int inx = this.reflist.size() - 1;
        ElementRefList reflist;
        while (inx >= 0) {
            reflist = this.reflist.get(inx);
            if (reflist.isInRevision(rev)) {
                return reflist;
            }
            inx--;
        }
        return null;
    }

    public ElementRefList getLastElementRefList() {
        return getElementRefList(TableModification.MaxRevision);
    }

    public void addElement(Element e) throws CoreException {
        if (elementType == null) {
            if (reflist.size() > 0) {
                throw new CoreException("ElementType missing in ElementRefs");
            }
            elementType = e.getElementType();
        } else {
            if (!elementType.equals(e.getElementType())) {
                logger.warn("ElementRefs::addElement(): ElementType not equal");
                logger.warn(" - ElementRefs #{} : {} / Element {}", this.id, elementType.getName(),
                        e.getElementType().getName());
                throw new CoreException("ElementType doesnt match");
            }
        }
        ElementRefList reflist = getLastElementRefList();
        if (reflist == null) {
            reflist = new ElementRefList(this);
        } else {
            if (reflist.isPersistent()) {
                reflist = new ElementRefList(reflist, this);
            }
        }
        reflist.addElement(e);
    }

    //ToDo unify (Long|long) in this file
    public List<Element> getListOfElements(final long rev) {
        final ElementRefList reflist = getElementRefList(rev);
        if (reflist != null) {
            return reflist.getElementList();
        }
        return null;
    }

    public List<Element> getLastListOfElements() {
        return getListOfElements(TableModification.MaxRevision);
    }

    public void setListOfElements(final List<Element> ellist) throws CoreException {
        logger.debug("ElementRefs::setListOfElements()");
        if (this.elementType == null) {
            if (reflist.size() > 0) {
                throw new CoreException("ElementType missing in ElementRefs");
            }
            if (ellist == null) {
                throw new CoreException("No parameter Elementlist");
            }
            if (ellist.size() > 0) {
                final Element e = ellist.get(0);
                this.elementType = e.getElementType();
            }
      /*
      else {
        throw new CoreException( "No Elements in Elementlist" );
      }*/
        }

        ElementRefList reflist = getLastElementRefList();
        if (reflist == null) {
            reflist = new ElementRefList(this);
        } else {
            if (!reflist.equalsElementList(ellist)) { // different
                if (reflist.isPersistent()) {
                    reflist = new ElementRefList(reflist, this);
                }
            }
        }
    /*
    for( Element e : ellist ) {
      if( !this.elementType.equals(e.getElementType()) ){
        throw new CoreException( "ElementType doesnt match" );
      }
    }*/
        reflist.setElementList(ellist);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((refname == null) ? 0 : refname.hashCode());
        result = prime * result
                + ((elementType == null) ? 0 : elementType.hashCode());
        ElementRefList last = getLastElementRefList();
        result = prime * result + ((last == null) ? 0 : last.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ElementRefs other = (ElementRefs) obj;
        // compare name
        if (refname == null) {
            if (other.refname != null) {
                return false;
            }
        } else if (!refname.equals(other.refname)) {
            return false;
        }
        // compare type
        if (elementType == null) {
            if (other.elementType != null) {
                return false;
            }
        } else if (!elementType.equals(other.elementType)) {
            return false;
        }
        // compare only last reflist
        final ElementRefList my_reflist = getLastElementRefList();
        final ElementRefList other_reflist = other.getLastElementRefList();
        if (my_reflist == null) {
            return other_reflist == null;
        }
        return my_reflist.equals(other_reflist);
    }

    public void assign(final ElementRefs from, List<Element> knownElements,
                       final ElementRepositoryJpa elementRepository) throws CoreException {
        ElementRefList to = getLastElementRefList();
        if (from == null) {
            to.createNewRevision();
            to = new ElementRefList(this);
            return;
        }
        final ElementRefList fromRefList = from.getLastElementRefList();

        if (fromRefList == null) {
            // close this list
            to.createNewRevision();

            logger.debug("end of ElementRefs::assign(): close reflist");
            return;
        }
        if (to == null) {
            to = new ElementRefList(this);
        }
        to.assign(fromRefList, knownElements, elementRepository);
        logger.debug("end of ElementRefs::assign()");
    }

    public boolean crunch() throws CoreException {
        logger.debug("begin of ElementRefs::crunch()");

        boolean crunched = false;
        final ElementRefList reflist = getLastElementRefList();
        if (reflist != null) {
            crunched = reflist.crunch();
        }
        logger.debug("end of ElementRefs::crunch()");
        return crunched;
    }

    public void delete() throws CoreException {
        if (isTransient()) {
            throw new CoreException("Cannot delete a new ElementReference");
        }
        if (getNextRevision() != TableModification.MaxRevision) {
            throw new CoreException("Cannot delete an already deleted Element");
        }
        setNextRevision(this.parent.getNewRevision());
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("RefName", refname);
        if (elementType == null) {
            sb.append("elementType missing");
        } else {
            sb.append("elementType", elementType.getName());
        }
        sb.append("revision", revision + "-" + nextRevision);
        sb.append("size", this.reflist.size());
        return sb.toString();
    }

    public void indent(final int ind) {
        this.parent.indent(ind);
    }

    public void print(final int ind) {
        indent(ind);
        // ToDo PMD: SystemPrintln
        System.out.println(this);
        for (ElementRefList reflist : this.reflist) {
            reflist.print(ind + 1);
        }
    }

    // for debugging
    public boolean checkParent(Element e) {
        if (this.parent != e) {
            return true; // bad
        }
        if (this.reflist != null) {
            for (ElementRefList reflist : this.reflist) {
                if (reflist.checkParent(this)) {
                    return true; // bad
                }
            }
        }
        return false;
    }

    public void setLoaded() {
        for (ElementRefList reflist : this.reflist) {
            reflist.setLoaded();
        }
    }

    public void resetPrinting() {
        for (ElementRefList reflist : this.reflist) {
            reflist.resetPrinting();
        }
    }

    public String getRefName() {
        return refname;
    }

    public List<ElementRefList> getReflist() {
        return this.reflist;
    }
}