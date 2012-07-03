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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A bounded {@linkplain BlockingQueue blocking queue} backed by a ring buffer.
 * Please note that the capacity of a instance of the queue (a.k.a. the buffer
 * size) should be a power of 2.
 * 
 * @author Min Zhou (coderplay@gmail.com)
 */
public class FastArrayBlockingQueue<E> extends AbstractQueue<E> implements
    BlockingQueue<E>, java.io.Serializable {

  private static final long serialVersionUID = -200258299566194572L;

  private final int indexMask;
  /** The queued items */
  private final E[] entries;

  private final ClaimStrategy claimStrategy;
  private final WaitStrategy waitStrategy;

  private Sequence upperCursor = new Sequence(Constants.INITIAL_CURSOR_VALUE);
  private Sequence lowerCursor = new Sequence(Constants.INITIAL_CURSOR_VALUE);

  /**
   * Please note that the capacity of a instance of the queue (a.k.a. the buffer
   * size ) should be a power of 2.
   * 
   * @param claimStrategy
   * @param waitStrategy
   */
  public FastArrayBlockingQueue(final ClaimStrategy claimStrategy,
      final WaitStrategy waitStrategy) {
    if (Integer.bitCount(claimStrategy.getBufferSize()) != 1) {
      throw new IllegalArgumentException("bufferSize must be a power of 2");
    }

    this.claimStrategy = claimStrategy;
    this.waitStrategy = waitStrategy;
    this.indexMask = claimStrategy.getBufferSize() - 1;
    this.entries = (E[]) new Object[claimStrategy.getBufferSize()];
  }

  @Override
  public E poll() {
    if (!waitStrategy.isEmpty(upperCursor))
      return null;
    long nextSequence = waitStrategy.incrementAndGet(upperCursor);
    E e = entries[(int) nextSequence & indexMask];
    waitStrategy.publish(nextSequence, lowerCursor);
    return e;
  }

  @Override
  public E peek() {
    throw new UnsupportedOperationException(
    		"This method is not supported yet.");
  }

  @Override
  public boolean offer(E e) {
    if (e == null)
      throw new NullPointerException();
    if (!claimStrategy.hasRemaining(lowerCursor))
      return false;
    // obtain the next sequence of the queue for publishing
    long nextSequence = claimStrategy.incrementAndGet(lowerCursor);
    // put e into the queue corresponding to the sequence
    entries[(int) nextSequence & indexMask] = e;
    // publish element e
    claimStrategy.publish(nextSequence, upperCursor);
    return true;
  }

  @Override
  public void put(E e) throws InterruptedException {
    if (e == null)
      throw new NullPointerException();
    // obtain the next sequence of the queue for publishing
    long nextSequence = claimStrategy.incrementAndGetInterruptibly(lowerCursor);
    // put e into the queue corresponding to the sequence
    entries[(int) nextSequence & indexMask] = e;
    // publish element e
    claimStrategy.publishInterruptibly(nextSequence, upperCursor);
  }

  @Override
  public boolean offer(E e, long timeout, TimeUnit unit)
      throws InterruptedException {
    throw new UnsupportedOperationException(
        "This method is not supported yet.");
  }

  @Override
  public E take() throws InterruptedException {
    // obtain the next sequence of the queue for consuming
    long nextSequence = waitStrategy.incrementAndGetInterruptibly(upperCursor);
    // fetch element e from the queue corresponding to the sequence
    E e = entries[(int) nextSequence & indexMask];
    // consume element e
    waitStrategy.publishInterruptibly(nextSequence, lowerCursor);
    return e;
  }

  @Override
  public E poll(long timeout, TimeUnit unit) throws InterruptedException {
    throw new UnsupportedOperationException(
        "This method is not supported yet.");
  }

  @Override
  public int remainingCapacity() {
    // Technically this method might return negative value
    long consumed = lowerCursor.get();
    long produced = upperCursor.get();
    return claimStrategy.getBufferSize() - (int) (produced - consumed);
  }

  @Override
  public int drainTo(Collection<? super E> c) {
    throw new UnsupportedOperationException("" +
    		"This method is not supported yet.");
  }

  @Override
  public int drainTo(Collection<? super E> c, int maxElements) {
    throw new UnsupportedOperationException("" +
    		"This method is not supported yet.");
  }

  @Override
  public Iterator<E> iterator() {
    throw new UnsupportedOperationException("" +
    		"This method is not supported yet.");
  }

  @Override
  public int size() {
    // Technically the returned size of this queue
    long consumed = lowerCursor.get();
    long produced = upperCursor.get();
    return (int) (produced - consumed);
  }

}
