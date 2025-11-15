package post_database;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;

import javax.swing.JOptionPane;

import java_sql_lib_raymond.Database;
import java_sql_lib_raymond.Table;
import java_sql_lib_raymond.TableVar;
import user_database.Sessions;

public class PostDatabase
{
	static Database database;
	
	public static void init(Database database, HashMap<String, Table> tableNameMap)
	{
		PostDatabase.database = database;
		tableNameMap.put("posts", new Table("posts", 
				new TableVar("id", "IDENTITY", "PRIMARY", "KEY"), 
				new TableVar("username", "VARCHAR(100)", "NOT", "NULL", "UNIQUE"), 
				new TableVar("text", "VARCHAR(1000)", "NOT", "NULL"), 
				new TableVar("ai_text", "VARCHAR(1000)", "NOT", "NULL"), 
				new TableVar("agree_responses", "INT", "DEFAULT", "0"),
				new TableVar("disagree_responses", "INT", "DEFAULT", "0"),
				new TableVar("created_at", "TIMESTAMP", "DEFAULT", "CURRENT_TIMESTAMP")
				));
	}
	
	public static void createPost(Post newPost)
	{
		try
		{
			String insert = "INSERT INTO posts (username, text, ai_text, agree_responses, disagree_responses) VALUES (?, ?, ?, ?, ?, ?)";
			
			newPost.agreeResponses = 0;
			newPost.disagreeResponses = 0;
			
			PreparedStatement statement = database.runStatement(insert);
			statement.setString(1, newPost.username);
			statement.setString(2, newPost.text);
			statement.setString(3, newPost.aiText);
			statement.setInt(4, newPost.agreeResponses);
			statement.setInt(5, newPost.disagreeResponses);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			doError(e, "While creating a new post");
		}
	}
	
	public static void updatePost(Post newPost, int agreeChange, int disagreeChange)
	{
		
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
