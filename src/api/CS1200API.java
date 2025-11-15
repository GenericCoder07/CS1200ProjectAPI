package api;
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Executors;

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
import java_sql_lib_raymond.Table;
import password_hash_raymond.Hasher;
import password_hash_raymond.Password4jHasher;
import post_database.PostDatabase;
import user_database.UserAccount;
import user_database.UserAccountDatabase;

@SuppressWarnings("unused")
public class CS1200API
{
	public static Database database;
	public static HashMap<String, Table> tableNameMap = new HashMap<>();
	public static Hasher passwordHasher;
	
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
			SQLDatabase sqlDatabase = new SQLDatabase("./sqldb/mydb");
			SupabaseDatabase supabaseDatabase = new SupabaseDatabase();
			
			database = sqlDatabase;
			
			Password4jHasher password4j = new Password4jHasher();
			
			passwordHasher = password4j;
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
	
	@SuppressWarnings("resource")
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
