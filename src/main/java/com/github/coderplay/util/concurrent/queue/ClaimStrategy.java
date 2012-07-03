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
 * Strategy contract for claiming the sequence of events in the
 * {@link Sequencer} by event publishers.
 * 
 * @author Min Zhou (coderplay@gmail.com)
 */
public interface ClaimStrategy extends SequenceStragegy {
  /**
   * Get the size of the data structure used to buffer events.
   * 
   * @return size of the underlying buffer.
   */
  int getBufferSize();

  /**
   * Get the current claimed sequence.
   * 
   * @return the current claimed sequence.
   */
  long getSequence();

  /**
   * Checks if there is remaining space of the data structure used to buffer
   * events.
   * 
   * @param lowerCursor
   * @return true if here is remaining space of the data structure used to
   *         buffer events
   */
  boolean hasRemaining(final Sequence lowerCursor);
}
