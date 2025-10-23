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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ch.semafor.gendas.exceptions.CoreException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyTest {
	private static final Logger logger = LoggerFactory
			.getLogger(PropertyTest.class);

	@Test
	public void testEquals() throws CoreException {
		final ElementType tpers = new ElementType("person");
		final PropertyType firstNameType = new PropertyType("firstname");
		final PropertyType lastNameType = new PropertyType("lastname");

		final Element person1 = new Element(tpers);
		final Element person2 = new Element(tpers);

		final Property givenName1 = new Property(person1, firstNameType);
		final Property givenName2 = new Property(person2, firstNameType);
		final Property familyName = new Property(person2, lastNameType);
		givenName1.setValue(0, "value1");
		familyName.setValue(0, "value1");

		assertNotEquals(familyName, givenName1);
		assertNotEquals(givenName2, givenName1);
		if (logger.isDebugEnabled()) {
			givenName1.print(0);
			givenName2.print(0);
		}
		givenName2.assign(givenName1);
		if (logger.isDebugEnabled()) {
			givenName1.print(0);
			givenName2.print(0);
		}
		assertEquals(givenName2, givenName1);
	}
}
