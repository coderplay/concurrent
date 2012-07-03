/**
 * 
 */
package com.github.coderplay.util.concurrent.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Basic class of multi-threaded {@link WaitStrategy} 
 * @author Min Zhou (coderplay@gmail.com)
 */
public abstract class AbstractMutlithreadWaitStrategy implements WaitStrategy {

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
  public long incrementAndGet(final Sequence upperCursor) {
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    waitForCapacity(upperCursor, minGatingSequence);

    final long nextSequence = waitSequence.incrementAndGet();
    waitForFreeSlotAt(nextSequence, upperCursor, minGatingSequence);

    return nextSequence;
  }

  @Override
  public long incrementAndGetInterruptibly(final Sequence upperCursor)
      throws InterruptedException {
    final MutableLong minGatingSequence = minGatingSequenceThreadLocal.get();
    if (waitForCapacity(upperCursor, minGatingSequence))
      throw new InterruptedException();

    final long nextSequence = waitSequence.incrementAndGet();
    if (waitForFreeSlotAt(nextSequence, upperCursor, minGatingSequence))
      throw new InterruptedException();

    return nextSequence;    
  }
  
  @Override
  public long incrementAndGetInterruptibly(Sequence upperCursor, long timeout,
      TimeUnit sourceUnit) throws InterruptedException {
    // TODO Auto-generated method stub
    return 0;
  }

  private boolean waitForCapacity(final Sequence upperCursor,
      final MutableLong maxGatingSequence) {
    boolean interrupted = false;

    final long nextSequence = (waitSequence.get() + 1L);
    if (nextSequence > maxGatingSequence.get()) {
      long maxSequence;
      while (nextSequence > (maxSequence = upperCursor.get())) {
        if (parkAndCheckInterrupt()) {
          interrupted = true;
          break;
        }
      }

      maxGatingSequence.set(maxSequence);
    }

    return interrupted;
  }

  private boolean waitForFreeSlotAt(final long sequence,
      final Sequence lowerCursor, final MutableLong minGatingSequence) {
    boolean interrupted = false;

    if (sequence > minGatingSequence.get()) {
      long minSequence;
      while (sequence > (minSequence = lowerCursor.get())) {
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

  @Override
  public void signalAllWhenBlocking() {
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
  

}
