INSERT INTO groups ( name ) VALUES
	( 'groupA' ),
	( 'groupB' );
INSERT INTO roles ( name ) VALUES
	( 'ENGINEER' ),
	( 'ADMIN' );
INSERT INTO owners ( username, password, enabled, totp_enabled, totp_secret ) VALUES
	( 'bob', 'bobspassword', true, false, '' ),
	( 'fred', 'fredspassword', true, false, '' );
INSERT INTO owners_roles ( owner_id, role_id ) VALUES
	( 'bob', 'ENGINEER' ),
	( 'fred', 'ENGINEER' ),
	( 'fred', 'ADMIN' );
INSERT INTO element_types ( id, name, bean_id, bean_version_id ) VALUES
	( 1, 'com.mycompany.customerrelations.Address', 'id', 'version' ),
	( 2, 'com.mycompany.customerrelations.GenderCode', 'id', 'version' ),
	( 3, 'com.mycompany.customerrelations.MaritalStatusCode', 'id', 'version' ),
	( 9, 'com.mycompany.customerrelations.Person', 'id', 'version' ),
	( 10, 'com.mycompany.customerrelations.Customer', 'id', 'version' ),
	( 11, 'TestType', 'id', 'version' ),
	( 12, 'ModelType', 'id', 'version' ),
	( 13, 'ParameterGroup', 'id', 'version' ),
	( 14, 'Parameter', 'id', 'version' );
INSERT INTO element_type_references ( element_type_id, references_id, references_KEY ) VALUES
	( 9, 2, 'gender' ),
	( 9, 3, 'maritalStatus' ),
	( 10, 1, 'addresses' ),
	( 10, 9, 'person' ),
	( 10, 10, 'friends' ),
	( 12, 13, 'modelParameters' ),
	( 12, 13, 'results' ),
	( 13, 14, 'parameters' );
INSERT INTO elements ( id, version, element_type_id ) VALUES
	( 2, 0, 1 ),
	( 3, 0, 9 ),
	( 5, 0, 2 ),
	( 6, 0, 2 ),
	( 8, 0, 1 );
INSERT INTO elements ( id, version, element_type_id, owner ) VALUES
	( 1, 0, 10, 'bob' ),
	( 4, 0, 10, 'fred' ),
	( 7, 0, 10, 'fred' );
INSERT INTO modifications ( id, element_id, revision, next_revision, timestamp, modcomment ) VALUES
	( 1, 1, 1, 9999999, '2010-05-03', 'created' ),
	( 2, 2, 1, 9999999, '2010-05-03', 'created' ),
	( 3, 3, 1, 9999999, '2010-05-03', 'created' ),
	( 4, 4, 1, 9999999, '2010-05-03', 'created' ),
	( 5, 5, 1, 9999999, '2010-05-03', 'created' ),
	( 6, 6, 1, 9999999, '2010-05-03', 'created' ),
	( 7, 7, 1, 9999999, '2010-05-03', 'created' ),
	( 8, 8, 1, 9999999, '2010-05-03', 'created' );
INSERT INTO elementrefs ( id, revision, next_revision, refname, element_type_id, parent_id ) VALUES
	( 1, 1, 9999999, 'addresses', 1, 1 ),
	( 5, 1, 9999999, 'addresses', 1, 4 ),
	( 2, 1, 9999999, 'person', 9, 1 ),
	( 3, 1, 9999999, 'person', 9, 4 ),
	( 4, 1, 9999999, 'friends', 10, 1 );
INSERT INTO elementrefs_lists ( id, revision, next_revision, elementrefs_id ) VALUES
	( 1, 1, 9999999, 1 ),
	( 5, 1, 9999999, 5 ),
	( 2, 1, 9999999, 2 ),
	( 3, 1, 9999999, 3 ),
	( 4, 1, 9999999, 4 );
INSERT INTO elementrefs_elements ( ref_id, element_id ) VALUES
	( 1, 2 ),
	( 5, 8 ),
	( 2, 3 ),
	( 3, 3 ),
	( 4, 7 );
INSERT INTO property_types ( id, name, type, unit ) VALUES
	( 1, 'postalCode', 'STRING', '' ),
	( 2, 'line1', 'STRING', '' ),
	( 3, 'country', 'STRING', '' ),
	( 4, 'line2', 'STRING', '' ),
	( 5, 'city', 'STRING', '' ),
	( 9, 'premium', 'BOOL', '' ),
	( 28, 'lastname', 'STRING', '' ),
	( 29, 'firstname', 'STRING', '' ),
	( 30, 'customerNumber', 'INTEGER', '' ),
	( 31, 'name', 'STRING', '' ),
	( 33, 'value', 'STRING', '' ),
	( 35, 'birthday', 'DATE', '' ),
	( 36, 'text1', 'STRING', '' ),
	( 37, 'text2', 'STRING', '' ),
	( 38, 'text3', 'STRING', '' ),
	( 39, 'revenue', 'REAL', '' ),
	( 40, 'credits', 'REAL', '' ),
	( 41, 'cdataval', 'CDATA', '' ),
	( 43, 'pgname', 'STRING', '' ),
	( 44, 'pname', 'STRING', '' ),
	( 45, 'ival', 'INTEGER', '' ),
	( 46, 'weight', 'REAL', 'kg' );
INSERT INTO element_types_property_types ( element_type_id, property_types_id ) VALUES
	( 1, 1 ),
	( 1, 2 ),
	( 1, 3 ),
	( 1, 4 ),
	( 1, 5 ),
	( 1, 31 ),
	( 2, 33 ),
	( 9, 28 ),
	( 9, 29 ),
	( 9, 31 ),
	( 9, 35 ),
	( 9, 36 ),
	( 9, 37 ),
	( 9, 38 ),
	( 9, 46 ),
	( 10, 30 ),
	( 10, 31 ),
	( 10, 9 ),
	( 10, 40 ),
	( 10, 39 ),
	( 1, 31 ),
	( 3, 33 ),
	( 11, 41 ),
	( 12, 31 ),
	( 13, 43 ),
	( 14, 44 ),
	( 12, 45 );
INSERT INTO properties ( id, element_id, property_type_id, revision, next_revision ) VALUES
	( 1, 1, 30, 1, 9999999 ),
	( 2, 2, 1, 1, 9999999 ),
	( 3, 2, 2, 1, 9999999 ),
	( 4, 2, 3, 1, 9999999 ),
	( 5, 2, 4, 1, 9999999 ),
	( 6, 2, 5, 1, 9999999 ),
	( 7, 3, 28, 1, 9999999 ),
	( 8, 3, 29, 1, 9999999 ),
	( 9, 1, 31, 1, 9999999 ),
	( 10, 2, 31, 1, 9999999 ),
	( 11, 3, 31, 1, 9999999 ),
	( 14, 5, 33, 1, 9999999 ),
	( 15, 6, 33, 1, 9999999 ),
	( 19, 7, 30, 1, 9999999 ),
	( 20, 1, 40, 1, 9999999 ),
	( 21, 8, 3, 1, 9999999 ),
	( 22, 8, 5, 1, 9999999 ),
	( 23, 4, 30, 1, 9999999 ),
	( 24, 1, 9, 1, 9999999 );
INSERT INTO property_value_list ( id, property_id, revision, next_revision ) VALUES
	( 1, 1, 1, 9999999 ),
	( 2, 2, 1, 9999999 ),
	( 3, 3, 1, 9999999 ),
	( 4, 4, 1, 9999999 ),
	( 5, 5, 1, 9999999 ),
	( 6, 6, 1, 9999999 ),
	( 7, 7, 1, 9999999 ),
	( 8, 8, 1, 9999999 ),
	( 9, 9, 1, 9999999 ),
	( 10, 10, 1, 9999999 ),
	( 11, 11, 1, 9999999 ),
	( 14, 14, 1, 9999999 ),
	( 15, 15, 1, 9999999 ),
	( 19, 19, 1, 9999999 ),
	( 20, 20, 1, 9999999 ),
	( 21, 21, 1, 9999999 ),
	( 22, 22, 1, 9999999 ),
	( 23, 23, 1, 9999999 ),
	( 24, 24, 1, 9999999 );
INSERT INTO property_values (id, valuelist_id, svalue) VALUES
	( 9, 9, 'cust1' ),
	( 7, 7, 'lastName1' ),
	( 8, 8, 'firstName1' ),
	( 11, 11, 'person' ),
	( 2, 2, 'zip1' ),
	( 10, 10, 'address' ),
	( 3, 3, 'line1' ),
	( 4, 4, 'Country1' ),
	( 6, 6, 'City1' ),
	( 21, 21, 'Country2' ),
	( 22, 22, 'City2' ),
	( 14, 14, 'female' ),
	( 15, 15, 'male' );
INSERT INTO property_values (id, valuelist_id, ivalue) VALUES
	( 101, 1, 123 ),
	( 102, 23, 987 ),
	( 19, 19, 2222 );
INSERT INTO property_values (id, valuelist_id, dvalue) VALUES
	( 20, 20, 1 );
INSERT INTO property_values (id, valuelist_id, bool) VALUES
	( 105, 24, 0 );
INSERT INTO property_values (id, valuelist_id) VALUES
	( 5, 5 );

ALTER SEQUENCE "ELEMENT_SEQ" RESTART with 1000 INCREMENT BY 50;
ALTER SEQUENCE "ELEM_TYPE_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "MOD_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "PROP_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "PROP_LIST_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "PROP_VAL_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "PROP_TYPE_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "ELEM_REF_SEQ" RESTART WITH 1000 INCREMENT BY 50;
ALTER SEQUENCE "ELEM_REF_LIST_SEQ" RESTART WITH 1000 INCREMENT BY 50;
