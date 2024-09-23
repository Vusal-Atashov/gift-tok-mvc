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
        connector.setHost("0.0.0.0");  // Tüm arayüzlerde dinleme
        connector.setPort(8080);
        connector.setIdleTimeout(600000);
        server.addConnector(connector);

        // Kontekst ve ServletHolder kurulması
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // TikTokControllerServlet sınıfının Jetty sunucusuna eklenmesi
        context.addServlet(new ServletHolder(new TikTokControllerServlet()), "/api/v1/*");

        // Sunucunun başlatılması
        server.start();
        System.out.println("Sunucu http://0.0.0.0:8080 adresinde başlatıldı");
        server.join();
    }
}
