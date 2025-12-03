
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
	static HashMap<Integer, String> verificationCodes = new HashMap<>();
	public static Hasher passwordHasher;

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

		UserAccountDatabase.init(passwordHasher, database, tableNameMap);
		PostDatabase.init(database, tableNameMap);

		tableNameMap.forEach((name, table) -> {
			database.addTableIfAbsent(table);
		});

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

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, InterruptedException, SQLException
	{
		loadApiKey();

		ServerManager manager = new ServerManager();
		manager.setVisible(true);

		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

		server.setExecutor(Executors.newFixedThreadPool(10));
		server.createContext("/", new MyHandler("api.html"));

		createAPIContextPostCreate(server);
		
		createAPIContextAccountsSignin(server);
		createAPIContextAccountsSignout(server);
		createAPIContextAccountsLogin(server);
		
		createAPIContextAccountsResetpasswordVerify(server);
		createAPIContextAccountsResetpassword(server);
		
		createAPIContextAI(server);
		createAPIContextAICan(server);

		server.start();
		//System.out.println("Server Running v1.22.0");
		Thread.currentThread().join();
	}

	private static void createAPIContextAICan(HttpServer server)
	{
		server.createContext("/api/ai/can", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
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
		});
	}

	private static void createAPIContextAI(HttpServer server)
	{
		server.createContext("/api/ai", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
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
		});
	}

	private static void createAPIContextAccountsLogin(HttpServer server)
	{
		server.createContext("/api/accounts/login", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				String session_id = null;
				try 
				{
					session_id = JSON.getString("session-id");
				}
				catch(Exception e) {}

				String username, password_hash;
				UserAccount user;

				if(session_id == null)
				{
					username = JSON.getString("username");
					password_hash = hashPassword(JSON.getString("password"));
					user = UserAccountDatabase.getUserAccount(username);

					if(user != null && !user.password.equals(password_hash))
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

		});
	}

	private static void createAPIContextAccountsResetpassword(HttpServer server)
	{
		server.createContext("/api/accounts/reset_password", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
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

		});
	}

	private static void createAPIContextAccountsResetpasswordVerify(HttpServer server)
	{
		server.createContext("/api/accounts/reset_password/verify", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
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

		});
	}

	private static void createAPIContextAccountsSignout(HttpServer server)
	{
		server.createContext("/api/accounts/signout", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
			{
				String session_id = JSON.getString("session-id");

				UserAccount user = UserAccountDatabase.getUserAccountBySession(session_id);

				user.session_id = null;
				user.session_timestamp = 0;
				
				UserAccountDatabase.updateUser(user.username, user);

				JSONObject response = new JSONObject();
				response.put("response", 200);
				response.put("successful", true);
				response.put("response-text", "Account successfully logged out");
				response.put("account", "null");
				System.out.println(response.toString());

				return response;
			}

		});
	}

	private static void createAPIContextAccountsSignin(HttpServer server)
	{
		server.createContext("/api/accounts/signin", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
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

		});
	}

	private static void createAPIContextPostCreate(HttpServer server)
	{
		server.createContext("/api/post/create", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
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
					response.put("response-text", "Bad Session ID");

					return response;
				}

				String aiText = sendChatGPTAPIRequest("You are a helpful assistant that will get a question and output a pros and cons list of the pros and cons of agreeing to that question. You must keep your response under 200 characters", postContent);



				return response;
			}

		});
	}

	public static boolean checkAccountExists(String email)
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
			String readString = Files.readString(new File(new File("apikey.txt").getAbsolutePath().replace("/src", "")).toPath()).trim();
			if(readString.isBlank())
				throw new Exception();
			return readString;
		} 
		catch (Exception e) 
		{
			try
			{
				System.out.println("Trying file path: " + new File("apikey.txt").getAbsolutePath().replace("/src", "").replace("C:\\", "\\mn\\c\\"));
				String readString = Files.readString(new File(new File("apikey.txt").getAbsolutePath().replace("C:\\", "\\mn\\c\\")).toPath()).trim();
				if(readString.isBlank())
					throw new Exception();
				return readString;
			} 
			catch (Exception e2)
			{
				JOptionPane.showMessageDialog(null, "No OpenAI API key is detected. If you continue with no key,\n"
						+ "the server will still run, but any request to\n"
						+ "the /ai or /ai/can endpoints will return an error.\n\n"
						+ "Put a valid OpenAI api key in the file \"apikey.txt\"", 
						"No OpenAI API Key", JOptionPane.ERROR_MESSAGE);
				
				File create = new File("apikey.txt");
				try {create.createNewFile();} catch (IOException e1) {}
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
		} 
		catch (IOException e)
		{
		} 
		catch (InterruptedException e)
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
}
