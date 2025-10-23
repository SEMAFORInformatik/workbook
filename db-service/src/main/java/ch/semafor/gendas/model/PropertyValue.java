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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Entity
@Table(name = "property_values")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
// @org.hibernate.annotations.Table(appliesTo="property_values", indexes = {
// @Index(name="idx", columnNames = { "svalue" } ) } )
public class PropertyValue implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "prop_val_seq_gen")
    @SequenceGenerator(name = "prop_val_seq_gen", sequenceName = "PROP_VAL_SEQ", allocationSize = 50)
    Long id;

    // Values
    @ManyToOne
    private PropertyValueList valuelist;
    //@Lob
    //@Index(name = "svalue_indx")
    //@Column(length=16777215)
    private String svalue = null;
    private Double dvalue = null;
    private Integer ivalue = null;
    private Long lvalue = null;
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateValue = null;
    private BigDecimal decimalValue = null;
    private Short bool = null;
    @Lob
    private String text = null;

    public PropertyValue() {
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist) {
        super();
        this.valuelist = vlist;
    }

    // Copy-Constructor
    public PropertyValue(final PropertyValueList vlist, final PropertyValue value) {
        super();
        if (value == null)
            return;
        this.valuelist = vlist;
        this.svalue = value.svalue;
        this.dvalue = value.dvalue;
        this.ivalue = value.ivalue;
        this.lvalue = value.lvalue;
        this.dateValue = value.dateValue;
        this.text = value.text;
        this.bool = value.bool;
        this.decimalValue = value.decimalValue;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist, final String value) {
        this.svalue = value;
        this.valuelist = vlist;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist, final Double value) {
        this.dvalue = value;
        this.valuelist = vlist;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist, final Integer value) {
        this.ivalue = value;
        this.valuelist = vlist;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist, final Long value) {
        this.lvalue = value;
        this.valuelist = vlist;
    }

    public PropertyValue(final PropertyValueList vlist, final Boolean val) {
        // TODO, PMD: AvoidUsingShortType
        this.bool = (short) (val ? 1 : 0);
        this.valuelist = vlist;
    }

    public PropertyValue(final PropertyValueList vlist, final BigDecimal val) {
        this.decimalValue = val;
        this.valuelist = vlist;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist,
                         final GregorianCalendar value) {
        setDate(value);
        this.valuelist = vlist;
    }

    // Constructor
    public PropertyValue(final PropertyValueList vlist,
                         final Date value) {
        this.dateValue = value;
        this.valuelist = vlist;
    }

    public PropertyValue(final PropertyValueList vlist, short shortValue) {
        this.bool = shortValue;
        this.valuelist = vlist;
    }

    public void setValueList(final PropertyValueList vlist) {
        this.valuelist = vlist;
    }

    public Element getElement() {
        return this.valuelist.getProperty().getElement();
    }

    public ElementType getElementType() {
        return this.valuelist.getProperty().getElement().getElementType();
    }

    public String getElementTypeName() {
        return this.valuelist.getProperty().getElement().getElementType().getName();
    }

    // getter and setter of String
    public String getString() {
        return svalue;
    }

    public void setString(final String value) {
        this.svalue = value;
    }

    public void setCdata(String val) {
        this.text = val;
    }

    // getter and setter of Double
    public Double getDouble() {
        return dvalue;
    }

    public void setDouble(final Double dvalue) {
        this.dvalue = dvalue;
    }

    // getter and setter of Boolean
    public Boolean getBool() {
        return this.bool == 1;
    }

    public void setBool(final Boolean bool) {
        // ToDo, PMD: AvoidUsingShortType
        this.bool = (short) (bool ? 1 : 0);
    }

    // getter and setter of Short
    public Short getShort() {
        return this.bool;
    }

    public void setShort(final Short sh) {
        // ToDo, PMD: AvoidUsingShortType
        this.bool = sh;
    }

    // getter and setter of Integer
    public Integer getInt() {
        return ivalue;
    }

    public void setInt(final Integer ivalue) {
        this.ivalue = ivalue;
    }

    // getter and setter of Long
    public Long getLong() {
        return lvalue;
    }

    public void setLong(final Long lvalue) {
        this.lvalue = lvalue;
    }

    // getter and setter of BigDecimal
    public BigDecimal getDecimal() {
        return decimalValue;
    }

    public void setDecimal(final BigDecimal bvalue) {
        this.decimalValue = bvalue;
    }

    // getter and setter of Date
    public GregorianCalendar getGregorianDate() {
        GregorianCalendar c = new GregorianCalendar();
        // 2:24 PM
        if (dateValue != null)
            c.setTime(dateValue);
        return c;
    }

    public Date getDate() {
        return dateValue;
    }

    public void setDate(final GregorianCalendar date) {
        if (date == null) {
            return;
        }
        this.dateValue = date.getTime();
    }

    public void setDate(final Date date) {
        this.dateValue = date;
    }

    public boolean isValid() {
        if (isNull()) {
            return false;
        }
        if (this.ivalue != null) {
            return true;
        }
        if (this.lvalue != null) {
            return true;
        }
        if (this.svalue != null) {
            if (this.svalue.length() > 0) {
                return true;
            }
        }
        if (this.dvalue != null) {
            return true;
        }
        if (this.bool != null) {
            return true;
        }
        if (this.decimalValue != null) {
            return true;
        }
        if (this.dateValue != null) {
            return true;
//			try {
//				final XMLGregorianCalendar date = DatatypeFactory.newInstance()
//						.newXMLGregorianCalendar();
//				if (!this.date.equals(date.toGregorianCalendar().getTime())) {
//					return true;
//				}
//			} catch (DatatypeConfigurationException e) {
//				return false; // fatal
//			}
        }
        return this.text != null;
    }

    public boolean isNull() {
        return this.ivalue == null && this.lvalue == null && this.svalue == null
                && this.dateValue == null && this.dvalue == null && this.bool == null
                && this.decimalValue == null && this.text == null;
    }

    public Object getValue() {
        if (svalue != null) {
            return svalue;
        }
        if (dvalue != null) {
            return dvalue;
        }
        if (ivalue != null) {
            return ivalue;
        }
        if (lvalue != null) {
            return lvalue;
        }
        if (dateValue != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateValue);
            return cal.getTime();
        }
        if (bool != null) {
            return Boolean.valueOf(bool != 0);
        }
        if (decimalValue != null) {
            return decimalValue;
        }
        return text;
    }

    public void setValue(final PropertyValue value) {
        if (value == null) {
            this.svalue = null;
            this.dvalue = null;
            this.ivalue = null;
            this.lvalue = null;
            this.dateValue = null;
            this.bool = null;
            this.decimalValue = null;
            this.text = null;
            return;
        }
        this.svalue = value.svalue;
        this.dvalue = value.dvalue;
        this.ivalue = value.ivalue;
        this.lvalue = value.lvalue;
        this.dateValue = value.dateValue;
        this.bool = value.bool;
        this.text = value.text;
        this.decimalValue = value.decimalValue;
    }

    public String toString() {
        final ToStringBuilder sb = new ToStringBuilder(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("id", this.id);
        if (svalue != null) {
            sb.append("String", svalue);
        } else if (dvalue != null) {
            sb.append("Double", dvalue);
        } else if (ivalue != null) {
            sb.append("Int", ivalue);
        } else if (lvalue != null) {
            sb.append("Long", lvalue);
        } else if (dateValue != null) {
            sb.append("Date", dateValue);
        } else if (bool != null) {
            sb.append("Bool", bool);
        } else if (decimalValue != null) {
            sb.append("BigDecimal", decimalValue);
        } else {
            sb.append(text);
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyValue p)) {
            return false;
        }

        if (svalue == null && p.svalue != null)
            return false;
        if (text == null && p.text != null)
            return false;
        if (dvalue == null && p.dvalue != null)
            return false;
        if (ivalue == null && p.ivalue != null)
            return false;
        if (lvalue == null && p.lvalue != null)
            return false;
        if (dateValue == null && p.dateValue != null)
            return false;
        if (bool == null && p.bool != null)
            return false;
        if (svalue != null) {
            if (p.svalue == null)
                return false;
            return p.svalue.equals(svalue);
        }
        if (text != null) {
            if (p.text == null)
                return false;
            return p.text.equals(text);
        }
        if (dvalue != null) {
            if (p.dvalue == null)
                return false;
            return p.dvalue.equals(dvalue);
        }
        if (ivalue != null) {
            if (p.ivalue == null)
                return false;
            return p.ivalue.equals(ivalue);
        }
        if (lvalue != null) {
            if (p.lvalue == null)
                return false;
            return p.lvalue.equals(lvalue);
        }
        if (dateValue != null) {
            if (p.dateValue == null)
                return false;
            return p.dateValue.equals(dateValue);
        }
        if (bool != null) {
            if (p.bool == null)
                return false;
            return p.bool.equals(bool);
        }
        if (decimalValue != null) {
            if (p.decimalValue == null)
                return false;
            return p.decimalValue.equals(decimalValue);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (svalue != null ? svalue.hashCode() : 0);                                                                        // PM
        result = prime * result + (text != null ? text.hashCode() : 0);                                                                        // PM
        result = prime * result + (dvalue != null ? dvalue.hashCode() : 0);                                                                    // PM
        result = prime * result + (ivalue != null ? ivalue.hashCode() : 0);                                                                    // PM
        result = prime * result + (lvalue != null ? lvalue.hashCode() : 0);                                                                    // PM
        result = prime * result + (dateValue != null ? dateValue.hashCode() : 0);                                                        // 2:31 PM
        result = prime * result
                + (decimalValue != null ? decimalValue.hashCode() : 0);                                                        // 2:31 PM
        result = prime * result + (bool != null ? bool.hashCode() : 0);                                                                // 2:31 PM
        return result;
    }

    public Object getObject() {
        if (this.ivalue != null) {
            return this.ivalue;
        }
        if (this.lvalue != null) {
            return this.lvalue;
        }
        if (this.svalue != null) {
            if (this.svalue.length() > 0) {
                return this.svalue;
            }
        }
        if (this.text != null) {
            if (this.text.length() > 0) {
                return this.text;
            }
        }
        if (this.dvalue != null) {
            return this.dvalue;
        }
        if (this.bool != null) {
            return this.bool;
        }
        if (this.decimalValue != null) {
            return this.decimalValue;
        }
        if (this.dateValue != null) {
            try {
                final XMLGregorianCalendar date = DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar();
                if (!this.dateValue.equals(date.toGregorianCalendar().getTime())) {
                    return date;
                }
            } catch (DatatypeConfigurationException e) {
                return null; // fatal
            }
        }

        return null;
    }

}
