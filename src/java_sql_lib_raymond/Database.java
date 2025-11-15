package java_sql_lib_raymond;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface Database
{
	public PreparedStatement runStatement(String statement) throws SQLException;
	public ResultSet runQuery(String query) throws SQLException;
	
	public default void addTableIfAbsent(Table table)
	{
		try
		{
			System.out.println("Table created");

			StringBuilder statementString = new StringBuilder();

			statementString.append("CREATE TABLE IF NOT EXISTS ");
			statementString.append(table.getName());
			statementString.append("(\n");

			table.forEach(tableVar -> {
				statementString.append(tableVar.toString());
				statementString.append(",\n");
			});

			statementString.deleteCharAt(statementString.length() - 2);

			statementString.append("\n)\n");

			PreparedStatement statement = runStatement(statementString.toString());
			statement.execute();
			statement.close();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}

	public default void addTable(Table table)
	{
		try
		{
			StringBuilder statementString = new StringBuilder();

			statementString.append("CREATE TABLE ");
			statementString.append(table.getName());
			statementString.append("(\n");

			table.forEach(tableVar -> {
				statementString.append(tableVar.toString());
				statementString.append(",\n");
			});

			statementString.append("\n)\n");

			PreparedStatement statement = runStatement(statementString.toString());
			statement.execute();
			statement.close();
			System.out.println("Table created");
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
