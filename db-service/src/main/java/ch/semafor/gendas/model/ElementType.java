/*
 * Copyright 2010 Semafor Informatik & Energie AG, Basel, Switzerland
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package ch.semafor.gendas.model;

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import org.springframework.data.mongodb.core.mapping.Document;

@Entity
@Document(collection = "types")
@Table(name = "element_types")
public class ElementType implements Serializable {
    @org.springframework.data.annotation.Transient
    public static final String SEQUENCE_NAME = "types";
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ElementType.class);
    @ManyToMany(cascade = {CascadeType.PERSIST}) // , CascadeType.PERSIST})
    @JoinTable(name = "element_type_references")
    @MapKeyJoinColumn // (name="refname",columnDefinition="coldef",updatable=true)
    Map<String, ElementType> references = new HashMap<String, ElementType>();
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elem_type_seq_gen")
    @SequenceGenerator(name = "elem_type_seq_gen", sequenceName = "ELEM_TYPE_SEQ", allocationSize = 50)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    private String beanId;
    private String beanVersionId;
    @ManyToMany(
            cascade = CascadeType.PERSIST)
//{CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(name = "element_types_property_types")
    private List<PropertyType> propertyTypes = new ArrayList<PropertyType>();
    @Transient
    private boolean created = false; // break recursion in recursive loop
    @Transient
    private boolean printed = false; // break recursion in recursive loop //

    // Constructor
    public ElementType() {
    }

    // Constructor
    public ElementType(final String name) {
        this.name = name;
    }

    // getter and setter of Id
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        if (this.id == null) {
            this.id = id;
        }
    }

    public void setCreated() {
        created = true;
    }

    public boolean isCreated() {
        return created;
    }

    // getter and setter of Name
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElementType t)) {
            return false;
        }
        return !(name != null ? !name.equals(t.getName()) : t.getName() != null);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
        // 1:54 PM
    }

    public void add(final PropertyType propType) {
        if (propType == null || propertyTypes.contains(propType)) {
            return;
        }
        propertyTypes.add(propType);
    }

    // get PropertyType by Name
    public PropertyType getPropertyType(final String type) {
        for (PropertyType p : propertyTypes) {
            if (p.getName().equals(type)) {
                return p;
            }
        }
        return null;
    }

    // get list of PropertyTypes
    public List<PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public void addReference(final String refName, final ElementType ref) {
        logger.debug("ElementType {} <--- {} ( of {})",
                getName(), ref.getName(), references.size());
        references.put(refName, ref);
    }

    /**
     * check if reference is already included
     *
     * @param refName name of reference to check
     * @return true if reference exist false otherwise
     */
    public boolean hasReference(final String refName) {
        return references.containsKey(refName);
        /*
         * for (ElementType r : references) { if (r.name.equals(canonicalName)) { return true;
         */
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this,
                // 9/20/10 1:54
                // PM
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("name", this.name);
        if (!printed) {
            printed = true;

            if (this.references.size() > 0) {
                sb.append("References: [");
                for (String refname : this.references.keySet()) {
                    sb.append(refname);
                    sb.append(this.references.get(refname).toString());
                }
                sb.append("]");
            }
        }
        return sb.toString();
    }

    /*
     * public List<ElementType> getReferences() { return this.references; }
     */

    public String getBeanId() {
        return beanId;
    }

    public void setBeanId(String beanId) {
        this.beanId = beanId;
    }

    public String getBeanVersionId() {
        return beanVersionId;
    }

    public void setBeanVersionId(String beanVersionId) {
        this.beanVersionId = beanVersionId;
    }

    public boolean isBeanId(String propName) {
        return beanId != null && beanId.equals(propName);
    }

    public boolean isBeanVersionId(String propName) {
        return beanVersionId != null && beanVersionId.equals(propName);
    }

    public ElementType getElementType(String key) {
        return this.references.get(key);
    }

    public boolean isOwner(String propName) {
        return propName != null && propName.equals("owner"); // TODO make it
        // configurable
    }

    public boolean isGroup(String propName) {
        return propName != null && propName.equals("group");
    }

    public Map<String, ElementType> getReferences() {
        return references;
    }

    public void cleanReference() {
        beanId = null;
        beanVersionId = null;
        references = null;
        propertyTypes = null;
    }

    /**
     * Get the element type of a string with endswith used in edm because the elementType 'length' is
     * 'input/Allgemein/length' we have only length as string
     *
     * @param string
     * @return elementtype
     */
    public ElementType getElementTypeOfReference(String string) {
        for (String refname : references.keySet()) {
            // EDM has /input/Allgemein/length as refname and we only want to test length on search string
            String[] splitted_refname = refname.split("/");
            if (splitted_refname[splitted_refname.length - 1].equals(string)) {
                return references.get(refname);
            }
        }
        return null;
    }

}
