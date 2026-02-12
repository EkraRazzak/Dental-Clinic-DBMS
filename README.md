# Dental-Clinic-DBMS
Assignment 9

Connects to Oracle Database using ojdbc17.jar.


This program is a Java and Oracle JDBC dental clinic management app that creates the required tables, inserts sample data, and provides a simple Swing GUI for viewing/searching patients and managing appointments. 

To run compile both .java files with ojdbc17.jar in the classpath and execute DentalClinicGUI.

javac -cp ".;ojdbc17.jar" DentalClinicApp.java DentalClinicGUI.java
java -cp ".;ojdbc17.jar" DentalClinicGUI

Click Create Tables button to create tables
Click Populate to populate tables with data
Click Query/Manage to manage queries, it will open a popup where you can input a number or letter to choose menu options
Click Drop Tables to drop tables
Click Exit to exit the program
