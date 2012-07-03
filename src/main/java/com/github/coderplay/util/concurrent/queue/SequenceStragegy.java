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
 * Stragegy contract for operating {@link Sequence}s.
 * @author Min Zhou (coderplay@gmail.com)
 */
public interface SequenceStragegy {

  /**
   * Claim the next sequence. The caller should be held up until the claimed
   * sequence is available by tracking the gating {@link Sequence}.
   * 
   * @param lowerCursor
   * @return
   */
  long incrementAndGet(final Sequence cursor);

  /**
   * Interruptible version of {@link #incrementAndGet(Sequence)} for claiming
   * the next sequence. The caller should be held up until the claimed sequence
   * is available by tracking the gating {@link Sequence}.
   */
  long incrementAndGetInterruptibly(final Sequence cursor)
      throws InterruptedException;
  
  /**
   * Claims the next sequence if it is within the given waiting time and the
   * current thread has not been {@linkplain Thread#interrupt interrupted}.
   * 
   * @param lowerCursor
   * @param timeout
   * @param sourceUnit
   * @return
   * @throws InterruptedException
   */
  long incrementAndGetInterruptibly(final Sequence cursor, final long timeout,
      final TimeUnit sourceUnit) throws InterruptedException;

  /**
   * Serialize publishers in sequence and set cursor to latest available
   * sequence.
   */
  void publish(final long sequence, final Sequence cursor);

  /**
   * Interruptible version of {@link #incrementAndGet(Sequence)} for serializing
   * publishers in sequence and set cursor to latest available sequence.
   * 
   * @param sequence sequence to be applied
   * @param lowerCursor to serialise against.
   */
  void publishInterruptibly(final long sequence, final Sequence cursor)
      throws InterruptedException;
}
