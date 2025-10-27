package java_sql_lib_raymond;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;

import javax.swing.JOptionPane;

public class SupabaseDatabase implements Database
{
	public static void main(String[] args) throws Exception 
	{
        String url = loadDatabaseURL();

        try (Connection conn = DriverManager.getConnection(url)) 
        {
            System.out.println("✅ Connected to Supabase Postgres!");
            
            // Example query
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT NOW()")) 
            {
                if (rs.next()) 
                {
                    System.out.println("Server time: " + rs.getString(1));
                }
            }
        } catch (SQLException e) 
        {
            e.printStackTrace();
        }
	}

	static String loadDatabaseURL() 
	{
		try 
		{
            return Files.readString(new File(new File("databasekey.txt").getAbsolutePath()).toPath()).trim();
        } 
		catch (Exception e) 
		{
			JOptionPane.showMessageDialog(null, "No Supabase Database url is detected. If you continue,\n"
											  + "the server will still run, but any request to\n"
											  + "access the database will return an error.", 
											  "No Supabase Database URL", JOptionPane.ERROR_MESSAGE);
        	return null;
        }
	}

	public PreparedStatement runStatement(String statement) throws SQLException
	{
		return null;
	}

	public ResultSet runQuery(String query) throws SQLException
	{
		return null;
	}
}
