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

import jakarta.persistence.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "property_types",
        uniqueConstraints =
        @UniqueConstraint(columnNames = {"name", "unit"}))
public class PropertyType implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prop_type_seq_gen")
    @SequenceGenerator(name = "prop_type_seq_gen", sequenceName = "PROP_TYPE_SEQ", allocationSize = 50)
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
    private String unit;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    public PropertyType() {
        super();
    }

    public PropertyType(final String type) {
        super();
        this.name = type;
        this.type = Type.STRING;
    }

    public PropertyType(final String name, final Type type, String unit) {
        super();
        this.name = name;
        this.type = type;
        this.unit = unit;
    }

    public PropertyType(final String name, final Type type) {
        super();
        this.name = name;
        this.type = type;
    }

    // getter and setter of Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // getter and setter of Name
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Type getType() {
        return this.type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public String toString() {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        sb.append("serialVersionUID", serialVersionUID);
        sb.append("name", this.name);
        sb.append("type", this.type);
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyType t)) {
            return false;
        }

        if (name != null) {
            return name.equals(t.getName());
        }
        return t.getName() == null;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (name != null ? name.hashCode() : 0);
    }

    public enum Type {
        LONG("java.lang.Long"),
        INTEGER("java.lang.Integer"),
        REAL("java.lang.Double"),
        STRING("java.lang.String"),
        BOOL("java.lang.Boolean"),
        CDATA("CDATA"),
        DATE("java.util.Date"),
        SHORT("java.lang.Short"),
        DECIMAL("java.math.BigDecimal"),
        ANY("Object");
        private static final Map<String, Type> lookup =
                new HashMap<String, Type>();

        static {
            for (Type t : EnumSet.allOf(Type.class))
                lookup.put(t.getName(), t);
            // add some synonyms
            lookup.put("STRING", Type.STRING);
            lookup.put("REAL", Type.REAL);
            lookup.put("INTEGER", Type.INTEGER);
            lookup.put("LONG", Type.LONG);
            lookup.put("DATE", Type.DATE);
        }

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public static Type get(String name) {
            Type t = lookup.get(name);
            if (t == null) {
                t = Type.STRING;
            }
            return t;
        }

        public String getName() {
            return name;
        }
    }
}
