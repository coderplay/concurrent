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
 * Basic class of multi-threaded {@link ClaimStrategy} 
 * @author Min Zhou (coderplay@gmail.com)
 */
public abstract class AbstractMultithreadedClaimStrategy implements
    ClaimStrategy {
  private final int bufferSize;
  private final Sequence claimSequence = new Sequence(
      Constants.INITIAL_CURSOR_VALUE);
  private final ThreadLocal<MutableLong> minGatingSequenceThreadLocal =
      new ThreadLocal<MutableLong>() {
        @Override
        protected MutableLong initialValue() {
          return new MutableLong(Constants.INITIAL_CURSOR_VALUE);
        }
      };

  public AbstractMultithreadedClaimStrategy(int bufferSize) {
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
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    waitForCapacity(lowerCursor, minGatingSequence);

    final long nextSequence = claimSequence.incrementAndGet();
    waitForFreeSlotAt(nextSequence, lowerCursor, minGatingSequence);

    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence lowerCursor)
      throws InterruptedException {
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if (waitForCapacity(lowerCursor, minGatingSequence))
      throw new InterruptedException();

    final long nextSequence = claimSequence.incrementAndGet();
    if (waitForFreeSlotAt(nextSequence, lowerCursor, minGatingSequence))
      throw new InterruptedException();

    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence lowerCursor, long timeout,
      TimeUnit sourceUnit) throws InterruptedException {
    // TODO: complete the timeout feature
    final long timeoutMs = sourceUnit.toMillis(timeout);
    final long startTime = System.currentTimeMillis();
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if(waitForCapacity(lowerCursor, minGatingSequence, timeout, startTime))
      throw new InterruptedException();

    final long elapsedTime = System.currentTimeMillis() - startTime;
    final long nextSequence = claimSequence.incrementAndGet();
    if (waitForFreeSlotAt(nextSequence, lowerCursor, minGatingSequence))
      throw new InterruptedException();

    return nextSequence;
  }

  @Override
  public boolean hasRemaining(final Sequence lowerCursor) {
    return hasRemaining(claimSequence.get(), lowerCursor);
  }

  /**
   * @return {@code true} if interrupted
   */
  private boolean waitForCapacity(final Sequence lowerCursor,
      final MutableLong minGatingSequence) {
    boolean interrupted = false;

    final long wrapPoint = (claimSequence.get() + 1L) - bufferSize;
    if (wrapPoint > minGatingSequence.get()) {
      long minSequence;
      while (wrapPoint > (minSequence = lowerCursor.get())) {
        if (parkAndCheckInterrupt()) {
          interrupted = true;
          break;
        }
      }

      minGatingSequence.set(minSequence);
    }

    return interrupted;
  }
  
  /**
   * @return {@code true} if interrupted
   */
  private boolean waitForCapacity(final Sequence lowerCursor,
          final MutableLong minGatingSequence, final long timeout,
          final long start) {
    boolean interrupted = false;

    final long wrapPoint = (claimSequence.get() + 1L) - bufferSize;
    if (wrapPoint > minGatingSequence.get()) {
      long minSequence;
      while (wrapPoint > (minSequence = lowerCursor.get())) {
        if (parkAndCheckInterrupt()) {
          interrupted = true;
          break;
        }

        final long elapsedTime = System.currentTimeMillis() - start;
        if (elapsedTime > timeout) {
          break;
        }
      }

      minGatingSequence.set(minSequence);
    }
 
    return interrupted;
  }

  /**
   * @return {@code true} if interrupted
   */
  private boolean waitForFreeSlotAt(final long sequence,
      final Sequence lowerCursor, final MutableLong minGatingSequence) {
    boolean interrupted = false;
    final long wrapPoint = sequence - bufferSize;

    if (wrapPoint > minGatingSequence.get()) {
      long minSequence;
      while (wrapPoint > (minSequence = lowerCursor.get())) {
        if (parkAndCheckInterrupt()) {
          interrupted = true;
          break;
        }
      }

      minGatingSequence.set(minSequence);
    }

    return interrupted;
  }

  /**
   * Convenience method to park and then check if interrupted
   * 
   * @return {@code true} if interrupted
   */
  private final boolean parkAndCheckInterrupt() {
    LockSupport.parkNanos(1L);
    return Thread.interrupted();
  }

  private boolean hasRemaining(long sequence, final Sequence lowerCursor) {
    final long wrapPoint = (sequence + 1L) - bufferSize;
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if (wrapPoint > minGatingSequence.get()) {
      long minSequence = lowerCursor.get();
      minGatingSequence.set(minSequence);

      if (wrapPoint > minSequence) {
        return false;
      }
    }
    return true;
  }
}
