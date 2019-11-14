package io.miscellanea.etcd;

import static org.assertj.core.api.Assertions.*;

import com.google.protobuf.ByteString;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test suite for {@code EtcdConfigSource}.
 */
class EtcdConfigSourceIT {
    // Constants
    private static final String ETCD_HOST = "localhost";
    private static final String ETCD_PORT = "4001";
    private static final String TEST_KEY = "test.key";
    private static final String TEST_VALUE = "IT Value";
    private static final String CHANGED_TEST_VALUE = "Changed IT Value";
    private static final String ETCD_USER = "root";
    private static final String ETCD_PASSWORD = "Passw0rd";

    // Tests
    @BeforeAll
    static void initializeTests() throws Exception {
        System.setProperty(Constants.ETCD_HOST_PROP, ETCD_HOST);
        System.setProperty(Constants.ETCD_PORT_PROP, ETCD_PORT);
        System.setProperty(Constants.ETCD_WATCHING_PROP, "");
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
        System.setProperty(Constants.ETCD_USER_PROP, "");
        System.setProperty(Constants.ETCD_PASSWORD_PROP, "");

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNull();
        }
    }

    @Test
    @DisplayName("Read a Configuration Value")
    void readConfigurationValue() throws Exception {
        System.setProperty(Constants.ETCD_USER_PROP, ETCD_USER);
        System.setProperty(Constants.ETCD_PASSWORD_PROP, ETCD_PASSWORD);

        this.setKeyValue(TEST_KEY,TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }

    @Test
    @DisplayName("Read Changed Configuration Value")
    void readChangedConfigurationValue() throws Exception {
        System.setProperty(Constants.ETCD_USER_PROP, ETCD_USER);
        System.setProperty(Constants.ETCD_PASSWORD_PROP, ETCD_PASSWORD);
        System.setProperty(Constants.ETCD_WATCHING_PROP, "true");

        this.setKeyValue(TEST_KEY,TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            this.setKeyValue(TEST_KEY,CHANGED_TEST_VALUE);

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
        System.setProperty(Constants.ETCD_USER_PROP, ETCD_USER);
        System.setProperty(Constants.ETCD_PASSWORD_PROP, ETCD_PASSWORD);

        this.setKeyValue(TEST_KEY,TEST_VALUE);

        try (EtcdConfigSource source = new EtcdConfigSource()) {
            String value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);

            this.setKeyValue(TEST_KEY,CHANGED_TEST_VALUE);

            // Sleep for 5 seconds; watches are process asynchronously
            Thread.sleep(5000);

            value = source.getPropertyValue(TEST_KEY);
            assertThat(value).isNotNull()
                    .isEqualTo(TEST_VALUE);
        }
    }

    // Utility methods
    private void setKeyValue(String key, String value){
        try (KvStoreClient kvStoreClient = EtcdClient.forEndpoint(ETCD_HOST, Integer.parseInt(ETCD_PORT))
                .withCredentials(ETCD_USER, ETCD_PASSWORD)
                .withPlainText()
                .build()) {

            KvClient client = kvStoreClient.getKvClient();
            client.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value)).sync();
        } catch (Exception e){
            fail("Unable to set value '" + value + "' for key '" + key + "'. Reason: " + e.getMessage());
        }
    }
}
