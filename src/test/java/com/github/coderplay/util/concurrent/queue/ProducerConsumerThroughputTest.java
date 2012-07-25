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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.junit.Test;

/**
 * @author Min Zhou (coderplay@gmail.com)
 */
public final class ProducerConsumerThroughputTest {
  private static final int RUNS = 3;
  private static final int BUFFER_SIZE = 1024 * 8;
  private static final long ITERATIONS = 1024L * 1024L;

  private static final Map<String, BlockingQueue<Long>> queueMap;
  static {
    queueMap = new HashMap<String, BlockingQueue<Long>>();
    queueMap.put("ArrayBlockingQueue(unfair)", new ArrayBlockingQueue<Long>(
        BUFFER_SIZE));
//    queueMap.put("ArrayBlockingQueue(fair)", new ArrayBlockingQueue<Long>(
//        BUFFER_SIZE, true));
    queueMap.put("LinkedBlockingQueue", new LinkedBlockingQueue<Long>());
    queueMap.put("SynchronousQueue(unfair)", new SynchronousQueue<Long>());
//    queueMap.put("SynchronousQueue(fair)", new SynchronousQueue<Long>(true));
//    queueMap.put("PriorityBlockingQueue", new PriorityBlockingQueue<Long>());
  }

  
  static class Pair<L,R> {

    private final L left;
    private final R right;

    public Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }

    public L getLeft() { return left; }
    public R getRight() { return right; }

    @Override
    public int hashCode() { return left.hashCode() ^ right.hashCode(); }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
      if (o == null) return false;
      if (!(o instanceof Pair)) return false;
      Pair<L, R> p = (Pair<L, R>) o;
      return this.left.equals(p.getLeft()) &&
             this.right.equals(p.getRight());
    }

  }


  abstract static class Stage implements Runnable {
    final CyclicBarrier cyclicBarrier;
    final BlockingQueue<Long> queue;
    final long iterations;

    public Stage(final BlockingQueue<Long> blockingQueue,
        final CyclicBarrier cyclicBarrier, final long iterations) {
      this.cyclicBarrier = cyclicBarrier;
      this.queue = blockingQueue;
      this.iterations = iterations;
    }

    @Override
    public void run() {
      try {
        cyclicBarrier.await();
        operate();
        cyclicBarrier.await();
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
    
    abstract void operate() throws Exception;
  }

  static final class Producer extends Stage {

    public Producer(BlockingQueue<Long> blockingQueue,
        CyclicBarrier cyclicBarrier, long iterations) {
      super(blockingQueue, cyclicBarrier, iterations);

    }

    @Override
    void operate() throws Exception {
      for (long i = 0; i < iterations; i++) {
        queue.put(Long.valueOf(i));
      }
    }
  }

  static final class Consumer extends Stage {

    public Consumer(BlockingQueue<Long> blockingQueue,
        CyclicBarrier cyclicBarrier, long iterations) {
      super(blockingQueue, cyclicBarrier, iterations);

    }

    @Override
    void operate() throws Exception {
      for (long i = 0; i < iterations; i++) {
        queue.take();
      }
    }
  }
  
  static class BarrierTimer implements Runnable {
    private boolean started;
    private long startTime, endTime;

    @Override
    public synchronized void run() {
      long t = System.nanoTime();
      if (!started) {
        started = true;
        startTime = t;
      } else
        endTime = t;
    }

    public synchronized void clear() {
      started = false;
    }

    public synchronized long getTime() {
      return endTime - startTime;
    }
  }

  @Test
  public void compareQueues() throws Exception {
    if ("true".equalsIgnoreCase(System.getProperty(
        "com.github.coderplay.util.concurrent.runQueueTests", "true"))) {
      for (int producerThread = 1; producerThread <= 16; producerThread <<= 1) {
        for (int consumerThread = 1; consumerThread <= 16; 
            consumerThread <<= 1) {
          for (int i = 0; i < RUNS; i++) {
            System.out.format(
                "Producer threads:%d, consumer threads: %d, run %d\n",
                Integer.valueOf(producerThread),
                Integer.valueOf(consumerThread),
                Integer.valueOf(i));
            for (Entry<String, BlockingQueue<Long>> entry :
                queueMap.entrySet()) {
              runOneQueue(entry.getKey(), entry.getValue(),
                      producerThread, consumerThread);
            }

            runOneQueue(
                "FastArrayBlockingQueue",
                getFastArrayBlockingQueue(BUFFER_SIZE, producerThread,
                    consumerThread), producerThread, consumerThread);
          }
        }
      }
    }
  }
  
  FastArrayBlockingQueue<Long> getFastArrayBlockingQueue(
      int bufferSize, int producerThread, int consumerThread) throws Exception {
    return new FastArrayBlockingQueue<Long>(getClaimStrategy(producerThread,
        bufferSize), getWaitStrategy(consumerThread));
  }

  ClaimStrategy getClaimStrategy(int producerThread, int bufferSize) {
    switch (producerThread) {
    case 1:
      return new SingleThreadedClaimStrategy(bufferSize);
    case 2:
    case 4:
      return new MultiThreadedLowContentionClaimStrategy(bufferSize);
    case 8:
    case 16:
      return new MultiThreadedClaimStrategy(bufferSize);
    default:
      return null;
    }
  }
  
  WaitStrategy getWaitStrategy(int consumerThread) {
    switch (consumerThread) {
    case 1:
      return new SingleThreadedWaitStrategy();
    case 2:
    case 4:
      return new MultiThreadedLowContentionWaitStrategy();
    case 8:
    case 16:
      return new MultiThreadedWaitStrategy();
    default:
      return null;
    }
  }

  protected void runOneQueue(String queueName, BlockingQueue<Long> queue,
      int producerThread, int consumerThread) throws Exception {
    final CyclicBarrier cyclicBarrier =
        new CyclicBarrier(producerThread + consumerThread + 1);

    final Producer[] producers =
        new Producer[producerThread];
    for (int i = 0; i < producerThread; i++) {
      producers[i] =
          new Producer(queue, cyclicBarrier, ITERATIONS / producerThread);
    }

    Consumer[] consumers =
        new Consumer[consumerThread];
    for (int i = 0; i < consumerThread; i++) {
      consumers[i] =
          new Consumer(queue, cyclicBarrier, ITERATIONS / consumerThread);
    }

    final ExecutorService pool =
        Executors.newFixedThreadPool(producerThread + consumerThread);

    System.gc();

    for (int i = 0; i < producerThread; i++) {
      pool.execute(producers[i]);
    }
    for (int i = 0; i < consumerThread; i++) {
      pool.execute(consumers[i]);
    }
    cyclicBarrier.await();
    long start = System.currentTimeMillis();

    cyclicBarrier.await();
    long opsPerSecond =
        (ITERATIONS * 1000L) / (System.currentTimeMillis() - start);

    System.out.println("\tBlockingQueue=" + queueName + " " + opsPerSecond
        + " ops/sec");
  }
}
