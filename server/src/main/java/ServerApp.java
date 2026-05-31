import collection.CollectionManager;
import command.CommandInvoker;
import database.DatabaseManager;
import network.PoolConfig;
import network.PoolType;
import network.ServerNetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        logger.info("=== Сервер Запускается ===");
        try {
            DatabaseManager db = new DatabaseManager();
            CollectionManager cm = new CollectionManager(db.loadAllLabWorks());
            CommandInvoker invoker = new CommandInvoker(cm, db);

            PoolConfig readerConf = new PoolConfig(PoolType.NEW_THREAD, 0);
            PoolConfig procConf   = new PoolConfig(PoolType.NEW_THREAD, 0);

            logger.info("Config -> Reader: {} | Processing: {}", readerConf.type(), procConf.type());

            ServerNetworkService server = new ServerNetworkService(5001, invoker, db, readerConf, procConf);
            server.start();
        } catch (Exception e) {
            logger.error("FATAL: {}", e.getMessage(), e);
        }
    }
}