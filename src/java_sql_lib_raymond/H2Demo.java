package java_sql_lib_raymond;
import java.sql.*;

public class H2Demo 
{
    public static void main(String[] args) throws SQLException 
    {
        // Creates file mydb.mv.db in your project directory
        String url = "jdbc:h2:./mydb";
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) 
        {

        	try (Statement s = conn.createStatement()) 
        	{
                s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS person (
                      id IDENTITY PRIMARY KEY,
                      name VARCHAR(100),
                      age INT
                    )
                """);
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO person(name, age) VALUES (?, ?)")) 
            {
                ps.setString(1, "Raymond");
                ps.setInt(2, 17);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM person");
                 ResultSet rs = ps.executeQuery()) 
            {
                while (rs.next()) 
                {
                    System.out.println(rs.getInt("id") + " " +
                                       rs.getString("name") + " " +
                                       rs.getInt("age"));
                }
            }
        }
    }
}