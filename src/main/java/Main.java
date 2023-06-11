import utils.ConnectConfig;
import utils.DatabaseConnector;
import java.util.logging.Logger;
import javax.swing.*; //swing  componment

public class Main {
    private static final Logger log = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            DatabaseConnector connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            // Initialize LibraryManagementSystem implementation
            LibraryManagementSystemImpl libraryManagementSystem = new LibraryManagementSystemImpl(connector);
            // Set up the GUI
            SwingUtilities.invokeLater(() -> new LibraryManagementGUI(libraryManagementSystem));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
