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

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * 
 */
public final class Util {
  /**
   * Calculate the next power of 2, greater than or equal to x.
   * <p>
   * From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
   * 
   * @param x Value to round up
   * @return The next power of 2 from x inclusive
   */
  public static int ceilingNextPowerOfTwo(final int x) {
    return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
  }

  private static final Unsafe THE_UNSAFE;
  static {
    try {
      Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      THE_UNSAFE = (Unsafe) theUnsafe.get(null);
    } catch (Exception e) {
      throw new RuntimeException("Unable to load unsafe", e);
    }
  }

  public static Unsafe getUnsafe() {
    return THE_UNSAFE;
  }
}
