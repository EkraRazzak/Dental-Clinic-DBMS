import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;



// Run in cmd
// javac -cp ".;ojdbc17.jar" DentalClinicApp.java DentalClinicGUI.java
// java -cp ".;ojdbc17.jar" DentalClinicGUI




public class DentalClinicGUI extends JFrame {

    private final Connection conn;
    private final JTextArea outputArea;

    public DentalClinicGUI(Connection conn) {
        super("Dental Clinic DB");
        this.conn = conn;
        this.outputArea = new JTextArea();
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // all the buttons on the top
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 5, 5));

        JButton dropBtn     = new JButton("Drop Tables");
        JButton createBtn   = new JButton("Create Tables");
        JButton populateBtn = new JButton("Populate");
        JButton queryBtn    = new JButton("Query / Manage");
        JButton exitBtn     = new JButton("Exit");

        buttonPanel.add(dropBtn);
        buttonPanel.add(createBtn);
        buttonPanel.add(populateBtn);
        buttonPanel.add(queryBtn);
        buttonPanel.add(exitBtn);

        add(buttonPanel, BorderLayout.NORTH);

        
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        // button action
        dropBtn.addActionListener(this::onDropTables);
        createBtn.addActionListener(this::onCreateTables);
        populateBtn.addActionListener(this::onPopulateTables);
        queryBtn.addActionListener(this::onQueryMenu);

        exitBtn.addActionListener(e -> {
            try {
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        setSize(900, 450);
        setLocationRelativeTo(null);
    }

    // button handler

    private void onDropTables(ActionEvent e) {
        appendLine("Dropping tables...");
        try {
            DentalClinicApp.dropTables(conn);
            appendLine("Drop done.");
        } catch (Exception ex) {
            appendLine("Error dropping tables: " + ex.getMessage());
        }
    }

    private void onCreateTables(ActionEvent e) {
        appendLine("Creating tables...");
        try {
            DentalClinicApp.createTables(conn);
            appendLine("Create done.");
        } catch (Exception ex) {
            appendLine("Error creating tables: " + ex.getMessage());
        }
    }

    private void onPopulateTables(ActionEvent e) {
        appendLine("Populating tables...");
        try {
            DentalClinicApp.populateTables(conn);
            appendLine("Populate done.");
        } catch (Exception ex) {
            appendLine("Error populating tables: " + ex.getMessage());
        }
    }

    private void onQueryMenu(ActionEvent e) {
        boolean back = false;

        while (!back) {
            String choice = JOptionPane.showInputDialog(
                    this,
                    """
                           Query / Manage Data 
                    1) List all patients
                    2) Search patients by last name
                    3) Update appointment status
                    4) Delete medical_history record
                    5) Dentist ranking (last 30 days)
                    6) Monthly billing by status
                    B) Back to main menu

                    Enter choice:""",
                    "Query / Manage",
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice == null) { // user cancelled
                break;
            }

            choice = choice.trim().toUpperCase();

            try {
                switch (choice) {
                    case "1" -> listPatientsGUI();
                    case "2" -> searchPatientsGUI();
                    case "3" -> updateAppointmentStatusGUI();
                    case "4" -> deleteMedicalHistoryGUI();
                    case "5" -> showDentistRankingGUI();
                    case "6" -> showMonthlyBillingGUI();
                    case "B" -> back = true;
                    default -> JOptionPane.showMessageDialog(
                            this,
                            "Invalid option.",
                            "Query / Manage",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            } catch (SQLException ex) {
                appendLine("SQL error: " + ex.getMessage());
            }
        }
    }

    private void appendLine(String msg) {
        outputArea.append(msg + "\n");
    }

    // gui queries

    private void listPatientsGUI() throws SQLException {
        String sql = """
                SELECT patient_id, first_name, last_name, city
                FROM patient
                ORDER BY patient_id
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append("Patient ID | Name               | City\n");
            sb.append("----------------------------------------------\n");

            while (rs.next()) {
                int id = rs.getInt("patient_id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                String city = rs.getString("city");
                sb.append(String.format("%9d | %-18s | %s%n", id, name, city));
            }

            appendLine(sb.toString());
        }
    }

    private void searchPatientsGUI() throws SQLException {
        String prefix = JOptionPane.showInputDialog(
                this,
                "Enter last name prefix to search:",
                "Search Patients",
                JOptionPane.QUESTION_MESSAGE
        );
        if (prefix == null || prefix.trim().isEmpty()) return;

        String sql = """
                SELECT patient_id, first_name, last_name, city
                FROM patient
                WHERE UPPER(last_name) LIKE UPPER(?)
                ORDER BY last_name, first_name
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Patient ID | Name               | City\n");
                sb.append("----------------------------------------------\n");

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    int id = rs.getInt("patient_id");
                    String name = rs.getString("first_name") + " " + rs.getString("last_name");
                    String city = rs.getString("city");
                    sb.append(String.format("%9d | %-18s | %s%n", id, name, city));
                }

                if (!any) {
                    sb.append("No patients found matching that prefix.\n");
                }
                appendLine(sb.toString());
            }
        }
    }

    private void updateAppointmentStatusGUI() throws SQLException {
        String apptIdStr = JOptionPane.showInputDialog(
                this,
                "Enter appointment ID to update:",
                "Update Appointment",
                JOptionPane.QUESTION_MESSAGE
        );
        if (apptIdStr == null || apptIdStr.trim().isEmpty()) return;

        int apptId = Integer.parseInt(apptIdStr.trim());

        String status = JOptionPane.showInputDialog(
                this,
                "Enter new status (SCHEDULED, COMPLETED, CANCELLED, NO_SHOW):",
                "Update Appointment",
                JOptionPane.QUESTION_MESSAGE
        );
        if (status == null || status.trim().isEmpty()) return;

        status = status.trim().toUpperCase();

        String sql = "UPDATE appointment SET status = ? WHERE appt_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, apptId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                appendLine("Appointment " + apptId + " updated to " + status + ".");
            } else {
                appendLine("No appointment found with ID " + apptId + ".");
            }
        }
    }

    private void deleteMedicalHistoryGUI() throws SQLException {
        String histIdStr = JOptionPane.showInputDialog(
                this,
                "Enter history_id to delete:",
                "Delete Medical History",
                JOptionPane.QUESTION_MESSAGE
        );
        if (histIdStr == null || histIdStr.trim().isEmpty()) return;

        int histId = Integer.parseInt(histIdStr.trim());

        String sql = "DELETE FROM medical_history WHERE history_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, histId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                appendLine("Medical history record " + histId + " deleted.");
            } else {
                appendLine("No history record found with ID " + histId + ".");
            }
        }
    }

    private void showDentistRankingGUI() throws SQLException {
        String sql = """
                SELECT d.name AS dentist,
                       COUNT(a.appt_id) AS appts_30d,
                       DENSE_RANK() OVER (ORDER BY COUNT(a.appt_id) DESC) AS rnk
                FROM dentist d
                LEFT JOIN appointment a
                  ON a.dentist_id = d.dentist_id
                 AND a.appt_date BETWEEN TRUNC(SYSDATE) - 30 AND TRUNC(SYSDATE)
                GROUP BY d.name
                ORDER BY rnk, dentist
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append("Dentist                 | Appts(30d) | Rank\n");
            sb.append("------------------------------------------------\n");

            while (rs.next()) {
                String name = rs.getString("dentist");
                int appts = rs.getInt("appts_30d");
                int rank  = rs.getInt("rnk");
                sb.append(String.format("%-22s | %9d | %4d%n", name, appts, rank));
            }

            appendLine(sb.toString());
        }
    }

    private void showMonthlyBillingGUI() throws SQLException {
        String sql = """
                SELECT TO_CHAR(TRUNC(a.appt_date,'MM'),'YYYY-MM') AS month,
                       SUM(CASE WHEN b.payment_status = 'PAID'    THEN b.amount END) AS paid,
                       SUM(CASE WHEN b.payment_status = 'PENDING' THEN b.amount END) AS pending,
                       SUM(CASE WHEN b.payment_status = 'OVERDUE' THEN b.amount END) AS overdue,
                       SUM(b.amount) AS total
                FROM appointment a
                JOIN treatment t ON t.appt_id = a.appt_id
                JOIN billing   b ON b.treatment_id = t.treatment_id
                GROUP BY TRUNC(a.appt_date,'MM')
                ORDER BY TRUNC(a.appt_date,'MM')
                """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            StringBuilder sb = new StringBuilder();
            sb.append("Month    | Paid    | Pending | Overdue | Total\n");
            sb.append("------------------------------------------------\n");

            while (rs.next()) {
                String month   = rs.getString("month");
                double paid    = rs.getDouble("paid");
                double pending = rs.getDouble("pending");
                double overdue = rs.getDouble("overdue");
                double total   = rs.getDouble("total");

                sb.append(String.format("%-7s | %7.2f | %7.2f | %7.2f | %7.2f%n",
                        month, paid, pending, overdue, total));
            }

            appendLine(sb.toString());
        }
    }

    // GUI starter

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");

            Connection conn = DriverManager.getConnection(
                    DentalClinicApp.DB_URL,
                    DentalClinicApp.DB_USER,
                    DentalClinicApp.DB_PASS
            );

            System.out.println("Connected to Oracle from GUI.");

            SwingUtilities.invokeLater(() -> {
                DentalClinicGUI gui = new DentalClinicGUI(conn);
                gui.setVisible(true);
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Database connection failed:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
