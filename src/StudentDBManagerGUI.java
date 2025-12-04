import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;



public class StudentDBManagerGUI extends JFrame {

    private final DatabaseManager dbManager;

    // Input Fields
    private final JTextField txtFirstName = new JTextField(20);
    private final JTextField txtLastName = new JTextField(20);
    private final JTextField txtAge = new JTextField(5);
    private final JTextField txtEmail = new JTextField(20);
    private final JTextField txtSearchId = new JTextField(5);

    // Buttons
    private final JButton btnAddStudent = new JButton("Add Student");
    private final JButton btnViewStudents = new JButton("View All Students");
    private final JButton btnSearchStudent = new JButton("Search by ID");

    // Display Components
    private final JTable studentTable;
    private final DefaultTableModel tableModel;
    private final JTextArea statusArea = new JTextArea(5, 40);

    // Table Column Headers
    private static final String[] COLUMN_NAMES = {"ID", "First Name", "Last Name", "Age", "Email"};

    
    public StudentDBManagerGUI() {
        this.dbManager = new DatabaseManager();

        // Setup the JTable model
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            // Override isCellEditable to make the table read-only
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        
        setTitle("Student Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initializeGUI();
        addListeners();
        pack();
        setLocationRelativeTo(null); // Center the window
    }

   
    private void initializeGUI() {
        // Use BorderLayout for the main frame: Inputs on NORTH, Table in CENTER, Status in SOUTH
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        // --- 1. Input Panel (NORTH) ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Student Details & Actions"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: First Name & Last Name
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; inputPanel.add(txtFirstName, gbc);
        gbc.gridx = 2; gbc.gridy = 0; inputPanel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; inputPanel.add(txtLastName, gbc);

        // Row 1: Age & Email
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Age (Years):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; inputPanel.add(txtAge, gbc);
        gbc.gridx = 2; gbc.gridy = 1; inputPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; inputPanel.add(txtEmail, gbc);

        // Row 2: Add Button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; inputPanel.add(btnAddStudent, gbc);
        gbc.gridwidth = 1; // Reset width

        // Row 3: View Button
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; inputPanel.add(btnViewStudents, gbc);

        // Row 3: Search Components
        gbc.gridx = 2; gbc.gridy = 3; inputPanel.add(txtSearchId, gbc);
        gbc.gridx = 3; gbc.gridy = 3; inputPanel.add(btnSearchStudent, gbc);
        gbc.gridwidth = 1; // Reset width

        contentPane.add(inputPanel, BorderLayout.NORTH);

        // --- 2. Table Panel (CENTER) ---
        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Student Records"));
        contentPane.add(scrollPane, BorderLayout.CENTER);
        
        // --- 3. Status Panel (SOUTH) ---
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder("Status / Message Log"));
        contentPane.add(statusScrollPane, BorderLayout.SOUTH);

        // Initial status check
        updateStatus("Application loaded. Please remember to configure DB credentials in DatabaseManager.java.");
    }

    private void updateStatus(String message) {
        // Ensure status update runs on the EDT
        SwingUtilities.invokeLater(() -> statusArea.append(message + "\n"));
    }

   
    private void addListeners() {
        btnAddStudent.addActionListener(e -> addStudentAction());
        btnViewStudents.addActionListener(e -> viewStudentsAction());
        btnSearchStudent.addActionListener(e -> searchStudentAction());
        
        // --- START OF FIX: Add ActionListener to txtSearchId for Enter key press ---
        txtSearchId.addActionListener(e -> searchStudentAction());
        // --- END OF FIX ---
    }

    private void addStudentAction() {
        // 1. Input Validation and Preparation
        String fName = txtFirstName.getText().trim();
        String lName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String ageText = txtAge.getText().trim();

        if (fName.isEmpty() || lName.isEmpty() || email.isEmpty() || ageText.isEmpty()) {
            updateStatus("Error: All fields must be filled for Add operation.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
            if (age <= 0 || age > 150) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            updateStatus("Error: Age must be a valid positive number.");
            return;
        }

        Student newStudent = new Student(fName, lName, age, email);
        
        // Disable buttons to indicate ongoing operation
        setFormEnabled(false);
        updateStatus("Attempting to add student: " + newStudent.getFirstName() + "...");

        // 2. Execute DB Operation in a background thread using SwingWorker
        new SwingWorker<Void, Void>() {
            private Exception exception = null;

            // This runs on a worker thread (background)
            @Override
            protected Void doInBackground() {
                try {
                    dbManager.addStudent(newStudent);
                } catch (Exception e) {
                    exception = e;
                }
                return null;
            }

            // This runs on the Event Dispatch Thread (EDT) when doInBackground is finished
            @Override
            protected void done() {
                setFormEnabled(true); // Re-enable buttons

                if (exception == null) {
                    updateStatus("SUCCESS: Student " + newStudent.getFirstName() + " added successfully.");
                    clearInputFields();
                    viewStudentsAction(); // Refresh the table automatically
                } else {
                    updateStatus("DB Error adding student: " + exception.getMessage());
                }
            }
        }.execute();
    }

    
    private void viewStudentsAction() {
        setFormEnabled(false);
        updateStatus("Fetching all student records...");

        // Execute DB Operation in a background thread using SwingWorker
        new SwingWorker<List<Student>, Void>() {
            private Exception exception = null;

            // This runs on a worker thread (background)
            @Override
            protected List<Student> doInBackground() {
                try {
                    return dbManager.viewStudents();
                } catch (Exception e) {
                    exception = e;
                    return null;
                }
            }

            // This runs on the Event Dispatch Thread (EDT) when doInBackground is finished
            @Override
            protected void done() {
                setFormEnabled(true); // Re-enable buttons

                if (exception == null) {
                    try {
                        List<Student> students = get(); // Get the result from doInBackground
                        updateStatus("SUCCESS: Fetched " + students.size() + " student records.");
                        updateTable(students);
                    } catch (Exception e) {
                        updateStatus("Error retrieving result: " + e.getMessage());
                    }
                } else {
                    updateStatus("DB Error viewing students: " + exception.getMessage());
                }
            }
        }.execute();
    }

    
    private void searchStudentAction() {
        String idText = txtSearchId.getText().trim();
        if (idText.isEmpty()) {
            updateStatus("Error: Please enter an ID to search.");
            return;
        }

        int searchId;
        try {
            searchId = Integer.parseInt(idText);
        } catch (NumberFormatException ex) {
            updateStatus("Error: Search ID must be a valid number.");
            return;
        }

        // We capture the final search ID variable to be used safely inside the SwingWorker
        final int finalSearchId = searchId;

        setFormEnabled(false);
        updateStatus("Searching for student with ID: " + finalSearchId + "...");

        // Execute DB Operation in a background thread using SwingWorker
        new SwingWorker<Student, Void>() {
            private Exception exception = null;

            // This runs on a worker thread (background)
            @Override
            protected Student doInBackground() {
                try {
                    // Use the captured finalSearchId variable
                    return dbManager.searchStudentById(finalSearchId);
                } catch (Exception e) {
                    exception = e;
                    return null;
                }
            }

            // This runs on the Event Dispatch Thread (EDT) when doInBackground is finished
            @Override
            protected void done() {
                setFormEnabled(true); // Re-enable buttons
                
                if (exception == null) {
                    try {
                        Student student = get();
                        if (student != null) {
                            updateStatus("SUCCESS: Found Student: " + student.toString());
                            // Show only the single result in the table
                            updateTable(java.util.Collections.singletonList(student)); 
                        } else {
                            // If student is null, it means no record was found
                            updateStatus("INFO: Student with ID " + finalSearchId + " not found. Clearing table.");
                            updateTable(java.util.Collections.emptyList()); // Clear table
                        }
                    } catch (Exception e) {
                        updateStatus("Error retrieving result: " + e.getMessage());
                    }
                } else {
                    updateStatus("DB Error searching student: " + exception.getMessage());
                }
            }
        }.execute();
    }

    
    private void clearInputFields() {
        txtFirstName.setText("");
        txtLastName.setText("");
        txtAge.setText("");
        txtEmail.setText("");
        // txtSearchId is intentionally not cleared here
    }

  
    private void updateTable(List<Student> students) {
        tableModel.setRowCount(0); // Clear existing data
        if (students != null) {
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                    s.getId(), s.getFirstName(), s.getLastName(), s.getAge(), s.getEmail()
                });
            }
        }
    }

    
    private void setFormEnabled(boolean enabled) {
        btnAddStudent.setEnabled(enabled);
        btnViewStudents.setEnabled(enabled);
        btnSearchStudent.setEnabled(enabled);
        txtFirstName.setEnabled(enabled);
        txtLastName.setEnabled(enabled);
        txtAge.setEnabled(enabled);
        txtEmail.setEnabled(enabled);
        txtSearchId.setEnabled(enabled);
        
        if (!enabled) {
             updateStatus("... Operation running in background thread. UI remains responsive. Please wait.");
        }
    }

    /**
     * Main method to run the application on the Event Dispatch Thread (EDT).
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> {
            try {
                // Set look and feel to system default for better aesthetics
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Could not set System Look and Feel.");
            }
            new StudentDBManagerGUI().setVisible(true);
        });
    }
}