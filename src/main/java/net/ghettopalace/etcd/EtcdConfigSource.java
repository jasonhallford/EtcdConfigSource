package net.ghettopalace.etcd;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import io.grpc.stub.StreamObserver;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * An Apache DeltaSpike <code>ConfigSource</code> implementation for etcd.
 *
 * @author Jason Hallford
 */
public class EtcdConfigSource implements ConfigSource, AutoCloseable {
    // Inner classes

    /**
     * A class for managing asynchronous watch updates from etcd.
     */
    class WatchObserver implements StreamObserver<WatchUpdate> {
        // Constructors
        public WatchObserver() {

        }

        // StreamObserver
        @Override
        public void onNext(WatchUpdate watchUpdate) {
            if (watchUpdate.getEvents() != null) {
                LOGGER.debug("Processing watch updates...");

                for (Event evt : watchUpdate.getEvents()) {
                    KeyValue kv = evt.getKv();
                    String key = kv.getKey().toStringUtf8();

                    LOGGER.debug("Processing event for key '{}'.", key);
                    removeCachedValue(key);
                }

                LOGGER.debug("Watch updates processed.");
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOGGER.error("WatchObserver received as error: ", throwable);
        }

        @Override
        public void onCompleted() {
            LOGGER.debug("onCompleted() invoked for WatchObserver.");
        }
    }

    // Constants
    private static final String SOURCE_NAME = "Etcd Config Source";
    private static final String ETCD_HOST_PROP = "etcd.endpoint.host";
    private static final String ETCD_PORT_PROP = "etcd.endpoint.port";
    private static final String ETCD_USER_PROP = "etcd.endpoint.user";
    private static final String ETCD_PASSWORD_PROP = "etcd.endpoint.password";
    private static final String ORDINAL_PROP = "etcd.cs.ordinal";
    private static final String WATCH_PROPS_PROP = "etcd.cs.watch";

    private static final int DEFAULT_ORDINAL = 1000;
    private static final int DEFAULT_PORT = 2379;

    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfigSource.class);

    private final Map<String, String> valueCache = new HashMap<>();
    private final Map<ByteString, KvClient.Watch> activeWatches = new HashMap<>();
    private final WatchObserver watchObserver = new WatchObserver();

    private int ordinal = 0;
    private String host;
    private int port;
    private boolean watch;
    private KvStoreClient kvStoreClient;

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
        this.watch = this.resolveWatchProperty();

        String user = ConfigResolver.getPropertyValue(ETCD_USER_PROP);
        String password = ConfigResolver.getPropertyValue(ETCD_PASSWORD_PROP);
        LOGGER.debug("etcd host = {}, etcd port = {}, etcd user = {}, etcd password = {}", this.host, this.port, user, password);

        if (this.host != null && (this.port != 0)) {
            if (Strings.isNullOrEmpty(user) || Strings.isNullOrEmpty(password)) {
                LOGGER.debug("Creating etcd KV store client without credentials.");
                this.kvStoreClient = EtcdClient.forEndpoint(this.host, this.port)
                        .withPlainText()
                        .build();
            } else {
                LOGGER.debug("Creating etcd KV store client with credentials.");
                this.kvStoreClient = EtcdClient.forEndpoint(this.host, this.port)
                        .withCredentials(user, password)
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
     * @param host     The name of the node hosting the etcd server.
     * @param port     The etcd server's TCP port.
     * @param kvClient An intialized <code>KvStoreClient</code> instance.
     */
    public EtcdConfigSource(String host, int port, KvStoreClient kvClient) {
        if (kvClient == null) {
            throw new IllegalArgumentException("kvClient must not be null.");
        }

        if (Strings.isNullOrEmpty(host)) {
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

    /**
     * Resolves the etcd server's TCP port using DeltaSpike's configuration
     * mechanism. To set the port, define the <code>etcd.endpoint.port</code>
     * property and set it to an integer value. If not set, the value defaults
     * to <code>2379</code>.
     *
     * @return The server's TCP port.
     */
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

    private boolean resolveWatchProperty(){
        boolean doWatch = false;

        String strWatch = ConfigResolver.getPropertyValue(WATCH_PROPS_PROP);
        if (!Strings.isNullOrEmpty(strWatch)) {
            doWatch = Boolean.parseBoolean(strWatch);
        }

        LOGGER.info("Config source {} watch for property changes.",doWatch ? "will" : "will not");

        return doWatch;
    }

    private void cacheValue(String key, String value) {
        synchronized (this.valueCache) {
            this.valueCache.put(key, value);
        }
    }

    private String readCachedValue(String key) {
        String value = null;

        synchronized (this.valueCache) {
            value = this.valueCache.get(key);
        }

        if (value != null && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Read value '{}' for key '{}' from cache.", value, key);
        }

        return value;
    }

    private void removeCachedValue(String key) {
        LOGGER.debug("Removing value for key '{}' from cache.");

        synchronized (this.valueCache) {
            this.valueCache.remove(key);
        }
    }

    private void addWatch(KvClient client, ByteString etcdKey) {
        if( this.watch) {
            this.removeWatch(etcdKey);
            synchronized (this.activeWatches) {
                KvClient.Watch watch = client.watch(etcdKey).start(watchObserver);
                this.activeWatches.put(etcdKey, watch);
            }
            LOGGER.debug("Added etcd watch for key '{}'.", etcdKey.toStringUtf8());
        }
    }

    private void removeWatch(ByteString etcdKey) {
        if( this.watch ) {
            synchronized (this.activeWatches) {
                if (this.activeWatches.containsKey(etcdKey)) {
                    LOGGER.debug("Closing current watch for '{}' and removing from map.", etcdKey.toStringUtf8());
                    KvClient.Watch active = this.activeWatches.get(etcdKey);
                    this.activeWatches.remove(etcdKey);
                    active.close();
                }
            }
        }
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
                value = this.readCachedValue(key);
                if (value == null) {
                    KvClient client = this.kvStoreClient.getKvClient();
                    ByteString etcdKey = ByteString.copyFromUtf8(key);

                    RangeResponse response = client.get(etcdKey).sync();

                    if (response.getCount() > 0) {
                        value = response.getKvs(0).getValue().toStringUtf8();

                        if (value != null) {
                            this.cacheValue(key, value);
                            this.addWatch(client, etcdKey);
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
        if (this.watch && this.activeWatches.size() > 0) {
            LOGGER.debug("Closing all active watches.");
            for (KvClient.Watch watch : this.activeWatches.values()) {
                try {
                    watch.close();
                } catch (Exception e) {
                    LOGGER.info("Error closing watch stream: {}", e.getMessage());
                }
            }
        }

        LOGGER.debug("Closing KV store client.");
        try {
            this.kvStoreClient.close();
        } catch (Exception e) {
            LOGGER.info("Error closing KV Store client: ",e.getMessage());
        }
    }
}
