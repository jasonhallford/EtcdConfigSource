package io.miscellanea.etcd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit test suite for {@code EnvironmentEtcdConfig}.
 */
public class EnvironmentEtcdConfigTest {
    // Tests
    @Test
    @DisplayName("All Values Read from System Properties")
    void allValuesReadFromSystemProperties(){
        System.setProperty(Constants.USER_PROP,"myUser");
        System.setProperty(Constants.PASSWORD_PROP,"myPassword");
        System.setProperty(Constants.HOST_PROP,"myHost");
        System.setProperty(Constants.PORT_PROP,"9999");
        System.setProperty(Constants.WATCHING_PROP,"true");
        System.setProperty(Constants.ORDINAL_PROP,"10000");
        System.setProperty(Constants.KEY_PREFIX,"service.environment.");

        EtcdConfig configuration = new EnvironmentEtcdConfig();

        assertThat(configuration.getHost()).isNotNull().isEqualTo("myHost");
        assertThat(configuration.getPort()).isEqualTo(9999);
        assertThat(configuration.getUser()).isNotNull().isEqualTo("myUser");
        assertThat(configuration.getPassword()).isNotNull().isEqualTo("myPassword");
        assertThat(configuration.isWatching()).isTrue();
        assertThat(configuration.getOrdinal()).isEqualTo(10000);
        assertThat(configuration.getKeyPrefix()).isEqualTo("service.environment.");
    }

    @Test
    @DisplayName("Key Prefix is Empty When Missing from System Properties")
    void keyPrefixIsEmptyWhenMissingFromSystemProperties(){
        System.setProperty(Constants.USER_PROP,"myUser");
        System.setProperty(Constants.PASSWORD_PROP,"myPassword");
        System.setProperty(Constants.HOST_PROP,"myHost");
        System.setProperty(Constants.PORT_PROP,"9999");
        System.setProperty(Constants.WATCHING_PROP,"true");
        System.setProperty(Constants.ORDINAL_PROP,"10000");
        System.clearProperty(Constants.KEY_PREFIX);

        EtcdConfig configuration = new EnvironmentEtcdConfig();

        assertThat(configuration.getHost()).isNotNull().isEqualTo("myHost");
        assertThat(configuration.getPort()).isEqualTo(9999);
        assertThat(configuration.getUser()).isNotNull().isEqualTo("myUser");
        assertThat(configuration.getPassword()).isNotNull().isEqualTo("myPassword");
        assertThat(configuration.isWatching()).isTrue();
        assertThat(configuration.getOrdinal()).isEqualTo(10000);
        assertThat(configuration.getKeyPrefix()).isEqualTo("");
    }

    @Test
    @DisplayName("IsWatching Can be Null and False")
    void isWatchingCanBeNullAndFalse(){
        System.setProperty(Constants.WATCHING_PROP,"");
        EtcdConfig configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isNull();

        System.setProperty(Constants.WATCHING_PROP,"false");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.WATCHING_PROP,"nonsense value");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.WATCHING_PROP,"TrUe");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isTrue();
    }
}
