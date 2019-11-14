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
        System.setProperty(Constants.ETCD_USER_PROP,"myUser");
        System.setProperty(Constants.ETCD_PASSWORD_PROP,"myPassword");
        System.setProperty(Constants.ETCD_HOST_PROP,"myHost");
        System.setProperty(Constants.ETCD_PORT_PROP,"9999");
        System.setProperty(Constants.ETCD_WATCHING_PROP,"true");

        EtcdConfig configuration = new EnvironmentEtcdConfig();

        assertThat(configuration.getHost()).isNotNull().isEqualTo("myHost");
        assertThat(configuration.getPort()).isEqualTo(9999);
        assertThat(configuration.getUser()).isNotNull().isEqualTo("myUser");
        assertThat(configuration.getPassword()).isNotNull().isEqualTo("myPassword");
        assertThat(configuration.isWatching()).isTrue();
    }

    @Test
    @DisplayName("IsWatching Can be Null and False")
    void isWatchingCanBeNullAndFalse(){
        System.setProperty(Constants.ETCD_WATCHING_PROP,"");
        EtcdConfig configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isNull();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"false");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"nonsense value");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"TrUe");
        configuration = new EnvironmentEtcdConfig();
        assertThat(configuration.isWatching()).isTrue();
    }
}
