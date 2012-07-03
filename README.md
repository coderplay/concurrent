Concurrent
==========

An attempt to implements j.u.c whereby other alogrithms

# Getting Started

## Pre-requirement
- JDK 6 +

## Installation
    git clone https://github.com/coderplay/concurrent.git
    mvn clean package

## Examples
### FastArrayBlockingQueue
```java
    final int BUFFER_SIZE = 1024 * 8;
    final long ITERATIONS = 1000L * 1000L * 10L;
    final BlockingQueue<Long> queue =
        new FastArrayBlockingQueue<Long>(
            // producer strategy 
            new SingleThreadedClaimStrategy(BUFFER_SIZE),
            // consumer strategy
            new SingleThreadedWaitStrategy()); 
    Runnable consumer = new Runnable() {
      @Override
      public void run() {
        try {
          for (long l = 0; l < ITERATIONS; l++)
            queue.take().longValue();
        } catch (InterruptedException ie) {
        }
      }
    };

    Runnable producer = new Runnable() {
      @Override
      public void run() {
        try {
          for (long l = 0; l < ITERATIONS; l++)
            queue.put(Long.valueOf(l));
        } catch (InterruptedException ie) {
        }
      }
    };

    new Thread(consumer).start();
    new Thread(producer).start();
```

