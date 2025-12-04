import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



public class DatabaseManager {

   
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/StudentDB?useSSL=false&serverTimezone=UTC";
    private static final String USERNAME = "root";   // <-- CHANGE THIS
    private static final String PASSWORD = "54513"; // <-- CHANGE THIS

   
    private Connection getConnection() throws SQLException {
        
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    
    public void addStudent(Student student) throws SQLException {
        String sql = "INSERT INTO students (first_name, last_name, age, email) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, student.getFirstName());
            stmt.setString(2, student.getLastName());
            stmt.setInt(3, student.getAge());
            stmt.setString(4, student.getEmail());
            
            stmt.executeUpdate();
            
        } finally {
            // Ensure resources are closed, even if an exception occurs
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }

    
    public List<Student> viewStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, age, email FROM students";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                int age = rs.getInt("age");
                String email = rs.getString("email");
                
                students.add(new Student(id, firstName, lastName, age, email));
            }
            
        } finally {
            // Ensure resources are closed
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
        return students;
    }

    
    public Student searchStudentById(int id) throws SQLException {
        Student student = null;
        String sql = "SELECT id, first_name, last_name, age, email FROM students WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Found a student
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                int age = rs.getInt("age");
                String email = rs.getString("email");
                
                student = new Student(id, firstName, lastName, age, email);
            }
            
        } finally {
            // Ensure resources are closed
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
        return student;
    }
}