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

/**
 * @deprecated
 * @author Min Zhou (coderplay@gmail.com)
 */
public class YieldingWaitStrategy implements WaitStrategy {
  private static final int SPIN_TRIES = 100;

  private final Sequence waitSequence = new Sequence(
      Constants.INITIAL_CURSOR_VALUE);
  private final ThreadLocal<MutableLong> minGatingSequenceThreadLocal =
      new ThreadLocal<MutableLong>() {
        @Override
        protected MutableLong initialValue() {
          return new MutableLong(Constants.INITIAL_CURSOR_VALUE);
        }
      };


  @Override
  public void signalAllWhenBlocking() {
    // do nothing
  }
  
  @Override
  public long incrementAndGet(Sequence upperCursor) {
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    waitForCapacity(upperCursor, minGatingSequence);

    final long nextSequence = waitSequence.incrementAndGet();
    waitForFreeSlotAt(nextSequence, upperCursor, minGatingSequence);

    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence upperCursor)
      throws InterruptedException {
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if(waitForCapacity(upperCursor, minGatingSequence))
      throw new InterruptedException();

    final long nextSequence = waitSequence.incrementAndGet();
    if(waitForFreeSlotAt(nextSequence, upperCursor, minGatingSequence))
      throw new InterruptedException();

    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence cursor, long timeout,
      TimeUnit sourceUnit) throws InterruptedException {
    return 0;
  }

  @Override
  public void publish(long sequence, Sequence lowerCursor) {
    final long expectedSequence = sequence - 1L;
    int counter = SPIN_TRIES;
    while (expectedSequence != lowerCursor.get()) {
      counter = applyWaitMethod(counter);
    }
    lowerCursor.set(sequence);
  }

  @Override
  public void publishInterruptibly(long sequence, Sequence lowerCursor)
      throws InterruptedException {
    final long expectedSequence = sequence - 1L;
    int counter = SPIN_TRIES;
    while (expectedSequence != lowerCursor.get()) {
      if(Thread.interrupted())
        throw new InterruptedException();
      counter = applyWaitMethod(counter);
    }
    lowerCursor.set(sequence); 
  }

  @Override
  public boolean isEmpty(Sequence upperCursor) {
    final long nextSequence = (waitSequence.get() + 1L);
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if (nextSequence > minGatingSequence.get()) {
      long minSequence = upperCursor.get();
      minGatingSequence.set(minSequence);

      if (nextSequence > minSequence) {
        return false;
      }
    }
    return true;
  }

  private boolean waitForCapacity(final Sequence upperCursor,
      final MutableLong maxGatingSequence) {
    boolean interrupted = false;

    final long nextSequence = (waitSequence.get() + 1L);
    if (nextSequence > maxGatingSequence.get()) {
      int counter = SPIN_TRIES;
      long maxSequence;
      while (nextSequence > (maxSequence = upperCursor.get())) {
        if (Thread.interrupted()) {
          interrupted = true;
          break;
        }
        counter = applyWaitMethod(counter);
      }
      maxGatingSequence.set(maxSequence);
    }

    return interrupted;
  }

  private boolean waitForFreeSlotAt(final long sequence,
      final Sequence upperCursor, final MutableLong minGatingSequence) {
    boolean interrupted = false;

    if (sequence > minGatingSequence.get()) {
      long maxSequence;
      int counter = SPIN_TRIES;
      while (sequence > (maxSequence = upperCursor.get())) {
        if (Thread.interrupted()) {
          interrupted = true;
          break;
        }
        counter = applyWaitMethod(counter);
      }
      minGatingSequence.set(maxSequence);
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
