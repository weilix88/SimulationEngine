package test.java.api;

import java.io.*;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.http.*;

import redis.clients.jedis.Jedis;

import javax.servlet.annotation.*;

@WebServlet("/test") 
public class Test extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
 
        // Set the response MIME type of the response message
        response.setContentType("text/html");
        // Allocate a output writer to write the response message into the network socket
        PrintWriter out = response.getWriter();
 
        // Write the response message, in an HTML page
        try {
        out.println("<html>");
        out.println("<head><title>Test Page</title></head>");
        out.println("<body>");
        out.println("<h1>Test Page</h1>");  // says Hello
        // Echo client's request information
        out.println("<p>Request URI: " + request.getRequestURI() + "</p>");
        out.println("<p>Protocol: " + request.getProtocol() + "</p>");
        out.println("<p>PathInfo: " + request.getPathInfo() + "</p>");
        out.println("<p>Remote Address: " + request.getRemoteAddr() + "</p>");
        // Generate a random number upon each request
        out.println("<p>A Random Number: <strong>" + Math.random() + "</strong></p>");
        out.println("</body></html>");
        } finally {
            out.close();  // Always close the output writer
        }
    }
    
    public static void main(String[] args){
        /*try(Jedis jedis = new Jedis("localhost")) {
            jedis.set("TaskQueue#status", "test_str");
            System.out.println("Write to Redis finished");
            jedis.quit();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        try(Jedis jedis = new Jedis("localhost")){
            Set<String> keys = jedis.keys("TaskQueue");
            for(String key : keys){
                jedis.del(key);
            }
            jedis.flushAll();
        }
        try(Jedis jedis = new Jedis("localhost")){
            String read = jedis.get("TaskQueue#status");
            System.out.println(read);
            jedis.quit();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try(Jedis jedis = new Jedis("localhost")){
            String read = jedis.get("TaskQueue#status");
            System.out.println(read);
            jedis.quit();
        }
    }
}

