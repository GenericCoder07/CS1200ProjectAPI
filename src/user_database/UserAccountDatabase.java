package user_database;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;

import javax.swing.JOptionPane;

import java_sql_lib_raymond.Database;
import java_sql_lib_raymond.Table;
import java_sql_lib_raymond.TableVar;
import password_hash_raymond.Hasher;

public class UserAccountDatabase
{
	static Database database;
	static Hasher hasher;

	public static void init(Hasher hash, Database db, HashMap<String, Table> tableNameMap)
	{
		database = db;
		hasher = hash;
		
		tableNameMap.put("users", new Table("users", 
				new TableVar("id", "IDENTITY", "PRIMARY", "KEY"), 
				new TableVar("username", "VARCHAR(100)", "NOT", "NULL", "UNIQUE"), 
				new TableVar("password_hash", "VARCHAR(255)", "NOT", "NULL"), 
				new TableVar("email", "VARCHAR(255)", "NOT", "NULL", "UNIQUE"), 
				new TableVar("session_id", "VARCHAR(255)", "UNIQUE"), 
				new TableVar("session_timestamp", "BIGINT", "NOT", "NULL"), 
				new TableVar("is_admin", "BOOLEAN", "NOT", "NULL", "DEFAULT", "FALSE"), 
				new TableVar("is_verified", "BOOLEAN", "NOT", "NULL", "DEFAULT", "FALSE"), 
				new TableVar("created_at", "TIMESTAMP", "NOT", "NULL", "DEFAULT", "CURRENT_TIMESTAMP")));
	}

	public static boolean addNewUserAccount(UserAccount account)
	{
		try
		{
			String insert = "INSERT INTO users (username, password_hash, email, is_admin, is_verified, session_id, session_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";

			account.session_id = Sessions.newSessionId();
			account.session_timestamp = Instant.now().toEpochMilli() + 1800000L;
			account.isVerified = false;

			PreparedStatement statement = database.runStatement(insert);
			statement.setString(1, account.username);
			statement.setString(2, hasher.hashPassword(account.password));
			statement.setString(3, account.email);
			statement.setBoolean(4, account.isAdmin);
			statement.setBoolean(5, account.isVerified);
			statement.setString(6, account.session_id);
			statement.setLong(7, account.session_timestamp);
			statement.executeUpdate();
			statement.close();
		} 
		catch (SQLException e)
		{
			doError(e, "While adding a new user account");
		}

		return true;
	}

	public static void resetSession(UserAccount user, boolean resetToken)
	{
		try
		{
			String update = "UPDATE users SET session_id = ?, session_timestamp = ? WHERE username = ?";

			System.out.println("reset token?: " + resetToken + "\nuser pre session_id:" + user.session_id);
			if(resetToken)
				user.session_id = Sessions.newSessionId();
			user.session_timestamp = Instant.now().toEpochMilli() + 1800000L;
			System.out.println("user post session_id:" + user.session_id);

			PreparedStatement statement = database.runStatement(update);
			statement.setString(1, user.session_id);
			statement.setLong(2, user.session_timestamp);
			statement.setString(3, user.username);
			statement.executeUpdate();
			statement.close();
		} 
		catch (SQLException e)
		{
			doError(e, "while reseting user session token");
		}
	}

	public static UserAccount getUserAccount(String username)
	{
		try
		{
			String sql = "SELECT * FROM users WHERE username = ?";

			PreparedStatement stmt = database.runStatement(sql);
			stmt.setString(1, username);


			System.out.println("Before query execution");
			ResultSet rs = stmt.executeQuery();
			System.out.println("After query execution");

			boolean hasNext = rs.next();
			
			if(!hasNext)
			{
				System.out.println("No account found");
				rs.close();
				stmt.close();
				return null;
			}

			System.out.println("Before getting data");
			UserAccount user = new UserAccount(rs.getString("username"), rs.getString("password_hash"), rs.getString("email"), rs.getBoolean("is_admin"));
			user.setSystemVars(rs.getString("session_id"), rs.getLong("session_timestamp"), rs.getBoolean("is_verified"));
			System.out.println("After getting data");

			rs.close();
			stmt.close();
			return user;
		} 
		catch (Exception e)
		{
			doError(e, "while getting user from database \"username\" query");
		}

		return null;
	}

	public static void updateUser(String username, UserAccount user)
	{
		try
		{
			String update = "UPDATE users SET username = ?, password_hash = ?, email = ?, is_admin = ?, is_verified = ?, session_id = ?, session_timestamp = ? WHERE username = ?";


			PreparedStatement statement = database.runStatement(update);
			statement.setString(1, user.username);
			statement.setString(2, user.password);
			statement.setString(3, user.email);
			statement.setBoolean(4, user.isAdmin);
			statement.setBoolean(5, user.isVerified);
			statement.setString(6, user.session_id);
			statement.setLong(7, user.session_timestamp);
			statement.setString(8, username);
			statement.executeUpdate();
			statement.close();
		} 
		catch (SQLException e)
		{
			doError(e, "while updating user account");
		}
	}

	public static UserAccount getUserAccountBySession(String session_id)
	{
		String sql = "SELECT * FROM users WHERE session_id = ?";

		try(PreparedStatement stmt = database.runStatement(sql))
		{
			stmt.setString(1, session_id);
			ResultSet rs = stmt.executeQuery();

			if(!rs.next())
			{
				rs.close();
				return null;
			}

			UserAccount user = new UserAccount(rs.getString("username"), rs.getString("password_hash"), rs.getString("email"), rs.getBoolean("is_admin"));
			user.setSystemVars(rs.getString("session_id"), rs.getLong("session_timestamp"), rs.getBoolean("is_verified"));

			if(user.session_timestamp <= Instant.now().toEpochMilli())
			{
				rs.close();
				return null;
			}

			rs.close();
			stmt.close();
			return user;
		} 
		catch (Exception e)
		{
			doError(e, "while getting user from database \"session\" query");
		}

		return null;
	}
	
	public static void doError(Exception e, String context)
	{
		Thread errorThread = new Thread(new Runnable() {

			public void run()
			{
				JOptionPane.showMessageDialog(null, e.getClass().getCanonicalName() + " caught error " + context + " - " + e.getMessage(), e.getClass().getCanonicalName(), JOptionPane.ERROR_MESSAGE);
			}
		});

		errorThread.start();
	}

}
