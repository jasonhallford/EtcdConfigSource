package io.miscellanea.etcd;

import static org.assertj.core.api.Assertions.*;

import com.google.protobuf.ByteString;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test suite for {@code EtcdConfigSource}.
 */
class EtcdConfigSourceIT {
    // Constants
    private static final String TEST_KEY = "test.key";
    private static final String TEST_VALUE = "IT Value";
    private static final String CHANGED_TEST_VALUE = "Changed IT Value";

    private static String etcdUser;
    private static String etcdPassword;
    private static String etcdHost;
    private static String etcdPort;
    private static String etcdMembers;
    private static String etcdKeyPrefix;

    // Test setup
    @BeforeAll
    static void storeUserAndPassword() {
        etcdUser = System.getProperty(Constants.USER_PROP);
        etcdPassword = System.getProperty(Constants.PASSWORD_PROP);
        etcdHost = System.getProperty(Constants.HOST_PROP);
        etcdPort = System.getProperty(Constants.PORT_PROP);
        etcdMembers = System.getProperty(Constants.MEMBERS_PROP);
        etcdKeyPrefix = System.getProperty(Constants.KEY_PREFIX);
    }

    @BeforeEach
    void setDefaultProperties() throws Exception {
        this.setProperty(Constants.USER_PROP, etcdUser);
        this.setProperty(Constants.PASSWORD_PROP, etcdPassword);
        this.setProperty(Constants.HOST_PROP, etcdHost);
        this.setProperty(Constants.PORT_PROP, etcdPort);
        this.setProperty(Constants.MEMBERS_PROP, etcdMembers);
        this.setProperty(Constants.KEY_PREFIX,etcdKeyPrefix);
        this.setProperty(Constants.WATCHING_PROP, "");
    }

    // Tests
    @Test
    @DisplayName("Create ConfigSource with Custom Port")
    void createConfigSourceWithCustomPort() throws Exception {
        System.setProperty(Constants.PORT_PROP, "9999");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            assertThat(source.getConfigName()).isEqualTo("Etcd Config Source");
            assertThat(source.getHost()).isEqualTo(etcdHost);
            assertThat(source.getPort()).isEqualTo(9999);
        }
    }

    @Test
    @DisplayName("Create ConfigSource with Default Port")
    void createConfigSourceWithDefaultPort() throws Exception {
        System.setProperty("etcd.endpoint.port", "");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            assertThat(source.getConfigName()).isEqualTo("Etcd Config Source");
            assertThat(source.getHost()).isEqualTo(etcdHost);
            assertThat(source.getPort()).isEqualTo(Constants.DEFAULT_PORT);
        }
    }

    @Test
    @DisplayName("Read a Configuration Value without Username or Password")
    void readConfigurationValueWithoutUsernameOrPassword() throws Exception {
        System.setProperty(Constants.USER_PROP, "");
        System.setProperty(Constants.PASSWORD_PROP, "");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNull();
        }
    }

    @Test
    @DisplayName("Read a Configuration Value")
    void readConfigurationValue() throws Exception {
        this.setEtcdKeyValue(TEST_KEY, TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }

    @Test
    @DisplayName("Read Changed Configuration Value")
    void readChangedConfigurationValue() throws Exception {
        System.setProperty(Constants.WATCHING_PROP, "true");

        this.setEtcdKeyValue(TEST_KEY, TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            this.setEtcdKeyValue(TEST_KEY, CHANGED_TEST_VALUE);

            // Sleep for 5 seconds; watches are process asynchronously
            Thread.sleep(1000);

            value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(CHANGED_TEST_VALUE);
        }
    }

    @Test
    @DisplayName("Ignore Changed Configuration Value")
    void ignoreChangedConfigurationValue() throws Exception {
        this.setEtcdKeyValue(TEST_KEY, TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            this.setEtcdKeyValue(TEST_KEY, CHANGED_TEST_VALUE);

            // Sleep for 5 seconds; watches are process asynchronously
            Thread.sleep(1000);

            value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }

    // Utility methods
    private void setProperty(String name, String value) {
        if (name != null && value != null) {
            System.setProperty(name, value);
        }
    }

    private void setEtcdKeyValue(String key, String value) {
        try (KvStoreClient kvStoreClient = Utils.buildKvStoreClient(new EnvironmentEtcdConfig())) {
            KvClient client = kvStoreClient.getKvClient();
            client.put(ByteString.copyFromUtf8(etcdKeyPrefix+key), ByteString.copyFromUtf8(value)).sync();
        } catch (Exception e) {
            fail("Unable to set value '" + value + "' for key '" + key + "'. Reason: " + e.getMessage());
        }
    }
}
