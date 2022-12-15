package server;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;

public class UrlRunner implements Runnable {

    private final UrlItem urlItem;
    public final Set<UrlItem> urlItemsSet;
    private final Set<String> seenUrls;
    private final Integer depth;
    public final Set<UrlItem> results;
    public boolean isDone;
    private final ConcurrentLinkedDeque<UrlItem> urlQueue;
    private URLConnection urlConnection_;
    private CountDownLatch latch;
    private static final String A_ELEMENT = "a";

    public UrlRunner(UrlItem urlItem,
                     Set<String> seenUrls,
                     ConcurrentLinkedDeque<UrlItem> urlQueue,
                     Integer depth,
                     CountDownLatch latch) {
        this.urlItem = urlItem;
        this.urlItemsSet = new HashSet<>();
        this.seenUrls = seenUrls;
        this.results = new HashSet<>();
        this.depth = depth;
        this.urlQueue = urlQueue;
        this.isDone = false;
        this.latch = latch;
    }

    public void run() {
        connectToUrl();
        String urlContent = getUrlContent();
        Elements elements = getUrlElements(urlContent, A_ELEMENT);
        getLinks(elements);
        isDone = true;
        latch.countDown();
    }

    public void getLinks(Elements elements) {
        Integer nextDepth = urlItem.depth + 1;
        for (Element element : elements) {
            String href = element.attr("href");
            try {
                URL url = getUrl(href, urlItem.url);
                String seenUrlEntry = url.getHost() + url.getPath();
                results.add(new UrlItem(url, urlItem.depth));
                if (nextDepth <= depth && !seenUrls.contains(seenUrlEntry)) {
                    seenUrls.add(seenUrlEntry);
                    urlQueue.offer(new UrlItem(url, nextDepth));
                }
            } catch (MalformedURLException e) {
                System.err.println(e.getMessage());
                System.err.println("href: " + href);
            }
        }
    }

    public Elements getUrlElements(String stringContent, String identifier) {
        Document document = Jsoup.parse(stringContent);
        return document.getElementsByTag(identifier);
    }

    public void connectToUrl() {
        try {
            urlConnection_ = urlItem.url.openConnection();
            urlConnection_.connect();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public String getUrlContent() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(urlConnection_.getInputStream());
            BufferedReader in = new BufferedReader(inputStreamReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                stringBuilder.append(inputLine);
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return "";
        }
    }

    public static URL getUrl(String urlString, URL context) throws MalformedURLException {
        if (context == null) {
            return new URL(urlString);
        }
        return new URL(context, urlString);
    }
}
