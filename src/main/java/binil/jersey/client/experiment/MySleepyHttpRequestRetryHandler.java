package binil.jersey.client.experiment;

import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;

public class MySleepyHttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
    private final int sleepBetweenRetriesMillis;

    public MySleepyHttpRequestRetryHandler(int sleepBetweenRetriesMillis, int maxRetries) {
        super(maxRetries, false, Arrays.asList(InterruptedIOException.class, SSLException.class));
        this.sleepBetweenRetriesMillis = sleepBetweenRetriesMillis;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        boolean shouldRetry = super.retryRequest(exception, executionCount, context);
        if (shouldRetry) {
            try {
                Thread.sleep(sleepBetweenRetriesMillis);
            } catch (InterruptedException e) {
                System.out.println("Interrupted while sleeping between HTTP retries");
                e.printStackTrace();
                return false;
            }
        }

        return shouldRetry;
    }
}
