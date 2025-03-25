package onlyoffice.sdk.manager.request;

import com.onlyoffice.manager.request.DefaultRequestManager;
import com.onlyoffice.manager.request.RequestManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.settings.HttpClientSettings;
import com.onlyoffice.utils.ConfigurationUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class RequestManagerImpl extends DefaultRequestManager {
    public RequestManagerImpl(final UrlManager urlManager, final JwtManager jwtManager,
                              final SettingsManager settingsManager) {
        super(urlManager, jwtManager, settingsManager);
    }


    @Override
    public <R> R executeGetRequest(final String url, final HttpClientSettings httpClientSettings,
                                   final RequestManager.Callback<R> callback) throws Exception {
        HttpGet httpGet = new HttpGet(url);

        String healthcheck = ConfigurationUtils.getDocsIntegrationSdkProperties()
                .getDocumentServer()
                .getHealthCheckUrl();

        if (url.contains(healthcheck)) {
            httpGet.addHeader("Upgrade", "");
        }

        return this.executeRequest(httpGet, httpClientSettings, callback);
    }

    private <R> R executeRequest(final HttpUriRequest request, final HttpClientSettings httpClientSettings,
                                 final Callback<R> callback)
            throws Exception {
        try (CloseableHttpClient httpClient = getHttpClient(httpClientSettings)) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity resEntity = response.getEntity();
                if (resEntity == null) {
                    throw new ClientProtocolException(
                            getSettingsManager().getDocsIntegrationSdkProperties().getProduct().getName()
                                    + " URL: " + request.getUri() + " did not return content.\n"
                                    + "Request: " + request.toString() + "\n"
                                    + "Response: " + response
                    );
                }

                int statusCode = response.getCode();
                if (statusCode != HttpStatus.SC_OK) {
                    throw new ClientProtocolException(
                            getSettingsManager().getDocsIntegrationSdkProperties().getProduct().getName()
                                    + " URL: " + request.getUri() + " return unexpected response.\n"
                                    + "Request: " + request.toString() + "\n"
                                    + "Response: " + response.toString()
                    );
                }

                R result = callback.doWork(resEntity);
                EntityUtils.consume(resEntity);

                return result;
            }
        }
    }

    private CloseableHttpClient getHttpClient(final HttpClientSettings httpClientSettings)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        Boolean ignoreSSLCertificate = getSettingsManager().isIgnoreSSLCertificate();

        Integer connectionTimeout = (int) TimeUnit.SECONDS.toMillis(
                getSettingsManager().getDocsIntegrationSdkProperties()
                        .getHttpClient()
                        .getConnectionTimeout()
        );

        Integer connectionRequestTimeout = (int) TimeUnit.SECONDS.toMillis(
                getSettingsManager().getDocsIntegrationSdkProperties()
                        .getHttpClient()
                        .getConnectionRequestTimeout()
        );

        Integer socketTimeout = (int) TimeUnit.SECONDS.toMillis(
                getSettingsManager().getDocsIntegrationSdkProperties()
                        .getHttpClient()
                        .getSocketTimeout()
        );

        if (httpClientSettings != null) {
            if (httpClientSettings.getConnectionTimeout() != null) {
                connectionTimeout = httpClientSettings.getConnectionTimeout();
            }

            if (httpClientSettings.getConnectionRequestTimeout() != null) {
                connectionRequestTimeout = httpClientSettings.getConnectionRequestTimeout();
            }


            if (httpClientSettings.getSocketTimeout() != null) {
                socketTimeout = httpClientSettings.getSocketTimeout();
            }

            if (httpClientSettings.getIgnoreSSLCertificate() != null) {
                ignoreSSLCertificate = httpClientSettings.getIgnoreSSLCertificate();
            }
        }


        PoolingHttpClientConnectionManagerBuilder poolingHttpClientConnectionManagerBuilder =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS)
                                .setSocketTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                                .build()
                        );

        if (ignoreSSLCertificate) {
            TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

            SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(acceptingTrustStrategy)
                    .build();
            TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);

            poolingHttpClientConnectionManagerBuilder.setTlsSocketStrategy(tlsStrategy);
        }

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(connectionRequestTimeout, TimeUnit.MILLISECONDS)
                        .build()
                )
                .setConnectionManager(poolingHttpClientConnectionManagerBuilder.build())
                .build();
    }
}
