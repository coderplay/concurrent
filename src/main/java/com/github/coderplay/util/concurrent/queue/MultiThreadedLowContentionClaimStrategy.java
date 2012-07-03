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

/**
 * @author Min Zhou (coderplay@gmail.com)
 */
public class MultiThreadedLowContentionClaimStrategy extends
    AbstractMultithreadedClaimStrategy {

  /**
   * Construct a new multi-threaded publisher {@link ClaimStrategy} for a given
   * buffer size.
   * 
   * @param bufferSize for the underlying data structure.
   */
  public MultiThreadedLowContentionClaimStrategy(final int bufferSize) {
    super(bufferSize);
  }

  @Override
  public void publish(final long sequence, final Sequence upperCursor) {
    final long expectedSequence = sequence - 1L;
    while (expectedSequence != upperCursor.get()) {
      // busy spin
    }
    upperCursor.set(sequence);
  }

  @Override
  public void publishInterruptibly(final long sequence,
      final Sequence upperCursor) throws InterruptedException {
    final long expectedSequence = sequence - 1L;
    while (expectedSequence != upperCursor.get()) {
      if (Thread.interrupted())
        throw new InterruptedException();
      // busy spin
    }
    upperCursor.set(sequence);
  }

}
