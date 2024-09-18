import controller.TikTokControllerServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class GiftTokApp {
    public static void main(String[] args) throws Exception {
        // Serverin qurulması
        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        connector.setIdleTimeout(600000);
        server.addConnector(connector);

        // Kontekst və ServletHolder qurulması
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // TikTokControllerServlet sinifinin Jetty serverə əlavə edilməsi
        context.addServlet(new ServletHolder(new TikTokControllerServlet()), "/api/v1/*");

        // Serverin işə salınması
        server.start();
        System.out.println("Server started at http://localhost:8080");
        server.join();
    }
}
