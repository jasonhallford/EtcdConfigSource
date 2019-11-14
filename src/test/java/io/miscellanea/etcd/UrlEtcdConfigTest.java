package io.miscellanea.etcd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test suite for {@code UrlEtcdConfig}.
 */
public class UrlEtcdConfigTest {

    // Tests
    @Test
    @DisplayName("Config Throws Exception with a Null URL")
    void throwsWithANullUrl() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> new UrlEtcdConfig((URL)null));

        assertThat(exception.getMessage()).contains("configUrl")
                .contains("must not")
                .contains("null");
    }

    @Test
    @DisplayName("Can Read Configuration from an InputStream")
    void canReadPropertiesFromInputStream() throws IOException {
        InputStream in =
                UrlEtcdConfigTest.class.getClassLoader().getResourceAsStream("etcdconfig.properties");
        assertThat(in).isNotNull();

        EtcdConfig config = new UrlEtcdConfig(in);
        assertThat(config.getHost()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(4001);
        assertThat(config.getUser()).isEqualTo("root");
        assertThat(config.getPassword()).isEqualTo("Passw0rd");
    }
}
