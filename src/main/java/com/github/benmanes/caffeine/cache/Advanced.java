/*
 * Copyright 2014 Ben Manes. All Rights Reserved.
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
 */
package com.github.benmanes.caffeine.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * An access point for inspecting and performing low-level operations based on the cache's runtime
 * characteristics. These operations are optional and dependent on how the cache was constructed
 * and what abilities the implementation exposes.
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
public interface Advanced<K, V> {

  /**
   * Returns access to perform operations based on the maximum size or maximum weight eviction
   * policy. If the cache was not constructed with a size-based bound or the implementation does
   * not support these operations, an empty {@link Optional} is returned.
   *
   * @return access to low-level operations for this cache if an eviction policy is used
   */
  Optional<Eviction<K, V>> eviction();

  /**
   * Returns access to perform operations based on the time-to-idle expiration policy. This policy
   * determines that an entry should be automatically removed from the cache once a fixed duration
   * has elapsed after the entry's creation, the most recent replacement of its value, or its last
   * access. Access time is reset by all cache read and write operations (including
   * {@code Cache.asMap().get(Object)} and {@code Cache.asMap().put(K, V)}), but not by operations
   * on the collection-views of {@link Cache#asMap}.
   * <p>
   * If the cache was not constructed with access-based expiration or the implementation does not
   * support these operations, an empty {@link Optional} is returned.
   *
   * @return access to low-level operations for this cache if a time-to-idle expiration policy is
   *         used
   */
  Optional<Expiration<K, V>> expireAfterRead();

  /**
   * Returns access to perform operations based on the time-to-live expiration policy. This policy
   * determines that an entry should be automatically removed from the cache once a fixed duration
   * has elapsed after the entry's creation, or the most recent replacement of its value.
   * <p>
   * If the cache was not constructed with write-based expiration or the implementation does not
   * support these operations, an empty {@link Optional} is returned.
   *
   * @return access to low-level operations for this cache if a time-to-live expiration policy is
   *         used
   */
  Optional<Expiration<K, V>> expireAfterWrite();

  /** The low-level operations for a cache with a size-based eviction policy. */
  interface Eviction<K, V> {

    /**
     * Returns whether the cache is bounded by a maximum size or maximum weight.
     *
     * @return if the size bounding takes into account the entry's weight
     */
    boolean isWeighted();

    /**
     * Returns the approximate accumulated weight of entries in this cache. If this cache does not
     * use a weighted size bound, then the {@link Optional} will be empty.
     *
     * @return the combined weight of the values in this cache
     */
    Optional<Long> weightedSize();

    /**
     * Returns the maximum total weighted or unweighted size of this cache, depending on how the
     * cache was constructed. This value can be best understood by inspecting {@link #isWeighted()}.
     *
     * @return the maximum size bounding, which may be either weighted or unweighted
     */
    long getMaximumSize();

    /**
     * Specifies the maximum total size of this cache. This value may be interpreted as the weighted
     * or unweighted threshold size based on how this cache was constructed. If the cache currently
     * exceeds the new maximum size this operation eagerly evict entries until the cache shrinks to
     * the appropriate size.
     *
     * @param maximumSize the maximum size, interpreted as weighted or unweighted depending on the
     *        whether how this cache was constructed.
     * @throws IllegalArgumentException if the maximum size specified is negative
     */
    void setMaximumSize(long maximumSize);

    /**
     * Returns an unmodifiable snapshot {@link Map} view of the cache with ordered traversal. The
     * order of iteration is from the entries least likely to be retained (coldest) to the entries
     * most likely to be retained (hottest). This order to determined by the eviction policy's best
     * guess at the time of creating this snapshot view.
     * <p>
     * Beware that obtaining the mappings is <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of the page replacement policy, determining the retention ordering
     * requires a traversal of the entries.
     *
     * @param limit the maximum size of the returned map (use {@link Integer#MAX_VALUE} to disregard
     *        the limit)
     * @return a snapshot view of the cache from coldest entry to the hottest
     */
    Map<K, V> coldest(int limit);

    /**
     * Returns an unmodifiable snapshot {@link Map} view of the cache with ordered traversal. The
     * order of iteration is from the entries most likely to be retained (hottest) to the entries
     * least likely to be retained (coldest). This order to determined by the eviction policy's best
     * guess at the time of creating this snapshot view.
     * <p>
     * Beware that obtaining the mappings is <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of the page replacement policy, determining the retention ordering
     * requires a traversal of the entries.
     *
     * @param limit the maximum size of the returned map (use {@link Integer#MAX_VALUE} to disregard
     *        the limit)
     * @return a snapshot view of the cache from hottest entry to the coldest
     */
    Map<K, V> hottest(int limit);
  }

  /** The low-level operations for a cache with a expiration policy. */
  interface Expiration<K, V> {

    /**
     * Returns the age of the entry based on the expiration policy. The entry's age is the cache's
     * estimate of the amount of time since the entry's expiration was last reset.
     * <p>
     * An expiration policy uses the age to determine if an entry is fresh or stale by comparing it
     * to the freshness lifetime. This is calculated as {@code fresh = freshnessLifetime > age}
     * where {@code freshnessLifetime = expires - currentTime}.
     *
     * @param key key with which the specified value is to be associated
     * @param unit the unit that {@code age} is expressed in
     * @return the age if the entry is present in the cache
     */
    Optional<Long> ageOf(K key, TimeUnit unit);

    /**
     * Returns the fixed duration used to determine if an entry should be automatically removed due
     * to elapsing this time bound. An entry is considered fresh if its age is less than this
     * duration, and stale otherwise. The expiration policy determines when the entry's age is
     * reset.
     *
     * @param unit the unit that duration is expressed in
     * @return the length of time after which an entry should be automatically removed
     */
    long getExpiresAfter(TimeUnit unit);

    /**
     * Specifies that each entry should be automatically removed from the cache once a fixed
     * duration has elapsed based. The expiration policy determines when the entry's age is reset.
     *
     * @param duration the length of time after which an entry should be automatically removed
     * @param unit the unit that {@code duration} is expressed in
     * @throws IllegalArgumentException if {@code duration} is negative
     */
    void setExpiresAfter(long duration, TimeUnit unit);

    /**
     * Returns an unmodifiable snapshot {@link Map} view of the cache with ordered traversal. The
     * order of iteration is from the entries most likely to expire (oldest) to the entries least
     * likely to expire (youngest). This order to determined by the expiration policy's best guess
     * at the time of creating this snapshot view.
     * <p>
     * Beware that obtaining the mappings is <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of the page replacement policy, determining the retention ordering
     * requires a traversal of the entries.
     *
     * @param limit the maximum size of the returned map (use {@link Integer#MAX_VALUE} to disregard
     *        the limit)
     * @return a snapshot view of the cache from oldest entry to the youngest
     */
    Map<K, V> oldest(int limit);

    /**
     * Returns an unmodifiable snapshot {@link Map} view of the cache with ordered traversal. The
     * order of iteration is from the entries least likely to expire (youngest) to the entries most
     * likely to expire (oldest). This order to determined by the expiration policy's best guess at
     * the time of creating this snapshot view.
     * <p>
     * Beware that obtaining the mappings is <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of the page replacement policy, determining the retention ordering
     * requires a traversal of the entries.
     *
     * @param limit the maximum size of the returned map (use {@link Integer#MAX_VALUE} to disregard
     *        the limit)
     * @return a snapshot view of the cache from youngest entry to the oldest
     */
    Map<K, V> youngest(int limit);
  }
}
