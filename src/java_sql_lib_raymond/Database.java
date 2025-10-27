package java_sql_lib_raymond;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface Database
{
	public PreparedStatement runStatement(String statement) throws SQLException;
	public ResultSet runQuery(String query) throws SQLException;
}
