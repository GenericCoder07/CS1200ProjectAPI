package api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MyHandler implements HttpHandler
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
