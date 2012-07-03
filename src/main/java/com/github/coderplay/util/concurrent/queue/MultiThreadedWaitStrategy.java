/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.coderplay.util.concurrent.queue;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Min Zhou (coderplay@gmail.com)
 * 
 */
public class MultiThreadedWaitStrategy extends AbstractMutlithreadWaitStrategy {

  private static final int RETRIES = 1000;

  private final AtomicLongArray pendingPublication;
  private final int pendingMask;

  /**
   * Construct a new multi-threaded publisher {@link ClaimStrategy} for a given
   * buffer size.
   * 
   * @param bufferSize for the underlying data structure.
   * @param pendingBufferSize number of item that can be pending for
   *          serialisation
   */
  public MultiThreadedWaitStrategy(final int pendingBufferSize) {
    if (Integer.bitCount(pendingBufferSize) != 1) {
      throw new IllegalArgumentException(
          "pendingBufferSize must be a power of 2, was: " + pendingBufferSize);
    }

    this.pendingPublication = new AtomicLongArray(pendingBufferSize);
    this.pendingMask = pendingBufferSize - 1;
  }

  /**
   * Construct a new multi-threaded publisher {@link ClaimStrategy} for a given
   * buffer size.
   * 
   * @param bufferSize for the underlying data structure.
   */
  public MultiThreadedWaitStrategy() {
    this(1024);
  }

  @Override
  public void publish(long sequence, Sequence lowerCursor) {
    int counter = RETRIES;
    while (sequence - lowerCursor.get() > pendingPublication.length()) {
      if (--counter == 0) {
        if (yieldAndCheckInterrupt())
          return;
        counter = RETRIES;
      }
    }

    pendingPublication.set((int) sequence & pendingMask, sequence);

    // One of other threads has published this sequence for the current thread
    long cursorSequence = lowerCursor.get();
    if (cursorSequence >= sequence) {
      return;
    }

    long expectedSequence = Math.max(sequence - 1L, cursorSequence);
    long nextSequence = expectedSequence + 1;
    while (lowerCursor.compareAndSet(expectedSequence, nextSequence)) {
      if (Thread.interrupted())
        return;
      expectedSequence = nextSequence;
      nextSequence++;
      if (pendingPublication.get((int) nextSequence & pendingMask) 
          != nextSequence) {
        // if exceeds the capacity of pendingPublication, exit
        break;
      }
    }
  }

  @Override
  public void publishInterruptibly(long sequence, Sequence lowerCursor)
      throws InterruptedException {
    int counter = RETRIES;
    while (sequence - lowerCursor.get() > pendingPublication.length()) {
      if (--counter == 0) {
        if (yieldAndCheckInterrupt())
          throw new InterruptedException();
        counter = RETRIES;
      }
    }

    pendingPublication.set((int) sequence & pendingMask, sequence);

    // One of other threads has published this sequence for the current thread
    long cursorSequence = lowerCursor.get();
    if (cursorSequence >= sequence) {
      return;
    }

    long expectedSequence = Math.max(sequence - 1L, cursorSequence);
    long nextSequence = expectedSequence + 1;
    while (lowerCursor.compareAndSet(expectedSequence, nextSequence)) {
      if (Thread.interrupted())
        throw new InterruptedException();
      expectedSequence = nextSequence;
      nextSequence++;
      if (pendingPublication.get((int) nextSequence & pendingMask) 
          != nextSequence) {
        // if exceeds the capacity of pendingPublication, exit
        break;
      }
    }
  }

  /**
   * Convenience method to yield and then check if interrupted
   * 
   * @return {@code true} if interrupted
   */
  private final boolean yieldAndCheckInterrupt() {
    Thread.yield();
    return Thread.interrupted();
  }

}
