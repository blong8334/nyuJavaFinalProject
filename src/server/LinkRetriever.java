package server;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class LinkRetriever {
    private final Set<String> seenUrls;
    private final Map<String, UrlItem> results;
    private final ConcurrentLinkedDeque<UrlItem> urlQueue;
    private final String url;
    private final int depth;
    private final int threads;

    public LinkRetriever(String url, int depth, int threads) {
        seenUrls = Collections.synchronizedSet(new HashSet<>());
        urlQueue = new ConcurrentLinkedDeque<>();
        results = new HashMap<>();
        this.url = url;
        this.depth = depth;
        this.threads = threads;
    }

    public Collection<UrlItem> getResults() throws MalformedURLException {
        urlQueue.offer(new UrlItem(new URL(url), 1));
        getLinks();
        return results.values();
    }

    public void getLinks() {
        while (true) {
            boolean keepGoing = false;
            Set<UrlRunner> urlRunners = new HashSet<>();
            ExecutorService executorService = Executors.newFixedThreadPool(threads);
            int urlLength = urlQueue.size();
            CountDownLatch latch = new CountDownLatch(urlLength);
            while (urlLength-- > 0) {
                keepGoing = true;
                UrlRunner urlRunner = new UrlRunner(urlQueue.poll(), seenUrls, urlQueue, depth, latch);
                executorService.submit(urlRunner);
                urlRunners.add(urlRunner);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                executorService.shutdown();
                return;
            }
            for (UrlRunner urlRunner : urlRunners) {
                if (!urlRunner.isDone) {
                    System.out.println("url runner is not done");
                }
                for (UrlItem item : urlRunner.results) {
                    String key = item.url.getHost() + item.url.getPath();
                    results.computeIfAbsent(key, val -> item);
                }
            }
            if (!keepGoing) {
                System.out.println("Done");
                executorService.shutdown();
                break;
            }
        }
    }
}