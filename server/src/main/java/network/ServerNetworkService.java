package network;

import command.CommandInvoker;
import database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerNetworkService {
    private static final Logger logger = LoggerFactory.getLogger(ServerNetworkService.class);

    private final int port;
    private final CommandInvoker invoker;
    private final DatabaseManager dbManager;
    private final ExecutorService readerPool;
    private final ExecutorService processingPool;
    private final ExecutorService senderPool;

    public ServerNetworkService(int port, CommandInvoker invoker, DatabaseManager dbManager,
                                PoolConfig readerConfig, PoolConfig processingConfig) {
        this.port = port;
        this.invoker = invoker;
        this.dbManager = dbManager;

        this.readerPool = createPool(readerConfig);
        this.processingPool = createPool(processingConfig);
        this.senderPool = Executors.newCachedThreadPool();

        logger.info("Network initialized | Reader: {} | Processing: {} | Sender: Cached",
                readerConfig.type(), processingConfig.type());
    }

    private ExecutorService createPool(PoolConfig config) {
        return switch (config.type()) {
            case NEW_THREAD -> null;
            case FIXED -> Executors.newFixedThreadPool(config.size());
            case CACHED -> Executors.newCachedThreadPool();
        };
    }

    public void start() {
        logger.info("Starting UDP server on port {}", port);
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            Selector selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            logger.info("Server ready");

            while (!Thread.currentThread().isInterrupted()) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()) {
                        if (readerPool == null) new Thread(() -> readAndDispatch(channel)).start();
                        else readerPool.submit(() -> readAndDispatch(channel));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Server fatal error: {}", e.getMessage(), e);
        } finally {
            if (readerPool != null) readerPool.shutdown();
            if (processingPool != null) processingPool.shutdown();
            senderPool.shutdown();
        }
    }

    private void readAndDispatch(DatagramChannel channel) {
        InetSocketAddress clientAddress = null;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(65535);
            buffer.clear();
            clientAddress = (InetSocketAddress) channel.receive(buffer);
            if (clientAddress == null) return;

            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            RequestWrapper wrapper;
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                wrapper = (RequestWrapper) ois.readObject();
            }
            final RequestWrapper fw = wrapper;
            final DatagramChannel fc = channel;
            final InetSocketAddress fa = clientAddress;

            if (processingPool == null) new Thread(() -> processRequest(fw, fc, fa)).start();
            else processingPool.submit(() -> processRequest(fw, fc, fa));

        } catch (Exception e) {
            logger.error("Read/Dispatch error: {}", e.getMessage());
            sendError(channel, clientAddress, "Read error");
        }
    }
    private void processRequest(RequestWrapper wrapper, DatagramChannel channel, InetSocketAddress clientAddress) {
        try {
            String result;

            if (wrapper.getCommand() != null && "register".equalsIgnoreCase(wrapper.getCommand().getType())) {
                result = invoker.execute(wrapper.getCommand(), -1, wrapper.getLogin(), wrapper.getPasswordHash());
            } else {
                result = invoker.execute(wrapper.getCommand(), 0, wrapper.getLogin(), wrapper.getPasswordHash());
            }

            final String fr = result;
            senderPool.submit(() -> {
                try { sendResponse(channel, clientAddress, fr); }
                catch (IOException e) { logger.error("Send error: {}", e.getMessage()); }
            });
        } catch (Exception e) {
            logger.error("Processing error: {}", e.getMessage());
            sendError(channel, clientAddress, "Internal error: " + e.getMessage());
        }
    }

    private void sendResponse(DatagramChannel ch, InetSocketAddress addr, String msg) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(msg);
            ch.send(ByteBuffer.wrap(bos.toByteArray()), addr);
        }
    }

    private void sendError(DatagramChannel ch, InetSocketAddress addr, String msg) {
        if (addr != null) try { sendResponse(ch, addr, msg); } catch (IOException ignored) {}
    }
}