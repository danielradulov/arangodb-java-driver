/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb.next.api.collection;

import com.arangodb.next.api.collection.entity.*;
import com.arangodb.next.api.entity.ReplicationFactor;
import com.arangodb.next.api.sync.ThreadConversation;
import com.arangodb.next.api.utils.ArangoApiTest;
import com.arangodb.next.api.utils.ArangoApiTestClass;
import com.arangodb.next.api.utils.TestContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Michele Rastelli
 */
@ArangoApiTestClass
class CollectionApiSyncTest {

    @ArangoApiTest
    void getCollectionsAndGetCollectionInfo(CollectionApiSync collectionApi) {
        Optional<SimpleCollectionEntity> graphsOpt = collectionApi
                .getCollections(CollectionsReadParams.builder().excludeSystem(false).build())
                .stream()
                .filter(c -> c.getName().equals("_graphs"))
                .findFirst();

        assertThat(graphsOpt).isPresent();
        SimpleCollectionEntity graphs = graphsOpt.get();
        assertThat(graphs.getName()).isNotNull();
        assertThat(graphs.getIsSystem()).isTrue();
        assertThat(graphs.getType()).isEqualTo(CollectionType.DOCUMENT);
        assertThat(graphs.getGloballyUniqueId()).isNotNull();

        Optional<SimpleCollectionEntity> collection = collectionApi
                .getCollections(CollectionsReadParams.builder().excludeSystem(true).build())
                .stream()
                .filter(c -> c.getName().equals("_graphs"))
                .findFirst();

        assertThat(collection).isNotPresent();

        SimpleCollectionEntity graphsInfo = collectionApi.getCollection("_graphs");
        assertThat(graphsInfo).isEqualTo(graphs);
    }

    @ArangoApiTest
    void createCollectionAndGetCollectionProperties(TestContext ctx, CollectionApiSync collectionApi) {
        CollectionCreateOptions options = CollectionCreateOptions.builder()
                .name("myCollection-" + UUID.randomUUID().toString())
                .replicationFactor(ReplicationFactor.of(2))
                .minReplicationFactor(1)
                .keyOptions(KeyOptions.builder()
                        .allowUserKeys(false)
                        .type(KeyType.UUID)
                        .build()
                )
                .waitForSync(true)
                .addShardKeys("a:")
                .numberOfShards(3)
                .isSystem(false)
                .type(CollectionType.DOCUMENT)
                .shardingStrategy(ShardingStrategy.HASH)
                .smartJoinAttribute("d")
                .cacheEnabled(true)
                .build();

        DetailedCollectionEntity createdCollection = collectionApi.createCollection(
                options,
                CollectionCreateParams.builder()
                        .enforceReplicationFactor(true)
                        .waitForSyncReplication(true)
                        .build()
        );

        assertThat(createdCollection).isNotNull();
        assertThat(createdCollection.getName()).isEqualTo(options.getName());
        assertThat(createdCollection.getKeyOptions()).isEqualTo(options.getKeyOptions());
        assertThat(createdCollection.getWaitForSync()).isEqualTo(options.getWaitForSync());
        assertThat(createdCollection.getIsSystem()).isEqualTo(options.getIsSystem());
        assertThat(createdCollection.getType()).isEqualTo(options.getType());
        assertThat(createdCollection.getGloballyUniqueId()).isNotNull();
        assertThat(createdCollection.getCacheEnabled()).isEqualTo(options.getCacheEnabled());

        if (ctx.isCluster()) {
            assertThat(createdCollection.getReplicationFactor()).isEqualTo(options.getReplicationFactor());
            assertThat(createdCollection.getMinReplicationFactor()).isEqualTo(options.getMinReplicationFactor());
            assertThat(createdCollection.getShardKeys()).isEqualTo(options.getShardKeys());
            assertThat(createdCollection.getNumberOfShards()).isEqualTo(options.getNumberOfShards());
            assertThat(createdCollection.getShardingStrategy()).isEqualTo(options.getShardingStrategy());

            if (ctx.isEnterprise()) {
                assertThat(createdCollection.getSmartJoinAttribute()).isNotNull();
                CollectionCreateOptions shardLikeOptions = CollectionCreateOptions.builder()
                        .name("shardLikeCollection-" + UUID.randomUUID().toString())
                        .distributeShardsLike(options.getName())
                        .shardKeys(options.getShardKeys())
                        .build();
                DetailedCollectionEntity shardLikeCollection = collectionApi.createCollection(shardLikeOptions);
                assertThat(shardLikeCollection).isNotNull();
                assertThat(shardLikeCollection.getDistributeShardsLike()).isEqualTo(createdCollection.getName());
            }
        }

        // readCollectionProperties
        DetailedCollectionEntity readCollectionProperties = collectionApi.getCollectionProperties(options.getName());
        assertThat(readCollectionProperties).isEqualTo(createdCollection);

        // changeCollectionProperties
        DetailedCollectionEntity changedCollectionProperties = collectionApi.changeCollectionProperties(
                options.getName(),
                CollectionChangePropertiesOptions.builder().waitForSync(!createdCollection.getWaitForSync()).build()
        );
        assertThat(changedCollectionProperties).isNotNull();
        assertThat(changedCollectionProperties.getWaitForSync()).isEqualTo(!createdCollection.getWaitForSync());
    }

    @ArangoApiTest
    void countAndDropCollection(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(
                CollectionCreateOptions.builder().name(name).build(),
                CollectionCreateParams.builder().waitForSyncReplication(true).build()
        );

        assertThat(collectionApi.existsCollection(name)).isTrue();
        assertThat(collectionApi.getCollectionCount(name)).isZero();

        try (ThreadConversation ignored = collectionApi.getConversationManager().requireConversation()) {
            collectionApi.dropCollection(name);
            assertThat(collectionApi.existsCollection(name)).isFalse();
        }
    }

    @ArangoApiTest
    void createAndDropSystemCollection(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(
                CollectionCreateOptions.builder().name(name).isSystem(true).build(),
                CollectionCreateParams.builder().waitForSyncReplication(true).build()
        );

        assertThat(collectionApi.existsCollection(name)).isTrue();

        try (ThreadConversation ignored = collectionApi.getConversationManager().requireConversation()) {
            collectionApi.dropCollection(name, CollectionDropParams.builder().isSystem(true).build());
            assertThat(collectionApi.existsCollection(name)).isFalse();
        }
    }

    @ArangoApiTest
    void renameCollection(TestContext ctx, CollectionApiSync collectionApi) {
        assumeTrue(!ctx.isCluster());

        String name = "collection-" + UUID.randomUUID().toString();

        DetailedCollectionEntity created = collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo(name);

        String newName = "collection-" + UUID.randomUUID().toString();
        SimpleCollectionEntity renamed = collectionApi.renameCollection(name, CollectionRenameOptions.builder().name(newName).build());
        assertThat(renamed).isNotNull();
        assertThat(renamed.getName()).isEqualTo(newName);
    }

    @ArangoApiTest
    void truncateCollection(CollectionApiSync collectionApi) {

        // FIXME: add some docs to the collection

        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        collectionApi.truncateCollection(name);
        Long count = collectionApi.getCollectionCount(name);
        assertThat(count).isEqualTo(0L);
    }

    @ArangoApiTest
    void getCollectionChecksum(TestContext ctx, CollectionApiSync collectionApi) {
        assumeTrue(!ctx.isCluster());

        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        CollectionChecksumEntity collectionChecksumEntity = collectionApi.getCollectionChecksum(name);
        assertThat(collectionChecksumEntity).isNotNull();
        assertThat(collectionChecksumEntity.getChecksum()).isNotNull();
        assertThat(collectionChecksumEntity.getRevision()).isNotNull();
    }

    @ArangoApiTest
    void getCollectionStatistics(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        Object collectionStatistics = collectionApi.getCollectionStatistics(name);
        System.out.println(collectionStatistics);
        assertThat(collectionStatistics).isNotNull();
    }

    @ArangoApiTest
    void loadCollection(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        collectionApi.loadCollection(name);
    }

    @ArangoApiTest
    void loadCollectionIndexes(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        collectionApi.loadCollectionIndexes(name);
    }

    @ArangoApiTest
    void recalculateCollectionCount(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        collectionApi.recalculateCollectionCount(name);
    }

    @ArangoApiTest
    void getResponsibleShard(TestContext ctx, CollectionApiSync collectionApi) {
        assumeTrue(ctx.isCluster());

        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        String responsibleShard = collectionApi.getResponsibleShard(name, Collections.singletonMap("_key", "aaa"));
        assertThat(responsibleShard).isNotNull();
    }

    @ArangoApiTest
    void getCollectionRevision(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        String revision = collectionApi.getCollectionRevision(name);
        assertThat(revision).isNotNull();
    }

    @ArangoApiTest
    void getCollectionShards(TestContext ctx, CollectionApiSync collectionApi) {
        assumeTrue(ctx.isCluster());

        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        List<String> shards = collectionApi.getCollectionShards(name);
        assertThat(shards).isNotNull();
        assertThat(shards).isNotEmpty();
    }

    @ArangoApiTest
    void unloadCollection(CollectionApiSync collectionApi) {
        String name = "collection-" + UUID.randomUUID().toString();
        collectionApi.createCollection(CollectionCreateOptions.builder().name(name).build());
        collectionApi.unloadCollection(name);
    }

}
