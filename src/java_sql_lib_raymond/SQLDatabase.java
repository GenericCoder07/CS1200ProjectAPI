package java_sql_lib_raymond;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLDatabase implements Database
{
	private Connection connection;
	
	/**
	 * Creates an SQLDatabase using the h2 library
	 * with filepath "./mydb", adminUsername "sa", adminPassword "", and memoryDatabase false
	 * equivalent to calling SQLDatabase("./mydb", "sa", "", false);
	 * @throws SQLException
	 */
	public SQLDatabase() throws SQLException
	{
		this("./mydb", "sa", "", false);
	}
	
	/**
	 * Creates an SQLDatabase using the h2 library
	 * with adminUsername "sa", adminPassword "", and memoryDatabase false
	 * equivalent to calling SQLDatabase(filepath, "sa", "", false);
	 * @param filepath The path to the database
	 * @throws SQLException
	 */
	public SQLDatabase(String filepath) throws SQLException
	{
		this(filepath, "sa", "", false);
	}
	
	/**
	 * Creates an SQLDatabase using the h2 library
	 * with memoryDatabase false
	 * equivalent to calling SQLDatabase(filepath, adminUsername, adminPassword, false);
	 * @param filepath The path to the database
	 * @param adminUsername The username required for further
	 * connections to the database
	 * @param adminPassword The password required for further
	 * connections to the database or "" for no password
	 * @throws SQLException
	 */
	public SQLDatabase(String filepath, String adminUsername, String adminPassword) throws SQLException
	{
		this(filepath, adminUsername, adminPassword, false);
	}
	
	/**
	 * Creates an SQLDatabase using the h2 library
	 * @param filepath The path to the database
	 * @param adminUsername The username required for further
	 * connections to the database
	 * @param adminPassword The password required for further
	 * connections to the database or "" for no password
	 * @param memoryDatabase Determines whether the database
	 * will stay in memory and get destroyed after program exit
	 * @throws SQLException
	 */
	public SQLDatabase(String filepath, String adminUsername, String adminPassword, boolean memoryDatabase) throws SQLException
	{
		try {
		    Class.forName("org.h2.Driver");
		    System.out.println("H2 driver class loaded.");
		} catch (ClassNotFoundException e) {
		    System.out.println("H2 driver NOT found: " + e);
		    e.printStackTrace();
		}
		
		System.out.println("Attempting to connect to sql database");
		connection = DriverManager.getConnection("jdbc:h2:" + (memoryDatabase ? "mem:" : "") + filepath, adminUsername, adminPassword);

		System.out.println("connected to sql database");
	}
	
	public PreparedStatement runStatement(String statement) throws SQLException
	{
		return connection.prepareStatement(statement);
	}
	
	public ResultSet runQuery(String query) throws SQLException
	{
		try(PreparedStatement statement = runStatement(query))
		{
			return statement.executeQuery();
		}
	}
}
