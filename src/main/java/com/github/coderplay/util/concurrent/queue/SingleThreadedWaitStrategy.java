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
 * Single-threaded consumer {@link WaitStrategy}.
 * @author Min Zhou (coderplay@gmail.com)
 */
public class SingleThreadedWaitStrategy implements WaitStrategy {

  private static final int SPIN_TRIES = 100;

  private final PaddedLong minGatingSequence = new PaddedLong(
      Constants.INITIAL_CURSOR_VALUE);
  private final PaddedLong waitSequence = new PaddedLong(
      Constants.INITIAL_CURSOR_VALUE);

  @Override
  public void signalAllWhenBlocking() {
    // do nothing
  }

  @Override
  public long incrementAndGet(final Sequence upperCursor) {
    final long nextSequence = waitSequence.get() + 1L;
    waitSequence.set(nextSequence);
    waitForFreeSlotAt(nextSequence, upperCursor);
    return nextSequence;
  }
  
  @Override
  public long incrementAndGetInterruptibly(Sequence upperCursor)
      throws InterruptedException {
    final long nextSequence = waitSequence.get() + 1L;
    waitSequence.set(nextSequence);
    if (waitForFreeSlotAt(nextSequence, upperCursor))
      throw new InterruptedException();
    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(Sequence lowerCursor, 
      long timeout, TimeUnit sourceUnit) 
          throws InterruptedException {
    // TODO Auto-generated method stub
    return 0;
  }

  private boolean waitForFreeSlotAt(final long sequence,
      final Sequence upperCursor) {
    boolean interrupted = false;

    if (sequence > minGatingSequence.get()) {
      int counter = SPIN_TRIES;
      long minSequence;
      while (sequence > (minSequence = upperCursor.get())) {
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

  @Override
  public void publish(long sequence, Sequence lowerCursor) {
    lowerCursor.set(sequence);
  }
  
  @Override
  public void publishInterruptibly(long sequence, Sequence lowerCursor)
      throws InterruptedException {
    if (Thread.interrupted())
      throw new InterruptedException();
    lowerCursor.set(sequence);
  }

  private int applyWaitMethod(int counter) {
    if (0 == counter) {
      Thread.yield();
    } else {
      --counter;
    }
    return counter;
  }

  @Override
  public boolean isEmpty(final Sequence upperCursor) {
    final long nextSequence = (waitSequence.get() + 1L);
    final MutableLong minGatingSequence = this.minGatingSequence;
    if (nextSequence > minGatingSequence.get()) {
      long minSequence = upperCursor.get();
      minGatingSequence.set(minSequence);

      if (nextSequence > minSequence) {
        return false;
      }
    }
    return true;
  }

}
