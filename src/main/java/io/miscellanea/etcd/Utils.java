package io.miscellanea.etcd;

import com.google.common.base.Strings;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of utility functions. This class should not be instantiated.
 *
 * @author Jason Hallford
 */
class Utils {
    // Fields
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    // Constructors
    /**
     * Ensures this class can't be instantiated.
     */
    private Utils(){

    }

    // Utility methods

    /**
     * Parses a comma-separated list of etcd cluster members into
     * a {@code java.util.List}.
     *
     * @param members A comma-separated list of cluster members.
     * @return The tokenized member list.
     */
    public static List<String> parseMembers(String members){
        List<String> memberList = new ArrayList<>();

        if(!Strings.isNullOrEmpty(members)){
            String[] splitMembers = members.split(",");
            for(String member : splitMembers){
                memberList.add(member);
                LOGGER.debug("Adding member {} to list.",member);
            }
        }

        return memberList;
    }

    /**
     * Builds a {@code KvStoreClient} instance based on values provided
     * by {@code config}.
     *
     * @param config The etcd configuration.
     * @return A configured {@code KvStoreClient} instance.
     */
    public static KvStoreClient buildKvStoreClient(EtcdConfig config){
        KvStoreClient client = null;

        if (config.getHost() != null || (config.getClusterMembers().size() > 0 )) {
            EtcdClient.Builder builder = null;

            if( config.getClusterMembers() != null && config.getClusterMembers().size() > 0){
                builder = EtcdClient.forEndpoints(config.getClusterMembers());
            } else {
                builder = EtcdClient.forEndpoint(config.getHost(), config.getPort());
            }

            if (!Strings.isNullOrEmpty(config.getUser()) &&
                    !Strings.isNullOrEmpty(config.getPassword())) {
                LOGGER.debug("Creating etcd KV store client with credentials.");
                builder = builder.withCredentials(config.getUser(), config.getPassword());
            }

            client = builder.withPlainText().build();
        } else {
            LOGGER.warn("Unable to load valid host and port configuration; config source is disabled.");
        }

        return client;
    }
}
