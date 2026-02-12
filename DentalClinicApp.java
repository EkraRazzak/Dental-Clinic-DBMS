import java.sql.*;
import java.util.Scanner;

/**
 * CPS510 - Assignment 9 
 */

public class DentalClinicApp {

    // db connection settings
    public static final String DB_URL  = "jdbc:oracle:thin:@//localhost:1522/XEPDB1";
    public static final String DB_USER = "z1haque";
    public static final String DB_PASS = "01164930";


    private static final String[] DROP_SQL = {
        "DROP TABLE order_items CASCADE CONSTRAINTS",
        "DROP TABLE treatment_supply CASCADE CONSTRAINTS",
        "DROP TABLE supply_substitute CASCADE CONSTRAINTS",
        "DROP TABLE billing CASCADE CONSTRAINTS",
        "DROP TABLE treatment CASCADE CONSTRAINTS",
        "DROP TABLE appointment CASCADE CONSTRAINTS",
        "DROP TABLE orders CASCADE CONSTRAINTS",
        "DROP TABLE dentist_day_off CASCADE CONSTRAINTS",
        "DROP TABLE staff_day_off CASCADE CONSTRAINTS",
        "DROP TABLE medical_history CASCADE CONSTRAINTS",
        "DROP TABLE supply CASCADE CONSTRAINTS",
        "DROP TABLE supplier CASCADE CONSTRAINTS",
        "DROP TABLE staff CASCADE CONSTRAINTS",
        "DROP TABLE dentist CASCADE CONSTRAINTS",
        "DROP TABLE patient CASCADE CONSTRAINTS"
    };

    private static final String[] CREATE_SQL = {
        "CREATE TABLE patient (" +
        "  patient_id NUMBER PRIMARY KEY," +
        "  first_name VARCHAR2(50) NOT NULL," +
        "  last_name  VARCHAR2(50) NOT NULL," +
        "  date_of_birth DATE NOT NULL," +
        "  insurance_provider VARCHAR2(100)," +
        "  phone_number VARCHAR2(20)," +
        "  street VARCHAR2(100)," +
        "  city   VARCHAR2(60)," +
        "  postal_code VARCHAR2(10)" +
        ")",

        "CREATE TABLE dentist (" +
        "  dentist_id NUMBER PRIMARY KEY," +
        "  name VARCHAR2(100) NOT NULL," +
        "  specialization VARCHAR2(100)," +
        "  phone_number VARCHAR2(20)" +
        ")",

        "CREATE TABLE staff (" +
        "  staff_id NUMBER PRIMARY KEY," +
        "  name VARCHAR2(100) NOT NULL," +
        "  role VARCHAR2(50) NOT NULL," +
        "  phone_number VARCHAR2(20)" +
        ")",

        "CREATE TABLE supplier (" +
        "  supplier_id NUMBER PRIMARY KEY," +
        "  name VARCHAR2(100) NOT NULL UNIQUE," +
        "  contact VARCHAR2(100)," +
        "  phone_number VARCHAR2(20)" +
        ")",

        "CREATE TABLE supply (" +
        "  supply_id NUMBER PRIMARY KEY," +
        "  name VARCHAR2(100) NOT NULL UNIQUE," +
        "  quantity NUMBER(10) DEFAULT 0 NOT NULL CHECK (quantity >= 0)" +
        ")",

        "CREATE TABLE dentist_day_off (" +
        "  dentist_id NUMBER NOT NULL," +
        "  day_of_week VARCHAR2(9) NOT NULL CHECK (day_of_week IN " +
        "    ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))," +
        "  CONSTRAINT pk_dentist_day_off PRIMARY KEY (dentist_id, day_of_week)," +
        "  CONSTRAINT fk_dentist_day_off FOREIGN KEY (dentist_id) REFERENCES dentist(dentist_id)" +
        ")",

        "CREATE TABLE staff_day_off (" +
        "  staff_id NUMBER NOT NULL," +
        "  day_of_week VARCHAR2(9) NOT NULL CHECK (day_of_week IN " +
        "    ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))," +
        "  CONSTRAINT pk_staff_day_off PRIMARY KEY (staff_id, day_of_week)," +
        "  CONSTRAINT fk_staff_day_off FOREIGN KEY (staff_id) REFERENCES staff(staff_id)" +
        ")",

        "CREATE TABLE appointment (" +
        "  appt_id NUMBER PRIMARY KEY," +
        "  patient_id NUMBER NOT NULL," +
        "  dentist_id NUMBER NOT NULL," +
        "  appt_date DATE NOT NULL," +
        "  start_time VARCHAR2(5) NOT NULL CHECK (REGEXP_LIKE(start_time,'^[0-2][0-9]:[0-5][0-9]$'))," +
        "  status VARCHAR2(20) DEFAULT 'SCHEDULED' NOT NULL CHECK " +
        "    (status IN ('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW'))," +
        "  CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patient(patient_id)," +
        "  CONSTRAINT fk_appt_dentist FOREIGN KEY (dentist_id) REFERENCES dentist(dentist_id)," +
        "  CONSTRAINT uq_dentist_slot UNIQUE (dentist_id, appt_date, start_time)" +
        ")",

        "CREATE TABLE treatment (" +
        "  treatment_id NUMBER PRIMARY KEY," +
        "  appt_id NUMBER NOT NULL," +
        "  staff_id NUMBER," +
        "  description VARCHAR2(200) NOT NULL," +
        "  cost NUMBER(8,2) DEFAULT 0 NOT NULL CHECK (cost >= 0)," +
        "  CONSTRAINT fk_trt_appt FOREIGN KEY (appt_id) REFERENCES appointment(appt_id)," +
        "  CONSTRAINT fk_trt_staff FOREIGN KEY (staff_id) REFERENCES staff(staff_id)" +
        ")",

        "CREATE TABLE billing (" +
        "  billing_id NUMBER PRIMARY KEY," +
        "  treatment_id NUMBER NOT NULL UNIQUE," +
        "  amount NUMBER(8,2) NOT NULL CHECK (amount >= 0)," +
        "  payment_status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL CHECK " +
        "    (payment_status IN ('PENDING','PAID','PARTIAL','CANCELLED'))," +
        "  created_on DATE DEFAULT SYSDATE NOT NULL," +
        "  CONSTRAINT fk_bill_treatment FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id)" +
        ")",

        "CREATE TABLE orders (" +
        "  order_id NUMBER PRIMARY KEY," +
        "  staff_id NUMBER NOT NULL," +
        "  supplier_id NUMBER NOT NULL," +
        "  order_date DATE DEFAULT SYSDATE NOT NULL," +
        "  status VARCHAR2(20) DEFAULT 'PLACED' NOT NULL CHECK " +
        "    (status IN ('PLACED','RECEIVED','CANCELLED'))," +
        "  CONSTRAINT fk_ord_staff FOREIGN KEY (staff_id) REFERENCES staff(staff_id)," +
        "  CONSTRAINT fk_ord_supplier FOREIGN KEY (supplier_id) REFERENCES supplier(supplier_id)" +
        ")",

        "CREATE TABLE order_items (" +
        "  order_id NUMBER NOT NULL," +
        "  supply_id NUMBER NOT NULL," +
        "  quantity NUMBER(10) NOT NULL CHECK (quantity > 0)," +
        "  CONSTRAINT pk_order_items PRIMARY KEY (order_id, supply_id)," +
        "  CONSTRAINT fk_oi_order FOREIGN KEY (order_id) REFERENCES orders(order_id)," +
        "  CONSTRAINT fk_oi_supply FOREIGN KEY (supply_id) REFERENCES supply(supply_id)" +
        ")",

        "CREATE TABLE treatment_supply (" +
        "  treatment_id NUMBER NOT NULL," +
        "  supply_id NUMBER NOT NULL," +
        "  qty_used NUMBER(10) DEFAULT 1 NOT NULL CHECK (qty_used > 0)," +
        "  CONSTRAINT pk_trt_supply PRIMARY KEY (treatment_id, supply_id)," +
        "  CONSTRAINT fk_ts_trt FOREIGN KEY (treatment_id) REFERENCES treatment(treatment_id)," +
        "  CONSTRAINT fk_ts_sup FOREIGN KEY (supply_id) REFERENCES supply(supply_id)" +
        ")",

        "CREATE TABLE supply_substitute (" +
        "  supply_id NUMBER NOT NULL," +
        "  substitute_id NUMBER NOT NULL," +
        "  CONSTRAINT pk_supply_sub PRIMARY KEY (supply_id, substitute_id)," +
        "  CONSTRAINT fk_sub_sup1 FOREIGN KEY (supply_id) REFERENCES supply(supply_id)," +
        "  CONSTRAINT fk_sub_sup2 FOREIGN KEY (substitute_id) REFERENCES supply(supply_id)," +
        "  CONSTRAINT chk_no_self_sub CHECK (supply_id <> substitute_id)" +
        ")",

        "CREATE TABLE medical_history (" +
        "  history_id NUMBER PRIMARY KEY," +
        "  patient_id NUMBER NOT NULL," +
        "  condition VARCHAR2(100) NOT NULL," +
        "  notes VARCHAR2(4000)," +
        "  recorded_on DATE DEFAULT SYSDATE NOT NULL," +
        "  CONSTRAINT fk_hist_patient FOREIGN KEY (patient_id) REFERENCES patient(patient_id)" +
        ")"
    };

    private static final String[] INSERT_SQL = {
        "INSERT INTO patient VALUES (1,'Dorian','Saraci', DATE '2002-01-05', NULL, '416-555-0101','123 King St','Toronto','M5B2K3')",
        "INSERT INTO patient VALUES (2,'Joe','Fazer',DATE '2003-06-17','SunLife','416-555-0102','77 Queen St','Toronto','M5V1A1')",

        "INSERT INTO dentist (dentist_id, name, specialization, phone_number) VALUES (10,'Dr. Eppley','ORTHODONTICS','416-555-0201')",
        "INSERT INTO dentist (dentist_id, name, specialization, phone_number) VALUES (11,'Dr. Taban','SURGEON','416-555-0202')",

        "INSERT INTO staff (staff_id, name, role, phone_number) VALUES (20,'Ekra Razzak','ASSISTANT','416-555-0301')",
        "INSERT INTO staff (staff_id, name, role, phone_number) VALUES (21,'Zaid Haque','HYGIENIST','416-555-0302')",

        "INSERT INTO supplier VALUES (30,'Sinclair Dental.','Chris Heria','416-555-0400')",
        "INSERT INTO supplier VALUES (31,'Benco Dental','Austin Dunham','416-555-0401')",

        "INSERT INTO supply VALUES (40,'Sterilazation Pouches',12)",
        "INSERT INTO supply VALUES (41,'Face masks',100)",
        "INSERT INTO supply VALUES (42,'Nitrile Gloves',200)",

        "INSERT INTO dentist_day_off VALUES (10,'MONDAY')",
        "INSERT INTO dentist_day_off VALUES (11,'FRIDAY')",

        "INSERT INTO staff_day_off VALUES (21,'WEDNESDAY')",

        "INSERT INTO appointment VALUES (50,1,10, DATE '2025-10-02','09:00','SCHEDULED')",
        "INSERT INTO appointment VALUES (51,2,10, DATE '2025-10-02','10:00','SCHEDULED')",
        "INSERT INTO appointment VALUES (52,2,11, DATE '2025-10-03','14:30','SCHEDULED')",

        "INSERT INTO treatment VALUES (60,50,21,'Cleaning',120)",
        "INSERT INTO treatment VALUES (61,51,21,'Filling',200)",

        "INSERT INTO billing VALUES (70,60,120,'PAID',SYSDATE)",
        "INSERT INTO billing VALUES (71,61,200,'PENDING',SYSDATE)",

        "INSERT INTO orders VALUES (80,20,31,SYSDATE-1,'PLACED')",
        "INSERT INTO orders VALUES (81,20,30,SYSDATE-5,'RECEIVED')",

        "INSERT INTO order_items VALUES (80,41,20)",
        "INSERT INTO order_items VALUES (81,40,5)",

        "INSERT INTO treatment_supply VALUES (60,42,2)",
        "INSERT INTO treatment_supply VALUES (61,40,1)",

        "INSERT INTO supply_substitute VALUES (41,40)",

        "INSERT INTO medical_history VALUES (90,1,'Asthma','Inhaler as needed',SYSDATE-400)",
        "INSERT INTO medical_history VALUES (91,2,'Peanut Allergies','EpiPen on hand',SYSDATE-400)"
    };

    // main console
    public static void main(String[] args) {

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (Exception e) {
            System.err.println("Could not load Oracle driver.");
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to Oracle as " + DB_USER);
            boolean running = true;

            while (running) {
                printMainMenu();
                String choice = sc.nextLine().trim().toUpperCase();

                switch (choice) {
                    case "1": dropTables(conn); break;
                    case "2": createTables(conn); break;
                    case "3": populateTables(conn); break;
                    case "4": queryMenu(conn, sc); break;
                    case "E": running = false; break;
                    default: System.out.println("Invalid option.");
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL error: " + e.getMessage());
        }
    }

    private static void printMainMenu() {
        System.out.println("\n============ Dental Clinic Menu ============");
        System.out.println("1) Drop Tables");
        System.out.println("2) Create Tables");
        System.out.println("3) Populate Tables");
        System.out.println("4) Query / Manage Data");
        System.out.println("E) Exit");
        System.out.print("Choice: ");
    }

    // public methods for gui
    public static void dropTables(Connection conn) {
        System.out.println("Dropping tables...");
        for (String sql : DROP_SQL) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println("Skip: " + e.getMessage());
            }
        }
        System.out.println("Drop done.");
    }

    public static void createTables(Connection conn) {
        System.out.println("Creating tables...");
        for (String sql : CREATE_SQL) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Create done.");
    }

    public static void populateTables(Connection conn) {
        System.out.println("Populating...");
        for (String sql : INSERT_SQL) {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            } catch (SQLException e) {
                System.out.println("Insert failed: " + e.getMessage());
            }
        }
        System.out.println("Populate done.");
    }

    // query menu
    public static void queryMenu(Connection conn, Scanner sc) {
        boolean back = false;

        while (!back) {
            System.out.println("\n------ Query / Manage Data ------");
            System.out.println("1) List Patients");
            System.out.println("2) Search Patients (Last Name)");
            System.out.println("3) Update Appointment Status");
            System.out.println("4) Delete Medical History");
            System.out.println("5) Dentist Ranking");
            System.out.println("6) Monthly Billing Summary");
            System.out.println("B) Back");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim().toUpperCase();

            try {
                switch (choice) {
                    case "1": listPatients(conn); break;
                    case "2": searchPatientsByLastName(conn, sc); break;
                    case "3": updateAppointmentStatus(conn, sc); break;
                    case "4": deleteMedicalHistory(conn, sc); break;
                    case "5": showDentistRanking(conn); break;
                    case "6": showMonthlyBilling(conn); break;
                    case "B": back = true; break;
                    default: System.out.println("Invalid option.");
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    
    private static void listPatients(Connection conn) throws SQLException {
        String sql = "SELECT patient_id, first_name, last_name, city FROM patient ORDER BY patient_id";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.println("Patient ID | Name | City");
            while (rs.next()) {
                int id = rs.getInt("patient_id");
                String name = rs.getString("first_name") + " " +
                              rs.getString("last_name");
                String city = rs.getString("city");
                System.out.printf("%d | %s | %s%n", id, name, city);
            }
        }
    }

    private static void searchPatientsByLastName(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Enter last name prefix: ");
        String prefix = sc.nextLine().trim();

        String sql = "SELECT patient_id, first_name, last_name, city " +
                     "FROM patient WHERE UPPER(last_name) LIKE UPPER(?) " +
                     "ORDER BY last_name, first_name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String fname = rs.getString(2);
                    String lname = rs.getString(3);
                    String city = rs.getString(4);
                    System.out.printf("%d | %s %s | %s%n", id, fname, lname, city);
                }
            }
        }
    }

    private static void updateAppointmentStatus(Connection conn, Scanner sc) throws SQLException {
        System.out.print("Appointment ID: ");
        int id = Integer.parseInt(sc.nextLine());
        System.out.print("New status: ");
        String status = sc.nextLine().trim();

        String sql = "UPDATE appointment SET status=? WHERE appt_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Updated." : "Not found.");
        }
    }

    private static void deleteMedicalHistory(Connection conn, Scanner sc) throws SQLException {
        System.out.print("History ID to delete: ");
        int id = Integer.parseInt(sc.nextLine());

        String sql = "DELETE FROM medical_history WHERE history_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "Deleted." : "Not found.");
        }
    }

    private static void showDentistRanking(Connection conn) throws SQLException {
        String sql =
            "SELECT d.name, COUNT(a.appt_id) AS total " +
            "FROM dentist d " +
            "LEFT JOIN appointment a ON d.dentist_id=a.dentist_id " +
            "GROUP BY d.name ORDER BY total DESC";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%s | %d%n",
                        rs.getString(1), rs.getInt(2));
            }
        }
    }

    private static void showMonthlyBilling(Connection conn) throws SQLException {
        String sql =
            "SELECT TO_CHAR(a.appt_date,'YYYY-MM') AS month, SUM(b.amount) " +
            "FROM appointment a JOIN treatment t ON a.appt_id=t.appt_id " +
            "JOIN billing b ON t.treatment_id=b.treatment_id " +
            "GROUP BY TO_CHAR(a.appt_date,'YYYY-MM')";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%s | %.2f%n",
                        rs.getString(1), rs.getDouble(2));
            }
        }
    }
}

