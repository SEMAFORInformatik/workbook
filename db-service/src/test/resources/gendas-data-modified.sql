INSERT INTO groups ( name ) VALUES
	( 'groupA' ),
	( 'groupB' );
INSERT INTO roles ( name ) VALUES
	( 'ADMIN' ),
	( 'ENGINEER' );
INSERT INTO owners ( username, enabled, password, totp_enabled, totp_secret ) VALUES
	( 'bob', true, 'bobspassword', false, '' ),
	( 'fred', true, 'fredspassword', false, '' );
INSERT INTO owners_roles ( owner_id, role_id ) VALUES
	( 'fred', 'ADMIN' ),
	( 'bob', 'ENGINEER' ),
	( 'fred', 'ENGINEER' );
INSERT INTO element_types ( id, bean_id, bean_version_id, name ) VALUES
	( 1, 'id', 'version', 'com.mycompany.customerrelations.Address' ),
	( 2, 'id', 'version', 'com.mycompany.customerrelations.GenderCode' ),
	( 3, 'id', 'version', 'com.mycompany.customerrelations.MaritalStatusCode' ),
	( 9, 'id', 'version', 'com.mycompany.customerrelations.Person' ),
	( 10, 'id', 'version', 'com.mycompany.customerrelations.Customer' ),
	( 11, 'id', 'version', 'TestType' ),
	( 12, 'id', 'version', 'ModelType' ),
	( 13, 'id', 'version', 'ParameterGroup' ),
	( 14, 'id', 'version', 'Parameter' );
INSERT INTO element_type_references ( element_type_id, references_id, references_KEY ) VALUES
	( 10, 1, 'addresses' ),
	( 9, 2, 'gender' ),
	( 9, 3, 'maritalStatus' ),
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
  ( 1, 1, 10, 'bob' ),
	( 7, 0, 10, 'fred' ),
	( 4, 0, 10, 'fred' );
INSERT INTO modifications ( id, modcomment, timestamp, next_revision, revision, element_id ) VALUES
	( 1, 'created', '2010-05-03 00:00:00.0', 2, 1, 1 ),
	( 2, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 2 ),
	( 3, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 3 ),
	( 4, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 4 ),
	( 5, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 5 ),
	( 6, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 6 ),
	( 7, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 7 ),
	( 8, 'created', '2010-05-03 00:00:00.0', 9999999, 1, 8 ),
	( 9, 'remove person', '2015-04-09 10:13:36.0', 9999999, 2, 1 );
INSERT INTO elementrefs ( id, next_revision, refname, revision, element_type_id, parent_id ) VALUES
	( 1, 9999999, 'addresses', 1, 1, 1 ),
	( 2, 9999999, 'person', 1, 9, 1 ),
	( 3, 9999999, 'person', 1, 9, 4 ),
	( 4, 9999999, 'friends', 1, 10, 1 ),
	( 5, 9999999, 'addresses', 1, 1, 4 );
INSERT INTO elementrefs_lists ( id, next_revision, revision, elementrefs_id ) VALUES
	( 1, 2, 1, 1 ),
	( 2, 2, 1, 2 ),
	( 3, 9999999, 1, 3 ),
	( 4, 2, 1, 4 ),
	( 5, 9999999, 1, 5 ),
	( 6, 9999999, 2, 1 ),
	( 7, 9999999, 2, 2 ),
	( 8, 9999999, 2, 4 );
INSERT INTO elementrefs_elements ( ref_id, element_id ) VALUES
	( 1, 2 ),
	( 5, 8 ),
	( 2, 3 ),
	( 3, 3 ),
	( 4, 7 ),
	( 6, 2 ),
	( 8, 7 );
INSERT INTO property_types ( id, name, type ) VALUES
	( 1, 'postalCode', 'STRING' ),
	( 2, 'line1', 'STRING' ),
	( 3, 'country', 'STRING' ),
	( 4, 'line2', 'STRING' ),
	( 5, 'city', 'STRING' ),
	( 9, 'premium', 'BOOL' ),
	( 28, 'lastname', 'STRING' ),
	( 29, 'firstname', 'STRING' ),
	( 30, 'customerNumber', 'INTEGER' ),
	( 31, 'name', 'STRING' ),
	( 33, 'value', 'STRING' ),
	( 35, 'birthday', 'DATE' ),
	( 36, 'text1', 'STRING' ),
	( 37, 'text2', 'STRING' ),
	( 38, 'text3', 'STRING' ),
	( 39, 'revenue', 'REAL' ),
	( 40, 'credits', 'REAL' ),
	( 41, 'cdataval', 'CDATA' ),
	( 43, 'pgname', 'STRING' ),
	( 44, 'pname', 'STRING' ),
	( 45, 'ival', 'INTEGER' );
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
INSERT INTO properties ( id, next_revision, revision, element_id, property_type_id ) VALUES
	( 1, 9999999, 1, 1, 30 ),
	( 2, 9999999, 1, 2, 1 ),
	( 3, 9999999, 1, 2, 2 ),
	( 4, 9999999, 1, 2, 3 ),
	( 5, 9999999, 1, 2, 4 ),
	( 6, 9999999, 1, 2, 5 ),
	( 7, 9999999, 1, 3, 28 ),
	( 8, 9999999, 1, 3, 29 ),
	( 9, 2, 1, 1, 31 ),
	( 10, 9999999, 1, 2, 31 ),
	( 11, 9999999, 1, 3, 31 ),
	( 14, 9999999, 1, 5, 33 ),
	( 15, 9999999, 1, 6, 33 ),
	( 19, 9999999, 1, 7, 30 ),
	( 20, 9999999, 1, 1, 40 ),
	( 21, 9999999, 1, 8, 3 ),
	( 22, 9999999, 1, 8, 5 ),
	( 23, 9999999, 1, 4, 30 ),
	( 24, 9999999, 1, 1, 9 );
INSERT INTO property_value_list ( id, next_revision, revision, property_id ) VALUES
	( 1, 9999999, 1, 1 ),
	( 2, 9999999, 1, 2 ),
	( 3, 9999999, 1, 3 ),
	( 4, 9999999, 1, 4 ),
	( 5, 9999999, 1, 5 ),
	( 6, 9999999, 1, 6 ),
	( 7, 9999999, 1, 7 ),
	( 8, 9999999, 1, 8 ),
	( 9, 2, 1, 9 ),
	( 10, 9999999, 1, 10 ),
	( 11, 9999999, 1, 11 ),
	( 14, 9999999, 1, 14 ),
	( 15, 9999999, 1, 15 ),
	( 19, 9999999, 1, 19 ),
	( 20, 9999999, 1, 20 ),
	( 21, 9999999, 1, 21 ),
	( 22, 9999999, 1, 22 ),
	( 23, 9999999, 1, 23 ),
	( 24, 9999999, 1, 24 );
INSERT INTO property_values ( id, svalue, valuelist_id ) VALUES
	( 2, 'zip1', 2 ),
	( 3, 'line1', 3 ),
	( 4, 'Country1', 4 ),
	( 6, 'City1', 6 ),
	( 7, 'lastName1', 7 ),
	( 8, 'firstName1', 8 ),
	( 9, 'cust1', 9 ),
	( 10, 'address', 10 ),
	( 11, 'person', 11 ),
	( 14, 'female', 14 ),
	( 15, 'male', 15 ),
	( 21, 'Country2', 21 ),
	( 22, 'City2', 22 );

INSERT INTO property_values ( id, ivalue, valuelist_id ) VALUES
	( 19, 2222, 19 ),
    ( 101, 123, 1 ),
	( 102, 987, 23 );

INSERT INTO property_values ( id, dvalue, valuelist_id ) VALUES
	( 20, 1.0, 20 );

INSERT INTO property_values ( id, bool, valuelist_id ) VALUES
    ( 105, 0, 24 );

INSERT INTO property_values ( id, valuelist_id ) VALUES
    ( 5, 5 );
