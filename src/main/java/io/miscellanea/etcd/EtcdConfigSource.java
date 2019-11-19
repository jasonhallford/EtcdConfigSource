package io.miscellanea.etcd;

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
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * An Apache DeltaSpike <code>ConfigSource</code> implementation for etcd. It
 * is configured in one of two ways:
 *
 *
 * Both methods may be combined, in which case parameters read from system properties
 * override those from the file.
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

    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdConfigSource.class);

    private final Map<String, String> valueCache = new HashMap<>();
    private final Map<ByteString, KvClient.Watch> activeWatches = new HashMap<>();
    private final WatchObserver watchObserver = new WatchObserver();

    private int ordinal = 0;
    private EtcdConfig etcdConfig = new CompositeEtcdConfig();
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
        this.kvStoreClient = Utils.buildKvStoreClient(this.etcdConfig);
    }

    /**
     * Constructor for unit testing or non-framework usage.
     *
     * @param etcdConfig  An initialized configuration loader.
     * @param kvClient An intialized <code>KvStoreClient</code> instance.
     */
    public EtcdConfigSource(EtcdConfig etcdConfig, KvStoreClient kvClient) {
        if (etcdConfig == null) {
            throw new IllegalArgumentException("configurationLoader must not be null.");
        }

        if (kvClient == null) {
            throw new IllegalArgumentException("kvClient must not be null.");
        }

        this.etcdConfig = etcdConfig;
        this.kvStoreClient = kvClient;
    }

    // Properties

    /**
     * Gets the etcd server's host name.
     *
     * @return The host name.
     */
    public String getHost() {
        return this.etcdConfig.getHost();
    }

    /**
     * Gets the etcd server's TCP port.
     *
     * @return The TCP port.
     */
    public int getPort() {
        return this.etcdConfig.getPort();
    }

    // Private methods
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
        LOGGER.debug("Removing value for key ''{}'' from cache.");

        synchronized (this.valueCache) {
            this.valueCache.remove(key);
        }
    }

    private void addWatch(KvClient client, ByteString etcdKey) {
        if (this.etcdConfig.isWatching()) {
            this.removeWatch(etcdKey);
            synchronized (this.activeWatches) {
                KvClient.Watch watch = client.watch(etcdKey).start(watchObserver);
                this.activeWatches.put(etcdKey, watch);
            }
            LOGGER.debug("Added etcd watch for key '{}'.", etcdKey.toStringUtf8());
        }
    }

    private void removeWatch(ByteString etcdKey) {
        if (this.etcdConfig.isWatching()) {
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
        return this.etcdConfig.getOrdinal();
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
        if (this.etcdConfig.isWatching() && this.activeWatches.size() > 0) {
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
            LOGGER.info("Error closing KV Store client: ", e.getMessage());
        }
    }
}
