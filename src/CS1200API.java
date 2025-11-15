import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java_sql_lib_raymond.Database;
import java_sql_lib_raymond.SQLDatabase;
import java_sql_lib_raymond.SupabaseDatabase;

public class CS1200API
{
	static SQLDatabase sqlDatabase;
	static SupabaseDatabase supabaseDatabase;
	static Database database;
	static HashMap<String, Table> tableNameMap = new HashMap<>();
	
	static class Table
	{
		private String name;
		private HashMap<String, TableVar> tableVarNameMap;
		private TableVar[] tableVars;
		public Table(String name, TableVar... tableVars)
		{
			this.name = name;
			this.tableVars = tableVars;
			
			tableVarNameMap = new HashMap<>();
			
			for(TableVar tableVar : tableVars)
				tableVarNameMap.put(tableVar.getName(), tableVar);
		}
		
		public String getName()
		{
			return name;
		}
		
		public TableVar getTableVar(int index)
		{
			return tableVars[index];
		}
		
		public TableVar getTableVar(String name)
		{
			return tableVarNameMap.get(name);
		}
		
		public void forEach(Consumer<TableVar> func)
		{
			for(TableVar tableVar : tableVars)
				func.accept(tableVar);
		}
		
		public TableVar[] getTableVars()
		{
			return tableVars;
		}
	}
	
	static class TableVar
	{
		private String name, type, modifiers[];
		
		public TableVar(String name, String type, String... modifiers)
		{
			this.name = name;
			this.type = type;
			this.modifiers = modifiers.clone();
		}
		
		public String getName()
		{
			return name;
		}
		
		public String toString()
		{
			StringBuilder result = new StringBuilder();
			
			result.append(name);
			result.append(" ");
			result.append(type);
			result.append(" ");
			
			for(String modifier : modifiers)
			{
				result.append(modifier);
				result.append(" ");
			}
			
			return result.toString().trim();
		}
	}
	
	static void addTableIfAbsent(Table table)
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
			
			PreparedStatement statement = database.runStatement(statementString.toString());
			statement.execute();
			statement.close();
		} 
		catch (SQLException e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	static void addTable(Table table)
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
			
			PreparedStatement statement = database.runStatement(statementString.toString());
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
	
	
	static
	{
		
		try
		{
			sqlDatabase = new SQLDatabase("./sqldb/mydb");
			supabaseDatabase = new SupabaseDatabase();
			
			database = sqlDatabase;
		} 
		catch (SQLException e)
		{
			doError(e, "");
		}
		
		UserAccountDatabase.init();
		PostDatabase.init();
		
		tableNameMap.forEach((name, table) -> {
			addTableIfAbsent(table);
		});
		
	}
	
	static class UserAccount
	{
		String username;
		String password;
		String email;
		String session_id;
		long session_timestamp;
		boolean isAdmin;
		boolean isVerified;
		
		public UserAccount(String username, String password, String email, boolean isAdmin)
		{
			this.username = username;
			this.password = password;
			this.email = email;
			this.isAdmin = isAdmin;
		}
		
		protected void setSystemVars(String session_id, long session_timestamp, boolean isVerified)
		{
			this.session_id = session_id;
			this.session_timestamp = session_timestamp;
			this.isVerified = isVerified;
		}
	}
	
	static class Post
	{
		String username, text, aiText;
		int agreeResponses, disagreeResponses;
		long creationTimestamp;
		
	}
	
	static final class Sessions 
	{
	    private static final SecureRandom RNG = new SecureRandom();

	    public static String newSessionId() 
	    {
	    	byte[] buf = new byte[32];
	    	RNG.nextBytes(buf);
	    	return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	    }
	}
	
	static class PostDatabase
	{
		static void init()
		{
			tableNameMap.put("posts", new Table("posts", 
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
	
	static class UserAccountDatabase
	{
		
		static void init()
		{
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
		
		static boolean addNewUserAccount(UserAccount account)
		{
			try
			{
				String insert = "INSERT INTO users (username, password_hash, email, is_admin, is_verified, session_id, session_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
	            
				account.session_id = Sessions.newSessionId();
				account.session_timestamp = Instant.now().toEpochMilli() + 1800000L;
				account.isVerified = false;
				
				PreparedStatement statement = database.runStatement(insert);
				statement.setString(1, account.username);
	            statement.setString(2, hashPassword(account.password));  // 🔒 see below
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
				e.printStackTrace();
				System.exit(1);
			}
			
			return true;
		}
		
		static void resetSession(UserAccount user, boolean resetToken)
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
		
		static UserAccount getUserAccount(String username)
		{
			try
			{
				String sql = "SELECT * FROM users WHERE username = ?";

				PreparedStatement stmt = database.runStatement(sql);
				stmt.setString(1, username);

				
				System.out.println("Before query execution");
				ResultSet rs = stmt.executeQuery();
				System.out.println("After query execution");

				System.out.println("Result set next: " + rs.next());
				
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
				String update = "UPDATE accounts SET username = ?, password_hash = ?, email = ?, is_admin = ?, is_verified = ?, session_id = ?, session_timestamp = ? WHERE username = ?";
				
				
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
				
				System.out.println("Quesry next: " + rs.next());
				
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
	}
	
	static HashMap<Integer, String> verificationCodes = new HashMap<>();
	
	public static void doError(Exception e, String context)
	{
		JOptionPane.showMessageDialog(null, e.getClass().getCanonicalName() + " caught error " + context + " - " + e.getMessage(), e.getClass().getCanonicalName(), JOptionPane.ERROR_MESSAGE);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, InterruptedException, SQLException
	{
		loadApiKey();
		
		//String aiText = sendChatGPTAPIRequest("You are a helpful assistant that will generate x number of random statements. The user will give their response as: \"Generate x statements\" and you will respond with that number of statements seperated by a | character.", "Generate 4 statements");
		//System.out.println(aiText);
		
		ServerManager manager = new ServerManager();
		manager.setVisible(true);
		
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		
		server.setExecutor(Executors.newFixedThreadPool(10));
		
		server.createContext("/", new MyHandler("api.html"));
		server.createContext("/api/post/create", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String session_id = JSON.getString("session-id");
					String postContent = JSON.getString("post-content");
					
					UserAccount user = UserAccountDatabase.getUserAccountBySession(session_id);
					
					if(session_id == null)
						user = null;
					
					JSONObject response = new JSONObject();
					
					if(user == null)
					{
						response.put("response", 200);
						response.put("successful", false);
						response.put("response-text", "Expired Session ID");
						
						return response;
					}
					
					String aiText = sendChatGPTAPIRequest("You are a helpful assistant that will get a question and output a pros and cons list of the pros and cons of agreeing to that question. You must keep your response under 200 characters", postContent);
					
					
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
			
		});
		server.createContext("/api/accounts/signin", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String username = JSON.getString("username");
					String password = JSON.getString("password");
					String email = JSON.getString("email");
					
					UserAccount user = new UserAccount(username, password, email, false);
					
					UserAccountDatabase.addNewUserAccount(user);
					
					JSONObject response = new JSONObject();
					response.put("response", 200);
					response.put("successful", true);
					response.put("response-text", "Account successfully Created");
					response.put("account", user.session_id);
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
			
		});
		server.createContext("/api/accounts/signout", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String session_id = JSON.getString("session-id");
					
					UserAccount user = UserAccountDatabase.getUserAccountBySession(session_id);
					
					user.session_id = null;
					user.session_timestamp = 0;
					
					JSONObject response = new JSONObject();
					response.put("response", 200);
					response.put("successful", true);
					response.put("response-text", "Account successfully logged out");
					response.put("account", "null");
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
			
		});
		server.createContext("/api/accounts/reset_password/verify", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String email = JSON.getString("email");
					String password = JSON.getString("new-password");
					int code = JSON.getInt("code");
					
					
					
					String emailStored = verificationCodes.get(code);
					
					JSONObject response = new JSONObject();
					
					if(emailStored != null && emailStored.equals(email))
					{
						response.put("response", 200);
						response.put("successful", true);
						response.put("response-text", "Password successfully changed");
					}
					else 
					{
						response.put("response", 200);
						response.put("successful", false);
						response.put("response-text", "Code is incorrect");
					}
					
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
			
		});
		server.createContext("/api/accounts/reset_password", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String email = JSON.getString("email");
					
					if(!checkAccountExists(email))
					{
						JSONObject response = new JSONObject();
						response.put("response", 200);
						response.put("successful", false);
						response.put("response-text", "Account does not exist");
						return response;
					}
					
					int code = new Random().nextInt(100000, 1000000);
					
					while(verificationCodes.get(code) != null)
						code = new Random().nextInt(100000, 1000000);
					
					verificationCodes.put(code, email);
					
					JSONObject response = new JSONObject();
					response.put("response", 200);
					response.put("successful", true);
					response.put("response-text", "Code sent");
					
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					

					return response;
				}
			}
			
		});
		server.createContext("/api/accounts/login", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				String session_id = null;
				try 
				{
					session_id = JSON.getString("session-id");
				}
				catch(Exception e) {}
				
				try
				{
					String username, password_hash;
					UserAccount user;
					
					if(session_id == null)
					{
						username = JSON.getString("username");
						password_hash = hashPassword(JSON.getString("password"));
						user = UserAccountDatabase.getUserAccount(username);
						
						if(!user.password.equals(password_hash))
							user = null;
					}
					else 
					{
						user = UserAccountDatabase.getUserAccountBySession(session_id);
					}
					
					
					JSONObject response = new JSONObject();
					if(user != null)
					{
						UserAccountDatabase.resetSession(user, session_id == null);
						
						response.put("response", 200);
						response.put("successful", true);
						response.put("response-text", "Login Successful");
						response.put("account", user.session_id);
						System.out.println(response.toString());
					}
					else if(session_id != null)
					{
						response.put("response", 200);
						response.put("successful", false);
						response.put("response-text", "Login Unsuccessful Expired Session ID");
						response.put("account", "null");
						System.out.println(response.toString());
					}
					else
					{
						response.put("response", 200);
						response.put("successful", false);
						response.put("response-text", "Login Unsuccessful Invalid Credentials");
						response.put("account", "null");
						System.out.println(response.toString());
					}
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
			
		});
		
		server.createContext("/api/ai", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					String text = JSON.getString("msg");
					
					String aiText = sendChatGPTAPIRequest("You are a helpful assistant that will get a question and output a pros and cons list of the pros and cons of agreeing to that question. You must keep your response under 200 characters", text);
					
					JSONObject response = new JSONObject();
					response.put("response", 200);
					response.put("successful", true);
					response.put("response-text", "OpenAI response successfully accessed");
					response.put("ai-text", aiText);
					
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
		});
		
		server.createContext("/api/ai/can", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				if(JSON == null)
					return nullResponse();
				
				try
				{
					int quantity = JSON.getInt("quantity");
					
					System.out.println("got /api/ai/can request with \nquantity: " + quantity);
					String[] statements = new String[quantity];
					
					String aiText = sendChatGPTAPIRequest("You are a helpful assistant that will generate x number of random statements that would be posted by a user. Other users will respond with their agreements or disagreements. The user will give their response as: \"Generate x statements\" and you will respond with that number of statements seperated by a | character.", "Generate " + quantity + " statements");
					
					System.out.println("ai-text generated : " + aiText);
					
					String[] temp = aiText.split("\\|", quantity);
					for(int i = 0; i < quantity; i++)
						if(i < temp.length)
							statements[i] = temp[i];
					
					JSONObject response = new JSONObject();
					response.put("response", 200);
					response.put("successful", true);
					response.put("response-text", "OpenAI response successfully accessed");
					
					for(int i = 0; i < quantity; i++)
						response.put("ai-text-" + (i+1), statements[i]);
					
					System.out.println(response.toString());
					
					return response;
				} 
				catch (JSONException e)
				{
					JSONObject response = new JSONObject();
					response.put("response", 400);
					response.put("successful", false);
					response.put("response-text", "Bad request");
					
					System.out.println(response.toString());
					
					return response;
				}
			}
		});
		
		server.start();
		System.out.println("Server Running v1.22.0");
		Thread.currentThread().join();
	}
	
	static boolean checkAccountExists(String email)
	{
		return true;
	}

	public static String hashPassword(String string)
	{
		return string;
	}

	static String loadApiKey() 
	{
		try 
		{
			System.out.println("Trying file path: " + new File("apikey.txt").getAbsolutePath().replace("/src", ""));
            return Files.readString(new File(new File("apikey.txt").getAbsolutePath().replace("/src", "")).toPath()).trim();
        } 
		catch (Exception e) 
		{
			try
			{
				System.out.println("Trying file path: " + new File("apikey.txt").getAbsolutePath().replace("/src", "").replace("C:\\", "\\mn\\c\\"));
	            return Files.readString(new File(new File("apikey.txt").getAbsolutePath().replace("C:\\", "\\mn\\c\\")).toPath()).trim();
			} 
			catch (Exception e2)
			{
				JOptionPane.showMessageDialog(null, "No OpenAI API key is detected. If you continue,\n"
						  + "the server will still run, but any request to\n"
						  + "the /ai or /ai/can endpoints will return an error.", 
						  "No OpenAI API Key", JOptionPane.ERROR_MESSAGE);
			}
			
        	return null;
        }
	}
	
	protected static String sendChatGPTAPIRequest(String prompt, String input)
	{
		String apiKeyString = loadApiKey();
		
		System.out.println("API STRING KEY - " + apiKeyString);
		
		prompt = prompt.replace("\"", "'");
		
		String body =
		        "{" + 
		          "\"model\": \"gpt-5-nano\"," + 
		          "\"messages\": [" + 
		            "{\"role\": \"system\", \"content\": \"" + prompt + "\"}," + 
		            "{\"role\": \"user\", \"content\": \"" + input + "\"}" + 
		          "]" + 
		        "}";
		
		System.out.println(body);
		
		HttpRequest request = HttpRequest.newBuilder()
		                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
		                .header("Content-Type", "application/json")
		                .header("Authorization", "Bearer " + apiKeyString)
		                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
		                .build();

		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response = null;
		
		try
		{
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e)
		{
		} catch (InterruptedException e)
		{
		}
		
		String responseBody = response.body();
		System.out.println(responseBody);
		
		
		JSONObject root = new JSONObject(responseBody);
		JSONArray choices = root.getJSONArray("choices");
		JSONObject firstChoice = choices.getJSONObject(0);
		JSONObject message = firstChoice.getJSONObject("message");
		String content = message.getString("content");
		
		System.out.println("CHATGPT REPLY");
		System.out.println(content);
		
		return content;
	}

	static class MyHandler implements HttpHandler
	{
		private String htmlFile;
		public MyHandler(String htmlFile)
		{
			this.htmlFile = "/mnt/c/Users/parri/Documents/Eclipse_Stuff/RAs_Programs/CS1200ProjectAPI/" + htmlFile;
		}
		public void handle(HttpExchange exchange) throws IOException {
		    System.out.println("Got Request: " + exchange.getRequestHeaders().getFirst("User-Agent") + " - " + exchange.getRemoteAddress().getAddress());

		    File file = new File(htmlFile);
		    System.out.println("Attempting to open: " + file.getAbsolutePath());
		    if (!file.exists()) {
		        System.err.println("File not found: " + htmlFile);
		        String response = "404 Webpage not found";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
		        return;
		    }

		    StringBuilder bld = new StringBuilder();
		    try (BufferedReader htmlR = new BufferedReader(new FileReader(file))) {
		        String line;
		        while ((line = htmlR.readLine()) != null) {
		            bld.append(line).append("\n");
		        }
		    }
		    catch(IOException e)
		    {
		    	e.printStackTrace();
		    }

		    String html = bld.toString();
		    exchange.sendResponseHeaders(200, html.getBytes().length);
		    try (OutputStream os = exchange.getResponseBody()) {
		        os.write(html.getBytes());
		    os.close();
		    }
		    catch(IOException e)
		    {
		    	e.printStackTrace();
		    }
		}

		
	}
	
	static abstract class APIHandler implements HttpHandler
	{

		public void handle(HttpExchange exchange) throws IOException
		{
		    
			BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
			
			StringBuilder builder = new StringBuilder();
			
			String ln = reader.readLine();
			while(ln != null)
			{
				builder.append(ln);
				builder.append("\n");
				
				ln = reader.readLine();
			}
			
			System.out.println("String to tokenize:\n" + builder.toString());
			
			System.out.println("Creating tokenizer");
			JSONTokener tokenizer = new JSONTokener(builder.toString());
			

			System.out.println("Creating JSONObject");
			JSONObject object = null;
			try
			{
				object = new JSONObject(tokenizer);
			} 
			catch (JSONException e)
			{
				e.printStackTrace();
			}
			
			
			System.out.println("Calling API handler");
			JSONObject response = handleAPICall(object);
			

			System.out.println("Creating String");
			String responseString = response.toString();

			System.out.println("Got - " + responseString);
			
			System.out.println("Response code - " + response.getInt("response"));
			
			System.out.println("Creating Response Headers");
		    exchange.sendResponseHeaders(response.getInt("response"), responseString.getBytes().length);
		    try (OutputStream os = exchange.getResponseBody()) {
		        os.write(responseString.getBytes());
		    os.close();
		    }
		    catch(IOException e)
		    {
		    	e.printStackTrace();
		    }
		}
		public abstract JSONObject handleAPICall(JSONObject JSON);
		protected JSONObject nullResponse()
		{
			JSONObject response = new JSONObject();
			response.put("response", 400);
			response.put("response-text", "Invalid JSON Request");
			System.out.println(response.toString());
			return response;
		}
	}

	
}

class User
{
	public static final long EXPIRE_WAIT_Minutes = 30;
	
	String username, email;
	String password;
	long userid;
	boolean verified;
	boolean isAdmin;
	long currentSessionToken;
	long expireDate;
	
	public User(String username, String email, String password, long userid, boolean verified, boolean isAdmin)
	{
		this.username = username;
		this.password = password;
		this.email = email;
		this.userid = userid;
		this.verified = verified;
		this.isAdmin = isAdmin;
		currentSessionToken = 0;
		expireDate = 0;
	}
	
	public long resetSessionToken()
	{
		currentSessionToken = new Random(System.nanoTime()).nextLong();
		
		final long EXPIRE_WAIT = EXPIRE_WAIT_Minutes * 60 * 1000;
		
		expireDate = System.currentTimeMillis() + EXPIRE_WAIT;
		
		return currentSessionToken;
	}
	
	public boolean checkSessionToken()
	{
		if(System.currentTimeMillis() > expireDate)
			return false;
		return true;
	}
	
	public static long getUserId(String username)
	{
		long hash = 0;
		
		int n = username.length();
		for(int i = 0; i < n; i++)
			hash += (long)(username.charAt(i) * Math.pow(63, n - i - 1));
		
		return hash;
	}
}
