package net.ghettopalace.etcd;

import static org.assertj.core.api.Assertions.*;

import com.google.protobuf.ByteString;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EtcdConfigSourceIT {
    // Constants
    private static final String ETCD_HOST = "localhost";
    private static final String ETCD_PORT = "4001";
    private static final String TEST_KEY = "test.key";
    private static final String TEST_VALUE = "IT Value";
    private static final String CHANGED_TEST_VALUE = "Changed IT Value";
    private static final String ETCD_ENDPOINT_HOST_PROPERTY = "etcd.endpoint.host";
    private static final String ETCD_ENDPOINT_PORT_PROPERTY = "etcd.endpoint.port";
    private static final String ETCD_USER_PROP = "etcd.endpoint.user";
    private static final String ETCD_PASSWORD_PROP = "etcd.endpoint.password";
    private static final String ETCD_USER = "root";
    private static final String ETCD_PASSWORD = "Passw0rd";
    public static final String ETCD_CS_WATCH_PROP = "etcd.cs.watch";

    // Tests
    @BeforeAll
    static void initializeTests() throws Exception {
        System.setProperty(ETCD_ENDPOINT_HOST_PROPERTY, ETCD_HOST);
        System.setProperty(ETCD_ENDPOINT_PORT_PROPERTY, ETCD_PORT);
        System.setProperty(ETCD_CS_WATCH_PROP, "");

        // Create test key(s)
        try (KvStoreClient kvStoreClient = EtcdClient.forEndpoint(ETCD_HOST, Integer.parseInt(ETCD_PORT))
                .withCredentials(ETCD_USER, ETCD_PASSWORD)
                .withPlainText()
                .build()) {

            KvClient client = kvStoreClient.getKvClient();
            client.put(ByteString.copyFromUtf8(TEST_KEY), ByteString.copyFromUtf8(TEST_VALUE)).sync();
        }
    }


    @Test
    @DisplayName("Create ConfigSource with Custom Port")
    void createConfigSourceWithCustomPort() throws Exception {
        try (EtcdConfigSource source = new EtcdConfigSource()) {
            assertThat(source.getConfigName()).isEqualTo("Etcd Config Source");
            assertThat(source.getHost()).isEqualTo(ETCD_HOST);
            assertThat(source.getPort()).isEqualTo(Integer.parseInt(ETCD_PORT));
        }
    }

    @Test
    @DisplayName("Create ConfigSource with Default Port")
    void createConfigSourceWithDefaultPort() throws Exception {
        System.setProperty("etcd.endpoint.port", "");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            assertThat(source.getConfigName()).isEqualTo("Etcd Config Source");
            assertThat(source.getHost()).isEqualTo(ETCD_HOST);
            assertThat(source.getPort()).isEqualTo(2379);
        }
    }

    @Test
    @DisplayName("Read a Configuration Value without Username or Password")
    void readConfigurationValueWithoutUsernameOrPassword() throws Exception {
        System.setProperty(ETCD_USER_PROP, "");
        System.setProperty(ETCD_PASSWORD_PROP, "");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNull();
        }
    }

    @Test
    @DisplayName("Read a Configuration Value")
    void readConfigurationValue() throws Exception {
        System.setProperty(ETCD_USER_PROP, ETCD_USER);
        System.setProperty(ETCD_PASSWORD_PROP, ETCD_PASSWORD);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }

    @Test
    @DisplayName("Read Changed Configuration Value")
    void readChangedConfigurationValue() throws Exception {
        System.setProperty(ETCD_USER_PROP, ETCD_USER);
        System.setProperty(ETCD_PASSWORD_PROP, ETCD_PASSWORD);
        System.setProperty(ETCD_CS_WATCH_PROP, "true");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            try (KvStoreClient kvStoreClient = EtcdClient.forEndpoint(ETCD_HOST, Integer.parseInt(ETCD_PORT))
                    .withCredentials(ETCD_USER, ETCD_PASSWORD)
                    .withPlainText()
                    .build()) {

                KvClient client = kvStoreClient.getKvClient();
                client.put(ByteString.copyFromUtf8(TEST_KEY), ByteString.copyFromUtf8(CHANGED_TEST_VALUE)).sync();
            }

            // Sleep for 5 seconds; watches are process asynchronously
            Thread.sleep(5000);

            value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(CHANGED_TEST_VALUE);
        }
    }

    @Test
    @DisplayName("Ignore Changed Configuration Value")
    void ignoreChangedConfigurationValue() throws Exception {
        System.setProperty(ETCD_USER_PROP, ETCD_USER);
        System.setProperty(ETCD_PASSWORD_PROP, ETCD_PASSWORD);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            try (KvStoreClient kvStoreClient = EtcdClient.forEndpoint(ETCD_HOST, Integer.parseInt(ETCD_PORT))
                    .withCredentials(ETCD_USER, ETCD_PASSWORD)
                    .withPlainText()
                    .build()) {

                KvClient client = kvStoreClient.getKvClient();
                client.put(ByteString.copyFromUtf8(TEST_KEY), ByteString.copyFromUtf8(CHANGED_TEST_VALUE)).sync();
            }

            // Sleep for 5 seconds; watches are process asynchronously
            Thread.sleep(5000);

            value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }
}
