package benchmark;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.*;
import commands.InfoCommand;
import network.RequestWrapper;

public class BenchmarkClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.out.println("Usage: java BenchmarkClient <host> <port> <login> <hash> <requests> <threads>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String login = args[2];
        String hash = args[3];
        int requests = Integer.parseInt(args[4]);
        int threads = Integer.parseInt(args[5]);

        System.out.printf("Start Benchmark: %d reqs, %d threads, %s:%d\n", requests, threads, host, port);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(requests);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger errors = new AtomicInteger(0);
        long globalStart = System.nanoTime();

        for (int i = 0; i < requests; i++) {
            pool.submit(() -> {
                long start = System.nanoTime();
                try {
                    sendUdp(host, port, login, hash);
                    latencies.add(System.nanoTime() - start);
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long globalEnd = System.nanoTime();
        pool.shutdown();

        Collections.sort(latencies);
        double durationSec = (globalEnd - globalStart) / 1_000_000_000.0;
        double throughput = requests / durationSec;
        int size = latencies.size();
        long p50 = size > 0 ? latencies.get(size / 2) / 1_000_000 : 0;
        long p95 = size > 0 ? latencies.get((int)(size * 0.95)) / 1_000_000 : 0;
        long p99 = size > 0 ? latencies.get((int)(size * 0.99)) / 1_000_000 : 0;

        System.out.println("=== RESULTS ===");
        System.out.printf(" Time: %.2f sec\n", durationSec);
        System.out.printf("Throughput: %.0f req/s\n", throughput);
        System.out.printf("Latency p50: %d ms\n", p50);
        System.out.printf("Latency p95: %d ms\n", p95);
        System.out.printf("Latency p99: %d ms\n", p99);
        System.out.printf("Errors: %d (%.1f%%)\n", errors.get(), (errors.get() * 100.0 / requests));
        System.out.println("==================\n");
    }

    private static void sendUdp(String host, int port, String login, String hash) throws Exception {
        try (DatagramChannel ch = DatagramChannel.open()) {
            ch.configureBlocking(false);
            RequestWrapper req = new RequestWrapper(login, hash, new InfoCommand());

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(req);
                ch.send(ByteBuffer.wrap(bos.toByteArray()), new InetSocketAddress(host, port));
            }

            ByteBuffer buf = ByteBuffer.allocate(65535);
            ch.receive(buf);
        }
    }
}