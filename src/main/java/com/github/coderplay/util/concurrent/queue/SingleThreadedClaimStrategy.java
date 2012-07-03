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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Single-threaded publisher {@link ClaimStrategy}.
 * 
 * @author Min Zhou (coderplay@gmail.com)
 */
public class SingleThreadedClaimStrategy implements ClaimStrategy {

  private static final int SPIN_TRIES = 100;

  private final int bufferSize;
  private final PaddedLong minGatingSequence = new PaddedLong(
      Constants.INITIAL_CURSOR_VALUE);
  private final PaddedLong claimSequence = new PaddedLong(
      Constants.INITIAL_CURSOR_VALUE);

  /**
   * Construct a new single threaded publisher {@link ClaimStrategy} for a given
   * buffer size.
   * 
   * @param bufferSize for the underlying data structure.
   */
  public SingleThreadedClaimStrategy(final int bufferSize) {
    this.bufferSize = bufferSize;
  }

  @Override
  public int getBufferSize() {
    return bufferSize;
  }

  @Override
  public long getSequence() {
    return claimSequence.get();
  }

  @Override
  public long incrementAndGet(Sequence lowerCursor) {
    final long nextSequence = claimSequence.get() + 1L;
    claimSequence.set(nextSequence);
    waitForFreeSlotAt(nextSequence, lowerCursor);
    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence lowerCursor)
      throws InterruptedException {
    final long nextSequence = claimSequence.get() + 1L;
    claimSequence.set(nextSequence);
    if (waitForFreeSlotAt(nextSequence, lowerCursor))
      throw new InterruptedException();
    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence lowerCursor, long timeout,
      TimeUnit sourceUnit) throws InterruptedException {
    return 0;
  }

  @Override
  public void publish(long sequence, Sequence upperCursor) {
    upperCursor.set(sequence);
  }

  @Override
  public void publishInterruptibly(long sequence, Sequence upperCursor)
      throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    upperCursor.set(sequence);
  }

  @Override
  public boolean hasRemaining(Sequence lowerCursor) {
    final long wrapPoint = (claimSequence.get() + 1L) - bufferSize;
    if (wrapPoint > minGatingSequence.get()) {
      long minSequence = lowerCursor.get();
      minGatingSequence.set(minSequence);

      if (wrapPoint > minSequence) {
        return false;
      }
    }

    return true;
  }

  private boolean waitForFreeSlotAt(final long sequence, 
      Sequence lowerCursor) {
    boolean interrupted = false;

    final long wrapPoint = sequence - bufferSize;
    if (wrapPoint > minGatingSequence.get()) {
      int counter = SPIN_TRIES;
      long minSequence;
      while (wrapPoint > (minSequence = lowerCursor.get())) {
        if (Thread.interrupted()) {
          interrupted = true;
          break;
        }
        counter = applyWaitMethod(counter);
      }
      minGatingSequence.set(minSequence);
    }

    return interrupted;
  }

  private int applyWaitMethod(int counter) {
    if (0 == counter) {
      Thread.yield();
    } else {
      --counter;
    }
    return counter;
  }

}
