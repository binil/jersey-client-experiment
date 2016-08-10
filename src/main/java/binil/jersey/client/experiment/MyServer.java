package binil.jersey.client.experiment;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;

public class MyServer {
    private static long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws Exception {
        Server server = init(9090);
        server.start();
        server.join();
    }

    public static Server init(int port) throws Exception {
        final Server server = new Server();
        Context context = new Context(server, "/", Context.SESSIONS);
        HandlerCollection handlerList = new HandlerCollection();
        server.setHandler(handlerList);

        SocketConnector socketConnector = new SocketConnector();
        socketConnector.setPort(port);
        server.setConnectors(new Connector[] {socketConnector});

        ResourceHandler resourceHandler = new ResourceHandler() {
            {
                setResourceBase("src/main/web");
                setCacheControl("max-age=0,no-cache,no-store,post-check=0,pre-check=0");
            }

            @Override
            public Resource getResource(String path) throws MalformedURLException {
                System.out.println("Fetching resource [" + path + "]");
                return super.getResource(path);
            }
        };
        context.setResourceBase("src/main/web");
        handlerList.addHandler(resourceHandler);

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                long now = System.currentTimeMillis();
                long uptime = (now - startTime)/1000;
                PrintWriter writer = null;
                try {
                    writer = resp.getWriter();
                    writer.println("Server up for " + uptime + " seconds");
                    writer.flush();
                } finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        }), "/uptime/*");


        handlerList.addHandler(context);
        System.out.println("[Server] Started web server on port [" + port + "]");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    server.stop();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return server;
    }
}

