package post_database;
import api.CS1200API;
import java_sql_lib_raymond.Table;
import java_sql_lib_raymond.TableVar;

public class PostDatabase
{
	public static void init()
	{
		CS1200API.tableNameMap.put("posts", new Table("posts", 
				new TableVar("username", "VARCHAR(100)", "NOT", "NULL", "UNIQUE"), 
				new TableVar("text", "VARCHAR(1000)", "NOT", "NULL"), 
				new TableVar("ai_text", "VARCHAR(1000)", "NOT", "NULL"), 
				new TableVar("agree_responses", "INT", "DEFAULT", "0"),
				new TableVar("disagree_responses", "INT", "DEFAULT", "0"),
				new TableVar("creation_timestamp", "BIGINT", "NOT", "NULL"), 
				new TableVar("created_at", "TIMESTAMP", "DEFAULT", "CURRENT_TIMESTAMP")
				));
	}
}
