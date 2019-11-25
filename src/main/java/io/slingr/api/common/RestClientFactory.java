package io.slingr.api.common;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.net.ssl.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * <p>Factory of secure REST clients
 * <p>
 * Created by lefunes on 14/06/16.
 */
public class RestClientFactory {
    private static final Logger logger = Logger.getLogger(RestClientFactory.class);

    private final static int DEFAULT_MAX_CONNECTIONS = 50;

    public static class RestClientOptions {
        public boolean allowSelfSignedCertificate = false;
        public boolean supressHttpComplianceValidation = true;
        public boolean followRedirects = true;
        public int maxConnections = DEFAULT_MAX_CONNECTIONS;
        public boolean allowMultipart = true;
        public boolean sharedConnectionManager = true;
    }

    public static Client configureClient() {
        return configureClient(new RestClientOptions());
    }

    public static Client configureClient(RestClientOptions options)  {
        try {
            // pool connection manager
            final PoolingHttpClientConnectionManager connectionManager;
            if (options.allowSelfSignedCertificate) {
                final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", configureSSL())
                        .build();
                connectionManager = new PoolingHttpClientConnectionManager(registry);
            } else {
                connectionManager = new PoolingHttpClientConnectionManager();
            }
            connectionManager.setMaxTotal(options.maxConnections);
            connectionManager.setDefaultMaxPerRoute(options.maxConnections);
            ApacheConnectorProvider connector = new ApacheConnectorProvider();

            // client config
            final ClientConfig clientConfig = new ClientConfig();
            if (options.allowMultipart) {
                clientConfig.register(MultiPartFeature.class);
            }
            clientConfig.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, options.supressHttpComplianceValidation);
            clientConfig.property(ClientProperties.FOLLOW_REDIRECTS, options.followRedirects);
            clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
            clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER_SHARED, options.sharedConnectionManager);
            clientConfig.connectorProvider(connector);

            // create client
            return ClientBuilder.newBuilder()
                    .withConfig(clientConfig)
                    .build();
        } catch (Exception e) {
            logger.error("Could not create secure rest client", e);
            throw new RuntimeException("Could not create secure rest client", e);
        }
    }

    private static SSLConnectionSocketFactory configureSSL() throws KeyManagementException, NoSuchAlgorithmException {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        return new SSLConnectionSocketFactory(sslContext, new TrustAllHostNameVerifier());
    }

    private static class TrustAllHostNameVerifier implements HostnameVerifier {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }


}
