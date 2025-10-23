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

package com.mycompany.customerrelations;

import java.io.Serializable;


import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Person complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Person">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="firstname" type="{http://www.mycompany.com/customerrelations}Name"/>
 *         &lt;element name="lastname" type="{http://www.mycompany.com/customerrelations}Name"/>
 *         &lt;element name="gender" type="{http://www.mycompany.com/customerrelations}Gender"/>
 *         &lt;element name="birthday" type="{http://www.w3.org/2001/XMLSchema}date" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */

public class Person implements Serializable {
    private Long id;
    private int version;
    protected String firstname;
    protected String lastname;
    protected String text1;
    protected String text2;
    protected String text3;
    
    protected GenderCode gender;
    protected MaritalStatusCode maritalStatus;
    protected XMLGregorianCalendar birthday;

    public Long getId() {
      return id;
    }

    public MaritalStatusCode getMaritalStatus() {
      return maritalStatus;
    }

    public void setMaritalStatus(MaritalStatusCode maritalStatus) {
      this.maritalStatus = maritalStatus;
    }

    public void setId(Long id) {
      this.id = id;
    }

   
    /**
     * Gets the value of the firstname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * Sets the value of the firstname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFirstname(String value) {
        this.firstname = value;
    }

    /**
     * Gets the value of the lastname property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * Sets the value of the lastname property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLastname(String value) {
        this.lastname = value;
    }

    public String getText1() {
      return text1;
    }
    public void setText1(String value) {
      this.text1 = value;
    }
    
    public String getText2() {
      return text2;
    }
    public void setText2(String value) {
      this.text2 = value;
    }
    
    public String getText3() {
      return text3;
    }
    public void setText3(String value) {
      this.text3 = value;
    }

    /**
     * Gets the value of the gender property.
     * 
     * @return
     *     possible object is
     *     {@link GenderCode }
     *     
     */
    public GenderCode getGender() {
        return gender;
    }

    /**
     * Sets the value of the gender property.
     * 
     * @param value
     *     allowed object is
     *     {@link GenderCode }
     *     
     */
    public void setGender(GenderCode value) {
        this.gender = value;
    }

    /**
     * Gets the value of the birthday property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getBirthday() {
        return birthday;
    }

    /**
     * Sets the value of the birthday property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setBirthday(XMLGregorianCalendar value) {
        this.birthday = value;
    }
    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }

}
