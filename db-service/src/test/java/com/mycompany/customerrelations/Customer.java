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
import java.math.BigDecimal;

import java.util.List;
import com.mycompany.customerrelations.Person;

/**
 * <p>Java class for Customer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Customer">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="customerNumber" type="{http://www.w3.org/2001/XMLSchema}ID"/>
 *         &lt;element name="person" type="{http://www.mycompany.com/customerrelations}Person"/>
 *         &lt;element name="address" type="{http://www.mycompany.com/customerrelations}Address"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
public class Customer implements Serializable{
    private Long id;
    private int version;
    private BigDecimal revenue;
    protected Integer customerNumber;
    protected boolean premium;
    protected Person person;
    protected List<Address> addresses;
    protected List<Customer> friends;
    protected List<Double> credits;
    
    /**
     * Gets the value of the credits property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public List<Double> getCredits() {
      return credits;
    }

    /**
     * Gets the value of the customerNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Integer getCustomerNumber() {
        return customerNumber;
    }

    /**
     * Sets the value of the customerNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCustomerNumber(Integer value) {
        this.customerNumber = value;
    }

    /**
     * Gets the value of the person property.
     * 
     * @return
     *     possible object is
     *     {@link Person }
     *     
     */
    public Person getPerson() {
        return person;
    }

    /**
     * Sets the value of the person property.
     * 
     * @param value
     *     allowed object is
     *     {@link Person }
     *     
     */
    public void setPerson(Person value) {
        this.person = value;
    }

    /**
     * Gets the value of the address property.
     * 
     * @return
     *     possible object is
     *     {@link Address }
     *     
     */
    public List<Address> getAddresses() {
        return addresses;
    }

    /**
     * Sets the value of the address property.
     * 
     * @param value
     *     allowed object is
     *     {@link Address }
     *     
     */
    public void setAddresses(List<Address> value) {
        this.addresses = value;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public int getVersion() {
      return version;
    }

    public void setVersion(int version) {
      this.version = version;
    }

    public void setPremium( boolean prem ){
      this.premium=prem;
    }
    public boolean isPremium( ){
      return this.premium;
    }

		public BigDecimal getRevenue() {
			return revenue;
		}

		public void setRevenue(BigDecimal revenue) {
			this.revenue = revenue;
		}

		public void setCredits(List<Double> credits) {
			this.credits = credits;
		}

    /**
     * Gets the value of the friends property.
     * 
     * @return
     *     possible object is
     *     {@link Customer }
     *     
     */
    public List<Customer> getFriends() {
        return friends;
    }

    /**
     * Sets the value of the customer property.
     * 
     * @param value
     *     allowed object is
     *     {@link Customer }
     *     
     */
    public void setFriends(List<Customer> value) {
        this.friends = value;
    }

}
