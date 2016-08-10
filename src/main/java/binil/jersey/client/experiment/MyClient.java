package binil.jersey.client.experiment;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MyClient {
    // At about 2 seconds per download, this should be done in 1200 seconds
    public static final int APPS_TO_DOWNLOAD = 400;

    public static final int WORKER_THREADS = 20;
    public static final int TIMEOUT_SEC = 55;
    public static final int MAX_CONN_PER_ROUTE = 6;
//    public static final int MAX_CONN_PER_ROUTE = WORKER_THREADS;

    public static void main(String[] args) {
        Client client = createClient();

        final AtomicInteger appsToComplete = new AtomicInteger(0);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                simulateOneMinuteDownload(client, appsToComplete);
            }
        }, 0, 60_000);
    }

    static void simulateOneMinuteDownload(final Client client, AtomicInteger appsToComplete) {
        System.out.println("");
        System.out.println(appsToComplete.get() + " apps did not finish download in the previous minute");
        System.out.println("-- ");
        System.out.println("Starting download for minute " + new Date());
        long startTime = System.currentTimeMillis();
        appsToComplete.set(0);

        final ProgressIndicator progressIndicator = new ProgressIndicator(WORKER_THREADS);
        final ExecutorService executorService = Executors.newFixedThreadPool(WORKER_THREADS);
        for (int i = 0; i < APPS_TO_DOWNLOAD; i++) {
            final int id = i;
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    makeWebRequest(client, id, progressIndicator);
                    if (appsToComplete.decrementAndGet() == 0) {
                        System.out.println("");
                        System.out.println("Looks like we are done for the minute by " + new Date());
                    };
                }
            };
            executorService.submit(task);
            appsToComplete.incrementAndGet();
        }

        executorService.shutdown();
        try {
            if (executorService.awaitTermination(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                long endTime = System.currentTimeMillis();
                long downloadTime = (endTime - startTime)/1000L;
                System.out.println("");
                System.out.println("Done downloading in " + downloadTime + " seconds");
            } else {
                long endTime = System.currentTimeMillis();
                long downloadTime = (endTime - startTime)/1000L;
                System.out.println("");
                System.out.println("Timed out download after " + downloadTime + " seconds");
                while (true) {
                    executorService.shutdownNow();
                    if (executorService.awaitTermination(40, TimeUnit.SECONDS)) {
                        break;
                    } else {
                        System.out.println("Timeout during terminating worker pool");
                    }
                }
            }
        } catch(InterruptedException e) {
            e.printStackTrace();
            executorService.shutdownNow();
        }
    }


    static void makeWebRequest(Client client, int id, ProgressIndicator progressIndicator) {
        WebResource webResource = client.resource("http://localhost:9090").path("/uptime").path("/" + id);

        int retryCount = 10;
        ClientResponse response = null;
        try {
            for (int i = 0; i < retryCount; i++) {
                response = webResource.getRequestBuilder().get(ClientResponse.class);
                progressIndicator.tick();
                if (response.getStatus() == ClientResponse.Status.ACCEPTED.getStatusCode()) {
                    // System.out.println(id + ": Retrying data download");
                } else if (response.getStatus() == ClientResponse.Status.NO_CONTENT.getStatusCode()) {
                    break;
                } else if (response.getStatus() >= 200 && response.getStatus() < 300) {
                    break;
                } else {
                    throw new UniformInterfaceException(response);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static Client createClient() {
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                // This client is used to download metric data, which has to happen once a minute.
                // The overall timeouts during one cycle should not be longer than the
                // cycle, so we don't skip any cycle, hence only 30 seconds timeout here.
                .setConnectTimeout(30_000)
                .setConnectionRequestTimeout(30_000)
                .setSocketTimeout(60_000)
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                .setRetryHandler(new MySleepyHttpRequestRetryHandler(1000, 5))
                .setDefaultRequestConfig(requestConfig)
                .build();

        ApacheHttpClient4Handler apacheHttpClient4Handler =
                new ApacheHttpClient4Handler(httpClient, new BasicCookieStore(), true);

        return new Client(apacheHttpClient4Handler);
    }
}

