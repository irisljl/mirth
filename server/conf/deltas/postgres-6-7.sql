DROP TABLE CONFIGURATION;

CREATE TABLE CONFIGURATION
	(CATEGORY VARCHAR(255) NOT NULL,
	NAME VARCHAR(255) NOT NULL,
	VALUE TEXT NOT NULL);
	
DROP TABLE PREFERENCES;

ALTER TABLE SCRIPT ADD COLUMN GROUP_ID VARCHAR(255) NOT NULL;

UPDATE SCRIPT SET GROUP_ID = 'GLOBAL' WHERE ID = 'Deploy' OR ID = 'Shutdown' OR ID = 'Preprocessor' OR ID = 'Postprocessor';

DELETE FROM SCRIPT WHERE GROUP_ID IS NULL OR GROUP_ID = '';

ALTER TABLE SCRIPT DROP CONSTRAINT 'SCRIPT_PKEY';

ALTER TABLE SCRIPT ADD PRIMARY KEY (GROUP_ID, ID);

DELETE FROM TEMPLATE;

ALTER TABLE TEMPLATE ADD COLUMN GROUP_ID VARCHAR(255) NOT NULL;

ALTER TABLE TEMPLATE DROP CONSTRAINT 'TEMPLATE_PKEY';

ALTER TABLE TEMPLATE ADD PRIMARY KEY (GROUP_ID, ID);