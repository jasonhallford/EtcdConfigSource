package net.ghettopalace.etcd;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An Apache DeltaSpike <code>ConfigSource</code> implementation for etcd.
 *
 * @author Jason Hallford
 */
public class EtcdConfigSource implements ConfigSource, AutoCloseable {
    // Constants
    private static final String SOURCE_NAME = "Etcd Config Source";
    private static final String ETCD_HOST_PROP = "etcd.endpoint.host";
    private static final String ETCD_PORT_PROP = "etcd.endpoint.port";
    private static final String ETCD_USER_PROP = "etcd.endpoint.user";
    private static final String ETCD_PASSWORD_PROP = "etcd.endpoint.password";
    private static final String ORDINAL_PROP = "etcd.cs.ordinal";

    private static final int DEFAULT_ORDINAL = 1000;
    private static final int DEFAULT_PORT = 2379;

    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfigSource.class);

    private int ordinal = 0;
    private String host;
    private int port;
    private KvStoreClient kvStoreClient;
    private final Map<String, String> valueCache = new HashMap<>();

    // Constructors

    /**
     * Default constructor for use by frameworks and Java Service Loader. This
     * constructor relies on two properties to initialize the etd connection:
     * <ul>
     *     <li><strong>etcd.endpoint.host</strong> - the DNS name of a machine hosting an
     *     etcd cluster member</li>
     *     <li><strong>etc.endpoint.port</strong> - the etcd cluster member's TCP port</li>
     * </ul>
     */
    public EtcdConfigSource() {
        this.host = ConfigResolver.getPropertyValue(ETCD_HOST_PROP);
        this.port = this.resolveEtcdPort();
        String user = ConfigResolver.getPropertyValue(ETCD_USER_PROP);
        String password = ConfigResolver.getPropertyValue(ETCD_PASSWORD_PROP);
        LOGGER.debug("etcd host = {}, etcd port = {}, etcd user = {}, etcd password = {}", this.host, this.port, user, password);

        if (this.host != null && (this.port != 0)) {
            if(Strings.isNullOrEmpty(user) || Strings.isNullOrEmpty(password)) {
                LOGGER.debug("Creating etcd KV store client without credentials.");
                this.kvStoreClient = EtcdClient.forEndpoint(this.host, this.port)
                        .withPlainText()
                        .build();
            } else {
                LOGGER.debug("Creating etcd KV store client with credentials.");
                this.kvStoreClient = EtcdClient.forEndpoint(this.host, this.port)
                        .withCredentials(user,password)
                        .withPlainText()
                        .build();
            }
        } else {
            LOGGER.warn("Unable to load valid host and port configuration; config source is disabled.");
        }
    }

    /**
     * Constructor for unit testing or non-framework usage.
     *
     * @param host The name of the node hosting the etcd server.
     * @param port The etcd server's TCP port.
     * @param kvClient An intialized <code>KvStoreClient</code> instance.
     */
    public EtcdConfigSource(String host, int port, KvStoreClient kvClient) {
        if (kvClient == null) {
            throw new IllegalArgumentException("kvClient must not be null.");
        }

        if( Strings.isNullOrEmpty(host)){
            throw new IllegalArgumentException("host must not be null.");
        }

        this.host = host;
        this.port = port;
        this.kvStoreClient = kvClient;
    }

    // Properties

    /**
     * Gets the etcd server's host name.
     *
     * @return The host name.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Gets the etcd server's TCP port.
     *
     * @return The TCP port.
     */
    public int getPort() {
        return this.port;
    }

    // Private methods
    private int resolveEtcdPort() {
        String strPort = ConfigResolver.getPropertyValue(ETCD_PORT_PROP);
        int port = DEFAULT_PORT;

        if (!Strings.isNullOrEmpty(strPort)) {
            try {
                port = Integer.parseInt(strPort);
            } catch (Exception e) {
                LOGGER.warn("Unable to convert configured value to an integer ({}); using default value ({}).", e.getMessage(), port);
            }
        } else {
            LOGGER.info("Using default etcd port {}.", port);
        }

        return port;
    }

    // ConfigSource
    @Override
    public int getOrdinal() {
        if (this.ordinal == 0) {
            try {
                String strOrdinal = ConfigResolver.getPropertyValue(ORDINAL_PROP);

                if (!Strings.isNullOrEmpty(strOrdinal)) {
                    try {
                        this.ordinal = Integer.parseInt(strOrdinal);
                    } catch (Exception e) {
                        LOGGER.warn("Unable to convert ordinal '{}' to an integer; using default value {}.", strOrdinal, DEFAULT_ORDINAL);
                    }
                } else {
                    LOGGER.info("Ordinal property {} not set; using default value {}.", ORDINAL_PROP, DEFAULT_ORDINAL);
                }
            } catch (Exception e) {
                LOGGER.info("Unable to resolve ordinal property '{}'; using default value {}.", ORDINAL_PROP, DEFAULT_ORDINAL);
            }
        }

        if (this.ordinal == 0) {
            this.ordinal = DEFAULT_ORDINAL;
        }

        LOGGER.debug("Configuration source is using ordinal value '{}'.", this.ordinal);

        return this.ordinal;
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public String getPropertyValue(String key) {
        String value = null;

        if (this.kvStoreClient != null) {
            try {
                synchronized (this.valueCache) {
                    value = this.valueCache.get(key);
                }

                if (value == null) {
                    KvClient client = this.kvStoreClient.getKvClient();
                    RangeResponse response = client.get(ByteString.copyFromUtf8(key)).sync();

                    if (response.getCount() > 0) {
                        value = response.getKvs(0).getValue().toStringUtf8();

                        if (value != null) {
                            synchronized (this.valueCache) {
                                this.valueCache.put(key, value);
                            }
                        }
                    }
                } else {
                    LOGGER.debug("Read value from cache.");
                }
            } catch (Exception e) {
                LOGGER.error("Unable to retrieve value for key '" + key + "'.", e);
            }
        } else {
            LOGGER.info("Ignoring request; configuration source is disabled.");
        }

        return value;
    }

    @Override
    public String getConfigName() {
        LOGGER.debug("Configuration source name = {}", SOURCE_NAME);
        return SOURCE_NAME;
    }

    @Override
    public boolean isScannable() {
        return false;
    }

    // AutoCloseable

    /**
     * Closes the encapsulated KV store client.
     *
     * @throws IOException If the client connection cannot be closed.
     */
    @Override
    public void close() throws IOException {
        LOGGER.debug("Closing KV store client.");
        this.kvStoreClient.close();
    }
}
