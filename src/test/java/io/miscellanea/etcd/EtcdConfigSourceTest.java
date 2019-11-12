package io.miscellanea.etcd;

import com.google.protobuf.ByteString;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtcdConfigSourceTest {
    // Constants
    private static final String TEST_KEY = "this.known.property";
    private static final ByteString TEST_KEY_AS_BYTES = ByteString.copyFromUtf8(TEST_KEY);

    // Tests
    @Test
    @DisplayName("ConfigSource throws on null KvStoreClient")
    void testExceptionOnNullKvStoreClient() {
        EtcdConfiguration loader = mock(EtcdConfiguration.class);
        when(loader.getHost()).thenReturn("localhost");
        when(loader.getPort()).thenReturn(4001);

        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new EtcdConfigSource(loader,null));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getMessage()).contains("kvClient")
                .contains("must not")
                .contains("null");
    }

    @Test
    @DisplayName("ConfigSource throws on null Configuration Loader")
    void testExceptionOnNullHost() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () ->
                new EtcdConfigSource(null,mock(KvStoreClient.class)));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getMessage()).contains("configurationLoader")
                .contains("must not")
                .contains("null");
    }

    @Test
    @DisplayName("ETCD3 Get for a Known Value")
    void testKnownValue() {
        // Train the client
        KvClient.FluentRangeRequest req = mock(KvClient.FluentRangeRequest.class);
        KeyValue kv = KeyValue.newBuilder()
                .setKey(TEST_KEY_AS_BYTES)
                .setValue(ByteString.copyFromUtf8("TestValue"))
                .build();
        RangeResponse response = RangeResponse.newBuilder()
                .addKvs(kv)
                .setCount(1)
                .build();
        when( req.sync() ).thenReturn(response);

        KvClient client = mock(KvClient.class);
        when(client.get(TEST_KEY_AS_BYTES)).thenReturn(req);

        // Train the KvStore client
        KvStoreClient storeClient = mock(KvStoreClient.class);
        when(storeClient.getKvClient()).thenReturn(client);

        EtcdConfiguration loader = mock(EtcdConfiguration.class);
        when(loader.getHost()).thenReturn("localhost");
        when(loader.getPort()).thenReturn(4001);

        EtcdConfigSource configSource = new EtcdConfigSource(loader,storeClient);
        String value = configSource.getPropertyValue(TEST_KEY);

        assertThat(value).isNotNull();
        assertThat(value).isEqualTo("TestValue");
    }

    @Test
    @DisplayName("ETCD3 Get for an Unknown Value")
    void testForUnknownValue() {
        // Train the client
        KvClient.FluentRangeRequest req = mock(KvClient.FluentRangeRequest.class);
        RangeResponse response = RangeResponse.newBuilder()
                .setCount(0)
                .build();
        when( req.sync() ).thenReturn(response);

        KvClient client = mock(KvClient.class);
        when(client.get(TEST_KEY_AS_BYTES)).thenReturn(req);

        // Train the KvStore client
        KvStoreClient storeClient = mock(KvStoreClient.class);
        when(storeClient.getKvClient()).thenReturn(client);

        EtcdConfiguration loader = mock(EtcdConfiguration.class);
        when(loader.getHost()).thenReturn("localhost");
        when(loader.getPort()).thenReturn(4001);

        EtcdConfigSource configSource = new EtcdConfigSource(loader,storeClient);
        String value = configSource.getPropertyValue(TEST_KEY);

        assertThat(value).isNull();
    }
}
