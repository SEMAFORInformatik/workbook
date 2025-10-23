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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "property_value_list")
//@org.hibernate.annotations.Table(appliesTo="property_value_list", indexes = { @Index(name="idx", columnNames = { "next_revision" } ) } )
public class PropertyValueList implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PropertyValueList.class);
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prop_list_seq_gen")
    @SequenceGenerator(name = "prop_list_seq_gen", sequenceName = "PROP_LIST_SEQ", allocationSize = 50)
    private Long id = null;

    @OneToMany(mappedBy = "valuelist", cascade = {CascadeType.ALL})
    //CascadeType.MERGE, CascadeType.REMOVE, CascadeType.PERSIST})
    @OrderBy("id asc")
    private List<PropertyValue> values = new ArrayList<PropertyValue>();

    @ElementCollection
    @CollectionTable(
            name = "propertyvaluelist_dimensions",
            joinColumns = @JoinColumn(name = "propertyvaluelist_id")
    )
    private List<Integer> dims = null;

    @ManyToOne
    @JoinColumn(name = "property_id")
    private Property property = null;

    @Column(nullable = false)
    private Long revision;

    @Column(nullable = false)
    //@Index(name = "next_rev_indx")
    private Long nextRevision;

    // Constructor
    public PropertyValueList() {
        super();
    }

    // Constructor
    public PropertyValueList(final Property pr) throws CoreException {
        super();
        //ToDo PMD: NullAssignment
        this.id = null;
        this.property = pr;
        this.revision = pr.getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        this.property.addValueList(this);
    }

    // Constructor
    private PropertyValueList(final PropertyValueList vlist, final Property p) throws CoreException {
        super();
        this.property = p;
        this.revision = this.property.getNewRevision();
        this.nextRevision = TableModification.MaxRevision;
        copyValues(vlist);
        this.property.addValueList(this);
        logger.debug("this rev {} vlist next {}", this.revision, vlist.getNextRevision());
    }

    private void copyValues(final PropertyValueList vlist) {
        this.values = new ArrayList<PropertyValue>();
        for (PropertyValue v : vlist.values) {
            this.values.add(new PropertyValue(this, v));
        }
    }

    public boolean isTransient() {
        return this.id == null;
    }

    public boolean isPersistent() {
        return this.id != null;
    }

    public boolean isInRevision(final long rev) {
        if (rev == TableModification.MaxRevision) {
            return this.nextRevision == TableModification.MaxRevision;
        }
        return rev >= this.revision && rev < this.nextRevision;
    }

    public boolean isLastRevision() {
        return this.nextRevision == TableModification.MaxRevision;
    }

    public Property getProperty() {
        return this.property;
    }

    // getter and setter of Property
    public void setProperty(final Property property) {
        this.property = property;
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

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public boolean isValid() {
        if (this.values.isEmpty()) {
            return false;
        }
        for (PropertyValue v : values) {
            if (v.isValid()) {
                return true;
            }
        }
        return false;
    }

    public boolean isntValid() {
        return !isValid();
    }

    // getter of Values
    public Object getValue(int index) {
        final PropertyValue v = values.size() > index ? values.get(index) : null;
        return v != null ? v.getValue() : null;
    }

    public Date getDate(int index) {
        final PropertyValue v = values.get(index);
        return v != null ? v.getDate() : null;
    }

    public GregorianCalendar getGregorianDate(final int index) {
        final PropertyValue v = values.get(index);
        return v != null ? v.getGregorianDate() : null;
    }

    public List<Integer> getDims() {
        return dims;
    }

    /**
     * set dimension vector of this list (only relevant for matrices)
     *
     * @param dims
     */
    public void setDims(final List<Integer> dims) {
        if (this.dims == null || dims.size() != this.dims.size()) {
            this.dims = new ArrayList<Integer>();
            for (int i = 0; i < dims.size(); i++) {
                this.dims.add(dims.get(i));
            }
            return;
        }
        for (int i = 0; i < dims.size(); i++) {
            this.dims.set(i, dims.get(i));
        }
    }

    // setter of Values
    public PropertyValueList setValue(final int index, final PropertyValue value)
            throws CoreException {
        PropertyValueList vlist = this;
        boolean valueChanged = false;

        if (isPersistent()) {
            if (vlist.values.size() <= index) {
                valueChanged = true;
            } else {
                final PropertyValue val = values.get(index);
                if (val == null) {
                    valueChanged = true;
                } else {
                    if (!val.equals(value)) {
                        valueChanged = true;
                    }
                }
            }
            if (!valueChanged) {
                return this;
            }
            vlist = new PropertyValueList(this, this.property);
        }
        if (vlist.values.size() < index + 1) {
            final PropertyValue new_value = new PropertyValue(vlist, value);
            vlist.values.add(index, new_value);
        } else {
            final PropertyValue cur_value = vlist.values.get(index);
            if (cur_value == null) {
                throw new CoreException("FATAL: Expected Value not available");
            }
            cur_value.setValue(value);
        }
        return vlist;
    }

    public PropertyValueList setString(final int index, final String val)
            throws CoreException {
        final PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    public PropertyValueList setDouble(final int index, final Double val)
            throws CoreException {
        final PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    public PropertyValueList setInt(final int index, final Integer val)
            throws CoreException {
        final PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    public PropertyValueList setLong(final int index, final Long val) throws CoreException {
        PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    public PropertyValueList setDecimal(final int index, final BigDecimal val) throws CoreException {
        PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    public PropertyValueList setDate(final int index, final GregorianCalendar val)
            throws CoreException {
        PropertyValue value = new PropertyValue(null, val);
        return setValue(index, value);
    }

    // getter and setter of Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        if (this.dims != null) {
            sb.append("dims", this.dims.toString());
        }
        sb.append("revision", revision + " -> " + nextRevision);
        sb.append("size", this.values.size());
        for (PropertyValue v : values) {
            sb.append(v.toString());
        }
        return sb.toString();
    }

    private void indent(final int ind) {
        property.getElement().indent(ind);
    }

    public void print(final int ind) {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("revision", revision + "-" + nextRevision);
        sb.append("size", this.values.size());
        indent(ind);
        System.out.println(sb);
        if (this.values.size() > 0) {
            indent(ind + 1);
            System.out.print("(");
            for (PropertyValue v : this.values) {
                System.out.print(" ");
                System.out.print(v.toString());
            }
            System.out.println(" )");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            // logger.debug("identical");
            return true;
        }
        if (!(o instanceof PropertyValueList vlist)) {
            // logger.debug("different class");
            return false;
        }
        if (this.values.size() != vlist.values.size()) {
            return false;
        }
        if (this.values.size() == 0) {
            return true;
        }

        Iterator<PropertyValue> v1 = this.values.iterator();
        Iterator<PropertyValue> v2 = vlist.values.iterator();
        while (v1.hasNext()) {
            final PropertyValue pv1 = v1.next();
            final PropertyValue pv2 = v2.next();
            if (pv1 == null) {
                if (pv2 != null && !pv2.isNull()) {
                    return false;
                }
            } else if (pv2 == null) {
                if (!pv1.isNull()) {
                    return false;
                }
            } else if (!pv1.equals(pv2)) {
                // logger.debug("different values");
                return false;
            }
        }
        // logger.debug("equal values: size "+values.size()
        // +"/"+p.values.size()+" hashcode " + values.hashCode() + "/" +
        // p.values.hashCode() );

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (property != null ? property.hashCode() : 0);
        result = prime * result + (values != null ? values.hashCode() : 0);
        return result;
    }

    public void assign(final PropertyValueList src_vlist) throws CoreException {
        logger.debug("begin of PropertyValueList::assign()");

        PropertyValueList dest_vlist = this;
        if (isTransient()) {
            if (!isEmpty()) {
                logger.debug("--> transient valuelist is NOT empty ?!");
            }
        }
        if (src_vlist.isEmpty()) {
            throw new CoreException("a source-valuelist must have values");
        }

        if (src_vlist.values.size() != dest_vlist.values.size()) {
            // The values are modified. We need to create a
            // new container
            if (dest_vlist.isTransient()) {
                logger.debug("--> update of transient valuelist");
            } else {
                dest_vlist.setNextRevision(this.property.getNewRevision());
                dest_vlist = new PropertyValueList(this.property);
                logger.debug("--> new valuelist created");
            }
            dest_vlist.copyValues(src_vlist);
        } else {
            for (int i = 0; i < src_vlist.values.size(); i++) {
                // if some value is modified, eventually a new
                // ValueList will be created and the existing gets 'historic'.
                dest_vlist = dest_vlist.setValue(i, src_vlist.values.get(i));
            }
            if (dest_vlist != this) {
                logger.debug("--> values updated in valuelist");
            }
        }

        if (src_vlist.dims != null) {
            dest_vlist.dims = new ArrayList<Integer>();
            for (Integer i : src_vlist.dims) {
                dest_vlist.dims.add(i);
            }
        }

        if (dest_vlist.values.size() == 0) {
            logger.debug("--> error: a valuelist must have values: Element: " + getProperty().getElement().toString());
            throw new CoreException("a valuelist must have values");
        }
        logger.debug("end of PropertyValueList::assign()");
    }

    public List<PropertyValue> getValues() {
        return this.values;
    }

}
