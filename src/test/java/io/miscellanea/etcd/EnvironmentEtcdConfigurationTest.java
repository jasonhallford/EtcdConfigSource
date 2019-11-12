package io.miscellanea.etcd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class EnvironmentEtcdConfigurationTest {
    // Tests
    @Test
    @DisplayName("All Values Read from System Properties")
    void allValuesReadFromSystemProperties(){
        System.setProperty(Constants.ETCD_USER_PROP,"myUser");
        System.setProperty(Constants.ETCD_PASSWORD_PROP,"myPassword");
        System.setProperty(Constants.ETCD_HOST_PROP,"myHost");
        System.setProperty(Constants.ETCD_PORT_PROP,"9999");
        System.setProperty(Constants.ETCD_WATCHING_PROP,"true");

        EtcdConfiguration configuration = new EnvironmentEtcdConfiguration();

        assertThat(configuration.getHost()).isNotNull().isEqualTo("myHost");
        assertThat(configuration.getPort()).isEqualTo(9999);
        assertThat(configuration.getUser()).isNotNull().isEqualTo("myUser");
        assertThat(configuration.getPassword()).isNotNull().isEqualTo("myPassword");
        assertThat(configuration.isWatching()).isTrue();
    }

    @Test
    @DisplayName("IsWatching Can be False")
    void isWatchingCanBeFalse(){
        System.setProperty(Constants.ETCD_WATCHING_PROP,"");
        EtcdConfiguration configuration = new EnvironmentEtcdConfiguration();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"false");
        configuration = new EnvironmentEtcdConfiguration();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"nonsense value");
        configuration = new EnvironmentEtcdConfiguration();
        assertThat(configuration.isWatching()).isFalse();

        System.setProperty(Constants.ETCD_WATCHING_PROP,"TrUe");
        configuration = new EnvironmentEtcdConfiguration();
        assertThat(configuration.isWatching()).isTrue();
    }
}
