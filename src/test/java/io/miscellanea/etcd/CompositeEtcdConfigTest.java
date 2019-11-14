package io.miscellanea.etcd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test suite for {@code CompositeEtcdConfig}.
 */
public class CompositeEtcdConfigTest {
    // Tests
    @Test
    @DisplayName("First Config Masks Second")
    void firstConfigMasksSecond() {
        EtcdConfig config1 = mock(EtcdConfig.class);
        EtcdConfig config2 = mock(EtcdConfig.class);

        when(config1.getHost()).thenReturn("config1Host");
        when(config1.getPort()).thenReturn(1000);
        when(config1.getUser()).thenReturn("config1User");
        when(config1.getPassword()).thenReturn("config1Password");
        when(config1.isWatching()).thenReturn(true);

        when(config2.getHost()).thenReturn("config2Host");
        when(config2.getPort()).thenReturn(2000);
        when(config2.getUser()).thenReturn("config2User");
        when(config2.getPassword()).thenReturn("config2Password");
        when(config2.isWatching()).thenReturn(false);

        EtcdConfig testConfig = new CompositeEtcdConfig(config1,config2);

        assertThat(testConfig.getHost()).isEqualTo("config1Host");
        assertThat(testConfig.getPort()).isEqualTo(1000);
        assertThat(testConfig.getUser()).isEqualTo("config1User");
        assertThat(testConfig.getPassword()).isEqualTo("config1Password");
        assertThat(testConfig.isWatching()).isTrue();
    }

    @Test
    @DisplayName("Second Config Overrides First")
    void secondConfigOverridesFirst() {
        EtcdConfig config1 = mock(EtcdConfig.class);
        EtcdConfig config2 = mock(EtcdConfig.class);

        when(config1.getHost()).thenReturn(null);
        when(config1.getPort()).thenReturn(null);
        when(config1.getUser()).thenReturn(null);
        when(config1.getPassword()).thenReturn(null);
        when(config1.isWatching()).thenReturn(null);

        when(config2.getHost()).thenReturn("config2Host");
        when(config2.getPort()).thenReturn(2000);
        when(config2.getUser()).thenReturn("config2User");
        when(config2.getPassword()).thenReturn("config2Password");
        when(config2.isWatching()).thenReturn(true);

        EtcdConfig testConfig = new CompositeEtcdConfig(config1,config2);

        assertThat(testConfig.getHost()).isEqualTo("config2Host");
        assertThat(testConfig.getPort()).isEqualTo(2000);
        assertThat(testConfig.getUser()).isEqualTo("config2User");
        assertThat(testConfig.getPassword()).isEqualTo("config2Password");
        assertThat(testConfig.isWatching()).isTrue();
    }
}
