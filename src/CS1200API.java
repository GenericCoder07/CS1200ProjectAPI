import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

import org.json.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class CS1200API
{

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, InterruptedException
	{
		
		Arrays.toString(new int[]{9});
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		
		server.setExecutor(Executors.newFixedThreadPool(10));
		
		server.createContext("/", new HttpHandler() {

			public void handle(HttpExchange exchange) throws IOException
			{
				String response = "This is an undefined endpoint\ntry going to **/api/about";
				exchange.sendResponseHeaders(404, response.length());
				OutputStream os = exchange.getResponseBody();
				os.write(response.getBytes());
				os.close();
			}
			
		});
		server.createContext("/api/about", new MyHandler("api.html"));
		server.createContext("/api/account/register", new APIHandler() {

			public JSONObject handleAPICall(JSONObject JSON)
			{
				JSONObject response = new JSONObject();
				
				response.put("response", 200);
				
				return response;
			}
			
		});
		server.createContext("/api/account/login", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				JSONObject response = new JSONObject();
				
				response.put("response", 200);
				
				return response;
			}
			
		});
		server.createContext("/api/accounts/verify", new APIHandler(){

			public JSONObject handleAPICall(JSONObject JSON)
			{
				JSONObject response = new JSONObject();
				
				response.put("response", 200);
				
				return response;
			}
			
		});
		server.start();
		System.out.println("Server Running");
		Thread.currentThread().join();
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
			
			JSONTokener tokenizer = new JSONTokener(builder.toString());
			
			JSONObject response = handleAPICall(new JSONObject(tokenizer));
			
			
			String responseString = response.toString();
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
	}

	
}

class UserDatabase 
{
    private final File dbFile;
    private final Map<String, User> users = new HashMap<>();

    public UserDatabase(String filename) throws IOException 
    {
        this.dbFile = new File(filename);
        if (dbFile.exists()) load();
        else dbFile.createNewFile();
    }

    // Add or update a user
    public void addUser(String username, String email, String password, boolean isAdmin) throws IOException 
    {
        
        users.put(username, new User(username, email, password, User.getUserId(username), false, isAdmin));
        save();
    }

    // Find user by username
    public User findUser(String username) 
    {
        return users.get(username);
    }

    // Load JSON-like data from file
    private void load() throws IOException 
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(dbFile))) 
        {
            String line;
            while ((line = reader.readLine()) != null) 
            {
                User user = parseJsonLine(line.trim());
                users.put(user.username, user);
            }
        }
    }

    private void save() throws IOException 
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFile,false))) 
        {
            for (User u : users.values()) 
            {
                writer.write(toJsonLine(u));
                writer.newLine();
            }
        }
    }

    private String toJsonLine(User user) 
    {
        return String.format("{username:%s,username:%s,password:%s,userid:%d,verified:%b,isadmin:%b}",
                user.username,
                user.email,
                user.password,
                user.userid,
                user.verified,
                user.isAdmin);
    }

    // Minimal parser for our simple JSON format
    public static User parseJsonLine(String line) 
    {
        Map<String,String> map = new HashMap<>();
        // Remove braces
        line = line.substring(1,line.length()-1); // strip {}
        String[] pairs = line.split("\",\"");
        for (String pair : pairs) {
            String[] kv = pair.replace("\"","").split(":",2);
            map.put(kv[0], kv[1]);
        }
        
        User user = new User(map.get("username"), map.get("email"), map.get("password"), Long.parseLong(map.get("userid")), Boolean.parseBoolean(map.get("verified")), Boolean.parseBoolean(map.get("isadmin")));
        
        return user;
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
