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

import ch.semafor.gendas.exceptions.CoreException;
import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

@Entity
@Table(name = "properties")
public class Property implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Property.class);
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "property", cascade = {CascadeType.ALL})
    //	CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    private final List<PropertyValueList> valuelist = new ArrayList<PropertyValueList>();
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prop_seq_gen")
    @SequenceGenerator(name = "prop_seq_gen", sequenceName = "PROP_SEQ", allocationSize = 50)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "element_id")
    private Element element;

    @ManyToOne
    @JoinColumn(name = "property_type_id")
    private PropertyType type;

    @Column(nullable = false)
    private Long revision;

    @Column(nullable = false)
    private Long nextRevision;

    // Constructor
    public Property() {
        super();
        this.element = null;
    }

    // Constructor
    public Property(final Element el) throws CoreException {
        super();
        this.id = null;
        this.element = el;
        this.revision = el.getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        el.addProperty(this);
    }

    // Constructor
    public Property(Element el, PropertyType type) throws CoreException {
        super();
        this.id = null;
        this.type = type;
        this.element = el;
        this.revision = el.getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        el.addProperty(this);
    }

    public String getName() {
        if (this.type != null) {
            return this.type.getName();
        }
        return "<no type>";
    }

    public boolean isInRevision(final long rev) {
        if (rev == TableModification.MaxRevision) {
            return this.nextRevision == TableModification.MaxRevision;
        }
        return rev >= this.revision && rev < this.nextRevision;
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

    public void initRevision(final long rev) throws CoreException {
        if (isPersistent()) {
            throw new CoreException(
                    String.format("Property '%s' must to be transient",
                            getName()));
        }
        this.revision = rev;
        this.nextRevision = TableModification.MaxRevision;
        if (this.valuelist.size() > 1) {
            throw new CoreException(
                    "Only one ValueList allowed in transient Properties");
        }
        for (PropertyValueList lst : this.valuelist) {
            lst.setRevision(rev);
            lst.setNextRevision(this.nextRevision);
        }
    }

    // Get the latest available Revision number.
    public long getLastRevision() throws CoreException {
        return this.element.getLastRevision();
    }

    // New Revision for current Transaction
    public long getNewRevision() throws CoreException {
        return this.element.getNewRevision();
    }

    public boolean isLastRevision() {
        return this.nextRevision == TableModification.MaxRevision;
    }

    public void addValueList(final PropertyValueList vlist)
            throws CoreException {
        assert this == vlist.getProperty() : "PropertyValueList #" + vlist.getId() + " not attached to this Property";
        Iterator<PropertyValueList> pvi = this.valuelist.iterator();
        PropertyValueList prev = null;
        while (pvi.hasNext()) {
            PropertyValueList lst = pvi.next();
            assert lst != vlist : "ValueList already added to Property";
            if (prev != null) {
                prev.setNextRevision(lst.getRevision());
            }
            prev = lst;
        }
        if (prev != null) {
            prev.setNextRevision(vlist.getRevision());
        }
        this.valuelist.add(vlist);
    }

    public PropertyValueList getValueList(final Long revision) {
        int inx = this.valuelist.size() - 1;
        PropertyValueList vlist;
        while (inx >= 0) {
            vlist = this.valuelist.get(inx);
            if (vlist.isInRevision(revision)) {
                return vlist;
            }
            inx--;
        }
        return null;
    }

    private PropertyValueList getLastValueList() {
        for (PropertyValueList lst : this.valuelist) {
            if (lst.isLastRevision()) {
                return lst;
            }
        }
        return null;
    }

    public Object getValue(final int index) {
        final PropertyValueList vlist = getValueList(TableModification.MaxRevision);
        if (vlist == null) {
            return null;
        }
        return vlist.getValue(index);
    }

    public GregorianCalendar getGregorianDate(final int index) {
        final PropertyValueList vlist = getValueList(TableModification.MaxRevision);
        if (vlist == null) {
            return null;
        }
        return vlist.getGregorianDate(index);
    }

    public void setValue(int index, Object value) throws CoreException {
        logger.debug("{}/{}[{}] (type {}) {}",
                element.getElementType(), getName(), index, type.getType(), value);

        if (value == null) {
            setValue(getLastRevision(), index, new PropertyValue(null));
            return;
        }
        try {
            switch (type.getType()) {
                case STRING:
                    setValue(getLastRevision(), index, new PropertyValue(null, value.toString()));
                    return;
                case INTEGER:
                    setValue(getLastRevision(), index, new PropertyValue(null, ((Number) value).intValue()));
                    return;
                case SHORT:
                    setValue(getLastRevision(), index, new PropertyValue(null, ((Number) value).shortValue()));
                    return;
                case LONG:
                    setValue(getLastRevision(), index, new PropertyValue(null, ((Number) value).longValue()));
                    return;
                case REAL:
                    setValue(getLastRevision(), index, new PropertyValue(null, ((Number) value).doubleValue()));
                    return;
                case BOOL:
                    setValue(getLastRevision(), index, new PropertyValue(null, (Boolean) (value)));
                    return;
                case CDATA:
                    final PropertyValue v = new PropertyValue();
                    v.setCdata((String) value);
                    setValue(getLastRevision(), index, v);
                    return;
                case DATE:
                    if (value instanceof XMLGregorianCalendar d) {
                        setValue(getLastRevision(), index, new PropertyValue(null, d.toGregorianCalendar()));
                        return;
                    }
                    if (value instanceof GregorianCalendar) {
                        setValue(getLastRevision(), index, new PropertyValue(null, (GregorianCalendar) value));
                        return;
                    }
                    setValue(getLastRevision(), index, new PropertyValue(null, (java.util.Date) (value)));
                    return;

                case DECIMAL:
                    if (value != null) {
                        logger.debug("save big decimal {}", value);
                        setValue(getLastRevision(), index, new PropertyValue(null, (BigDecimal) value));
                    }
                    return;

                default:
                    logger.error("Unknown type {}", type.getType());
                    throw new CoreException("Unknown type " + type.getType());
            }
        } catch (ClassCastException ex) {
            logger.error("Conversion error {}", ex.getMessage(), ex);
            throw new CoreException("Conversion error "
                    + ex.getMessage() + " property "
                    + element.getTypeName() + "/" + getName());
        }
    }

    public void setValue(final Long revision, final int index,
                         final PropertyValue value) throws CoreException {
        PropertyValueList vlist = getValueList(revision);
        if (vlist == null) {
            vlist = new PropertyValueList(this);
        }
        vlist.setValue(index, value);
    }


    // getter and setter of Element
    public Element getElement() {
        return element;
    }

    public void setElement(Element el) {
        this.element = el;
    }

    // getter and setter of Type
    public PropertyType getType() {
        return type;
    }

    public void setType(final PropertyType type) {
        this.type = type;
    }

    // getter and setter of Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isId(long id) {
        if (this.id != null) {
            return this.id.longValue() == id;
        }
        return false;
    }

    public boolean isTransient() {
        return this.id == null;
    }

    public boolean isPersistent() {
        return this.id != null;
    }

    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("type", this.type.getName());
        sb.append("revision", revision + "-" + nextRevision);
        for (PropertyValueList v : valuelist) {
            sb.append(v.toString());
        }
        return sb.toString();
    }

    public void print(final int ind) {
        ToStringBuilder sb = new ToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("serialVersionUID", serialVersionUID);
        sb.append("revision", revision + "-" + nextRevision);
        sb.append("type", this.type.toString());
        element.indent(ind);
        System.out.println(sb);
        for (PropertyValueList vlist : this.valuelist) {
            vlist.print(ind + 1);
        }
    }

    public boolean isValid() {
        final PropertyValueList vlist = getLastValueList();
        if (vlist == null) {
            return false;
        }
        return vlist.isValid();
    }

    public boolean isntValid() {
        return !isValid();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Property p)) {
            return false;
        }
        if (type != null) {
            if (!type.equals(p.type)) {
                return false;
            }
        } else if (p.type == null) {
            return true;
        }
        // logger.debug("compare values " + values.size() + "==" +
        // p.values.size());
        final PropertyValueList this_vlist = getValueList(TableModification.MaxRevision);
        final PropertyValueList that_vlist = p.getValueList(TableModification.MaxRevision);

        if (this_vlist == null) {
            logger.debug("no value list");
            return that_vlist == null || that_vlist.isEmpty();
            // logger.debug("no value list");
        }
        if (that_vlist == null) {
            return false;
        }
        return this_vlist.equals(that_vlist);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (type != null ? type.hashCode() : 0);
        // Stackoverflow
		/*
		final PropertyValueList pvl = getValueList(Modification.MaxRevision);
		result = prime * result + (type != null ? pvl.hashCode() : 0); */
        return result;
    }

    public void assign(Property from) throws CoreException {
        logger.debug("begin of Property::assign(): to {}", this.getName());
        if (type == null || from.type == null) {
            throw new CoreException(
                    "Properties without a type not possible in assign()");
        }
        if (type != from.type) {
            throw new CoreException(
                    "A change of PropertyType is not allowed in assign()");
        }
        final PropertyValueList src_vlist = from.getLastValueList();
        if (src_vlist == null) {
            logger.debug("no values available => delete");
            delete();
        } else if (src_vlist.isntValid()) {
            logger.debug("no valid values available in {}: => delete",
                    from.getName());
            delete();
        } else {
            logger.debug("Source Property : {}", src_vlist);
            PropertyValueList dest_vlist = getLastValueList();
            if (dest_vlist == null) {
                logger.debug("create new PropertyValueList");
                dest_vlist = new PropertyValueList(this);
            }
            dest_vlist.assign(src_vlist);
            this.nextRevision = TableModification.MaxRevision;
        }
        logger.debug("end of Property::assign()");
    }

    public void delete() throws CoreException {
        logger.debug("delete Property {}", this);

        if (this.nextRevision != TableModification.MaxRevision) {
            logger.debug("already deleted");
        } else {
            // set invalid
            this.nextRevision = getNewRevision();
            final PropertyValueList vlist = getLastValueList();
            if (vlist.isLastRevision()) {
                vlist.setNextRevision(this.nextRevision);
            }
        }
    }

    public boolean checkParent(Element e) {
        return this.element != e;
    }

    public void setDims(List<Integer> dims) throws CoreException {
        PropertyValueList vlist = getLastValueList();
        if (vlist == null) {
            vlist = new PropertyValueList(this);
        }
        vlist.setDims(dims);
    }

}
