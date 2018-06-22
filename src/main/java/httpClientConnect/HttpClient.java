package main.java.httpClientConnect;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class HttpClient {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    private CloseableHttpClient client;
    private HttpPost httpPost = null;

    private List<NameValuePair> params = null;

    public HttpClient() {
        client = HttpClients
                .custom()
                .setConnectionManager(HttpClientConnectManager.getConnectionManager())
                .setConnectionManagerShared(true)
                .build();
    }

    public void setup(String requestURL) {
        httpPost = new HttpPost(requestURL);
        params = new ArrayList<>();
    }

    public void addParameter(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
    }

    public String send(boolean closeAfter) {
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);

            return null;
        }

        String content = null;
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            int status = response.getStatusLine().getStatusCode();
            HttpEntity body = response.getEntity();
            if (status == HttpURLConnection.HTTP_OK) {
                content = EntityUtils.toString(body, "utf-8");
            } else {
                LOG.error("HTTP connection, Server returned abnormal status: " + status);
                LOG.error(IOUtils.toString(body.getContent(), "utf-8"), new IllegalStateException());
            }

            if (closeAfter) {
                EntityUtils.consume(body);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        if (closeAfter) {
            try {
                client.close();
            } catch (IOException e) {}
        }

        return content;
    }
}
