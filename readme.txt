Pre-requisites and steps to deploy:
1. Get the Google Maps account created and the API_KEY to make Direction REST API calls.
2. Get the Open Weather account created and the APP_KEY to make Current Weather REST API calls.
3. Modify the application.properties file for the above key values at respective placeholders
4. Java jdk1.8 or higher
5. Database: Mysql installation, version 8.0
6. Modify the application.properties with the mySQL config details replacing the placeholders-
(Create a custom user and bind using this new user as "root" is not recommended, set the "spring.jpa.hibernate.ddl-auto" property within application.properties to "create" during the initial deployment and revert back to "none" to restrict further database changes)
7. This is a maven project, have maven installed to create the project .war
8. From project home directory (/home/Phase2/)Run command :  mvn package, this will create Phase2.war file at /home/Phase/target/
9. Run this war file from project home executing: spring-boot: run
10. Access Phase2 at localhost:8090/
11. This opens up the index.html and start requesting