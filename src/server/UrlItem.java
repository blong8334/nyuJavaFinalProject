package server;

import java.net.URL;

public class UrlItem {
    public URL url;
    public Integer depth;
    public UrlItem(URL url, Integer depth) {
        this.url = url;
        this.depth = depth;
    }
}
