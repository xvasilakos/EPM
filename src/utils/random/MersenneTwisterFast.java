package utils.random;

import java.io.*;
import java.util.*;


public strictfp class MersenneTwisterFast implements Serializable, Cloneable {

   // Serialization
   private static final long serialVersionUID = -8219700664442619525L;  // locked as of Version 15

   // Period parameters
   private static final int N = 624;
   private static final int M = 397;
   private static final int MATRIX_A = 0x9908b0df;   //    private static final * constant vector a
   private static final int UPPER_MASK = 0x80000000; // most significant w-r bits
   private static final int LOWER_MASK = 0x7fffffff; // least significant r bits

   // Tempering parameters
   private static final int TEMPERING_MASK_B = 0x9d2c5680;
   private static final int TEMPERING_MASK_C = 0xefc60000;

   private int mt[]; // the array for the state vector
   private int mti; // mti==N+1 means mt[N] is not initialized
   private int mag01[];

    // a good initial seed (of int size, though stored in a long)
   //private static final long GOOD_SEED = 4357;
   private double __nextNextGaussian;
   private boolean __haveNextNextGaussian;

   /* We're overriding all internal data, to my knowledge, so this should be okay */
   public Object clone() {
      try {
         MersenneTwisterFast f = (MersenneTwisterFast) (super.clone());
         f.mt = (int[]) (mt.clone());
         f.mag01 = (int[]) (mag01.clone());
         return f;
      } catch (CloneNotSupportedException e) {
         throw new InternalError();
      } // should never happen
   }

   public boolean stateEquals(Object o) {
      if (o == this) {
         return true;
      }
      if (o == null || !(o instanceof MersenneTwisterFast)) {
         return false;
      }
      MersenneTwisterFast other = (MersenneTwisterFast) o;
      if (mti != other.mti) {
         return false;
      }
      for (int x = 0; x < mag01.length; x++) {
         if (mag01[x] != other.mag01[x]) {
            return false;
         }
      }
      for (int x = 0; x < mt.length; x++) {
         if (mt[x] != other.mt[x]) {
            return false;
         }
      }
      return true;
   }

   public void readState(DataInputStream stream) throws IOException {
      int len = mt.length;
      for (int x = 0; x < len; x++) {
         mt[x] = stream.readInt();
      }

      len = mag01.length;
      for (int x = 0; x < len; x++) {
         mag01[x] = stream.readInt();
      }

      mti = stream.readInt();
      __nextNextGaussian = stream.readDouble();
      __haveNextNextGaussian = stream.readBoolean();
   }

   public void writeState(DataOutputStream stream) throws IOException {
      int len = mt.length;
      for (int x = 0; x < len; x++) {
         stream.writeInt(mt[x]);
      }

      len = mag01.length;
      for (int x = 0; x < len; x++) {
         stream.writeInt(mag01[x]);
      }

      stream.writeInt(mti);
      stream.writeDouble(__nextNextGaussian);
      stream.writeBoolean(__haveNextNextGaussian);
   }

   public MersenneTwisterFast() {
      this(System.currentTimeMillis());
   }

   public MersenneTwisterFast(long seed) {
      setSeed(seed);
   }

   public MersenneTwisterFast(int[] array) {
      setSeed(array);
   }

   synchronized public void setSeed(long seed) {
        // Due to a bug in java.util.Random clear up to 1.2, we're
      // doing our own Gaussian variable.
      __haveNextNextGaussian = false;

      mt = new int[N];

      mag01 = new int[2];
      mag01[0] = 0x0;
      mag01[1] = MATRIX_A;

      mt[0] = (int) (seed & 0xffffffff);
      for (mti = 1; mti < N; mti++) {
         mt[mti]
                 = (1812433253 * (mt[mti - 1] ^ (mt[mti - 1] >>> 30)) + mti);
         /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
         /* In the previous versions, MSBs of the seed affect   */
         /* only MSBs of the array mt[].                        */
         /* 2002/01/09 modified by Makoto Matsumoto             */
         // mt[mti] &= 0xffffffff;
            /* for >32 bit machines */
      }
   }

   synchronized public void setSeed(int[] array) {
      if (array.length == 0) {
         throw new IllegalArgumentException("Array length must be greater than zero");
      }
      int i, j, k;
      setSeed(19650218);
      i = 1;
      j = 0;
      k = (N > array.length ? N : array.length);
      for (; k != 0; k--) {
         mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1664525)) + array[j] + j; /* non linear */

         // mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
         i++;
         j++;
         if (i >= N) {
            mt[0] = mt[N - 1];
            i = 1;
         }
         if (j >= array.length) {
            j = 0;
         }
      }
      for (k = N - 1; k != 0; k--) {
         mt[i] = (mt[i] ^ ((mt[i - 1] ^ (mt[i - 1] >>> 30)) * 1566083941)) - i; /* non linear */

         // mt[i] &= 0xffffffff; /* for WORDSIZE > 32 machines */
         i++;
         if (i >= N) {
            mt[0] = mt[N - 1];
            i = 1;
         }
      }
      mt[0] = 0x80000000; /* MSB is 1; assuring non-zero initial array */

   }

   public int nextInt() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return y;
   }

   public short nextShort() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (short) (y >>> 16);
   }

   public char nextChar() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (char) (y >>> 16);
   }

   public boolean nextBoolean() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (boolean) ((y >>> 31) != 0);
   }

   public boolean nextBoolean(float probability) {
      int y;

      if (probability < 0.0f || probability > 1.0f) {
         throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
      }
      if (probability == 0.0f) {
         return false;            // fix half-open issues
      } else if (probability == 1.0f) {
         return true;        // fix half-open issues
      }
      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (y >>> 8) / ((float) (1 << 24)) < probability;
   }

   public boolean nextBoolean(double probability) {
      int y;
      int z;

      if (probability < 0.0 || probability > 1.0) {
         throw new IllegalArgumentException("probability must be between 0.0 and 1.0 inclusive.");
      }
      if (probability == 0.0) {
         return false;             // fix half-open issues
      } else if (probability == 1.0) {
         return true; // fix half-open issues
      }
      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         for (; kk < N - 1; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

         mti = 0;
      }

      z = mt[mti++];
      z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
      z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
      z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
      z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

      /* derived from nextDouble documentation in jdk 1.2 docs, see top */
      return ((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53) < probability;
   }

   public byte nextByte() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (byte) (y >>> 24);
   }

   public void nextBytes(byte[] bytes) {
      int y;

      for (int x = 0; x < bytes.length; x++) {
         if (mti >= N) // generate N words at one time
         {
            int kk;
            final int[] mt = this.mt; // locals are slightly faster 
            final int[] mag01 = this.mag01; // locals are slightly faster 

            for (kk = 0; kk < N - M; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

            mti = 0;
         }

         y = mt[mti++];
         y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
         y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
         y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
         y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

         bytes[x] = (byte) (y >>> 24);
      }
   }

   public long nextLong() {
      int y;
      int z;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         for (; kk < N - 1; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

         mti = 0;
      }

      z = mt[mti++];
      z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
      z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
      z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
      z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

      return (((long) y) << 32) + (long) z;
   }

   public long nextLong(long n) {
      if (n <= 0) {
         throw new IllegalArgumentException("n must be positive, got: " + n);
      }

      long bits, val;
      do {
         int y;
         int z;

         if (mti >= N) // generate N words at one time
         {
            int kk;
            final int[] mt = this.mt; // locals are slightly faster 
            final int[] mag01 = this.mag01; // locals are slightly faster 

            for (kk = 0; kk < N - M; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

            mti = 0;
         }

         y = mt[mti++];
         y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
         y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
         y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
         y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

         if (mti >= N) // generate N words at one time
         {
            int kk;
            final int[] mt = this.mt; // locals are slightly faster 
            final int[] mag01 = this.mag01; // locals are slightly faster 

            for (kk = 0; kk < N - M; kk++) {
               z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
            }
            for (; kk < N - 1; kk++) {
               z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
            }
            z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

            mti = 0;
         }

         z = mt[mti++];
         z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
         z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
         z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
         z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

         bits = (((((long) y) << 32) + (long) z) >>> 1);
         val = bits % n;
      } while (bits - val + (n - 1) < 0);
      return val;
   }

   public double nextDouble() {
      int y;
      int z;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         for (; kk < N - 1; kk++) {
            z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
         }
         z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

         mti = 0;
      }

      z = mt[mti++];
      z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
      z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
      z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
      z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

      /* derived from nextDouble documentation in jdk 1.2 docs, see top */
      return ((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53);
   }

   public double nextDouble(boolean includeZero, boolean includeOne) {
      double d = 0.0;
      do {
         d = nextDouble();                           // grab a value, initially from half-open [0.0, 1.0)
         if (includeOne && nextBoolean()) {
            d += 1.0;  // if includeOne, with 1/2 probability, push to [1.0, 2.0)
         }
      } while ((d > 1.0) || // everything above 1.0 is always invalid
              (!includeZero && d == 0.0));            // if we're not including zero, 0.0 is invalid
      return d;
   }

   public double nextGaussian() {
      if (__haveNextNextGaussian) {
         __haveNextNextGaussian = false;
         return __nextNextGaussian;
      } else {
         double v1, v2, s;
         do {
            int y;
            int z;
            int a;
            int b;

            if (mti >= N) // generate N words at one time
            {
               int kk;
               final int[] mt = this.mt; // locals are slightly faster 
               final int[] mag01 = this.mag01; // locals are slightly faster 

               for (kk = 0; kk < N - M; kk++) {
                  y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
               }
               for (; kk < N - 1; kk++) {
                  y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
               }
               y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
               mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

               mti = 0;
            }

            y = mt[mti++];
            y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
            y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
            y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
            y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

            if (mti >= N) // generate N words at one time
            {
               int kk;
               final int[] mt = this.mt; // locals are slightly faster 
               final int[] mag01 = this.mag01; // locals are slightly faster 

               for (kk = 0; kk < N - M; kk++) {
                  z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + M] ^ (z >>> 1) ^ mag01[z & 0x1];
               }
               for (; kk < N - 1; kk++) {
                  z = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + (M - N)] ^ (z >>> 1) ^ mag01[z & 0x1];
               }
               z = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
               mt[N - 1] = mt[M - 1] ^ (z >>> 1) ^ mag01[z & 0x1];

               mti = 0;
            }

            z = mt[mti++];
            z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
            z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
            z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
            z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

            if (mti >= N) // generate N words at one time
            {
               int kk;
               final int[] mt = this.mt; // locals are slightly faster 
               final int[] mag01 = this.mag01; // locals are slightly faster 

               for (kk = 0; kk < N - M; kk++) {
                  a = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + M] ^ (a >>> 1) ^ mag01[a & 0x1];
               }
               for (; kk < N - 1; kk++) {
                  a = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + (M - N)] ^ (a >>> 1) ^ mag01[a & 0x1];
               }
               a = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
               mt[N - 1] = mt[M - 1] ^ (a >>> 1) ^ mag01[a & 0x1];

               mti = 0;
            }

            a = mt[mti++];
            a ^= a >>> 11;                          // TEMPERING_SHIFT_U(a)
            a ^= (a << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(a)
            a ^= (a << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(a)
            a ^= (a >>> 18);                        // TEMPERING_SHIFT_L(a)

            if (mti >= N) // generate N words at one time
            {
               int kk;
               final int[] mt = this.mt; // locals are slightly faster 
               final int[] mag01 = this.mag01; // locals are slightly faster 

               for (kk = 0; kk < N - M; kk++) {
                  b = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + M] ^ (b >>> 1) ^ mag01[b & 0x1];
               }
               for (; kk < N - 1; kk++) {
                  b = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
                  mt[kk] = mt[kk + (M - N)] ^ (b >>> 1) ^ mag01[b & 0x1];
               }
               b = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
               mt[N - 1] = mt[M - 1] ^ (b >>> 1) ^ mag01[b & 0x1];

               mti = 0;
            }

            b = mt[mti++];
            b ^= b >>> 11;                          // TEMPERING_SHIFT_U(b)
            b ^= (b << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(b)
            b ^= (b << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(b)
            b ^= (b >>> 18);                        // TEMPERING_SHIFT_L(b)

            /* derived from nextDouble documentation in jdk 1.2 docs, see top */
            v1 = 2
                    * (((((long) (y >>> 6)) << 27) + (z >>> 5)) / (double) (1L << 53))
                    - 1;
            v2 = 2 * (((((long) (a >>> 6)) << 27) + (b >>> 5)) / (double) (1L << 53))
                    - 1;
            s = v1 * v1 + v2 * v2;
         } while (s >= 1 || s == 0);
         double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
         __nextNextGaussian = v2 * multiplier;
         __haveNextNextGaussian = true;
         return v1 * multiplier;
      }
   }

   public float nextFloat() {
      int y;

      if (mti >= N) // generate N words at one time
      {
         int kk;
         final int[] mt = this.mt; // locals are slightly faster 
         final int[] mag01 = this.mag01; // locals are slightly faster 

         for (kk = 0; kk < N - M; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         for (; kk < N - 1; kk++) {
            y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
            mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
         }
         y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
         mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

         mti = 0;
      }

      y = mt[mti++];
      y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
      y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
      y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
      y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

      return (y >>> 8) / ((float) (1 << 24));
   }

   public float nextFloat(boolean includeZero, boolean includeOne) {
      float d = 0.0f;
      do {
         d = nextFloat();                            // grab a value, initially from half-open [0.0f, 1.0f)
         if (includeOne && nextBoolean()) {
            d += 1.0f; // if includeOne, with 1/2 probability, push to [1.0f, 2.0f)
         }
      } while ((d > 1.0f) || // everything above 1.0f is always invalid
              (!includeZero && d == 0.0f));           // if we're not including zero, 0.0f is invalid
      return d;
   }

   public int nextInt(int n) {
      if (n <= 0) {
         throw new IllegalArgumentException("n must be positive, got: " + n);
      }

      if ((n & -n) == n) // i.e., n is a power of 2
      {
         int y;

         if (mti >= N) // generate N words at one time
         {
            int kk;
            final int[] mt = this.mt; // locals are slightly faster 
            final int[] mag01 = this.mag01; // locals are slightly faster 

            for (kk = 0; kk < N - M; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

            mti = 0;
         }

         y = mt[mti++];
         y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
         y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
         y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
         y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

         return (int) ((n * (long) (y >>> 1)) >> 31);
      }

      int bits, val;
      do {
         int y;

         if (mti >= N) // generate N words at one time
         {
            int kk;
            final int[] mt = this.mt; // locals are slightly faster 
            final int[] mag01 = this.mag01; // locals are slightly faster 

            for (kk = 0; kk < N - M; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
               y = (mt[kk] & UPPER_MASK) | (mt[kk + 1] & LOWER_MASK);
               mt[kk] = mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
            }
            y = (mt[N - 1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
            mt[N - 1] = mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

            mti = 0;
         }

         y = mt[mti++];
         y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
         y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
         y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
         y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

         bits = (y >>> 1);
         val = bits % n;
      } while (bits - val + (n - 1) < 0);
      return val;
   }

   public static void main(String args[]) {
      int j;

      MersenneTwisterFast r;

        // CORRECTNESS TEST
      // COMPARE WITH http://www.math.keio.ac.jp/matumoto/CODES/MT2002/mt19937ar.out
      r = new MersenneTwisterFast(new int[]{0x123, 0x234, 0x345, 0x456});
      System.out.println("Output of MersenneTwisterFast with new (2002/1/26) seeding mechanism");
      for (j = 0; j < 1000; j++) {
         // first, convert the int from signed to "unsigned"
         long l = (long) r.nextInt();
         if (l < 0) {
            l += 4294967296L;  // max int value
         }
         String s = String.valueOf(l);
         while (s.length() < 10) {
            s = " " + s;  // buffer
         }
         System.out.print(s + " ");
         if (j % 5 == 4) {
            System.out.println();
         }
      }

        // SPEED TEST
      final long SEED = 4357;

      int xx;
      long ms;
      System.out.println("\nTime to test grabbing 100000000 ints");

      Random rr = new Random(SEED);
      xx = 0;
      ms = System.currentTimeMillis();
      for (j = 0; j < 100000000; j++) {
         xx += rr.nextInt();
      }
      System.out.println("java.util.Random: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx);

      r = new MersenneTwisterFast(SEED);
      ms = System.currentTimeMillis();
      xx = 0;
      for (j = 0; j < 100000000; j++) {
         xx += r.nextInt();
      }
      System.out.println("Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx);

        // TEST TO COMPARE TYPE CONVERSION BETWEEN
      // MersenneTwisterFast.java AND MersenneTwister.java
      System.out.println("\nGrab the first 1000 booleans");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextBoolean() + " ");
         if (j % 8 == 7) {
            System.out.println();
         }
      }
      if (!(j % 8 == 7)) {
         System.out.println();
      }

      System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(double)");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextBoolean((double) (j / 999.0)) + " ");
         if (j % 8 == 7) {
            System.out.println();
         }
      }
      if (!(j % 8 == 7)) {
         System.out.println();
      }

      System.out.println("\nGrab 1000 booleans of increasing probability using nextBoolean(float)");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextBoolean((float) (j / 999.0f)) + " ");
         if (j % 8 == 7) {
            System.out.println();
         }
      }
      if (!(j % 8 == 7)) {
         System.out.println();
      }

      byte[] bytes = new byte[1000];
      System.out.println("\nGrab the first 1000 bytes using nextBytes");
      r = new MersenneTwisterFast(SEED);
      r.nextBytes(bytes);
      for (j = 0; j < 1000; j++) {
         System.out.print(bytes[j] + " ");
         if (j % 16 == 15) {
            System.out.println();
         }
      }
      if (!(j % 16 == 15)) {
         System.out.println();
      }

      byte b;
      System.out.println("\nGrab the first 1000 bytes -- must be same as nextBytes");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print((b = r.nextByte()) + " ");
         if (b != bytes[j]) {
            System.out.print("BAD ");
         }
         if (j % 16 == 15) {
            System.out.println();
         }
      }
      if (!(j % 16 == 15)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 shorts");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextShort() + " ");
         if (j % 8 == 7) {
            System.out.println();
         }
      }
      if (!(j % 8 == 7)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 ints");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextInt() + " ");
         if (j % 4 == 3) {
            System.out.println();
         }
      }
      if (!(j % 4 == 3)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 ints of different sizes");
      r = new MersenneTwisterFast(SEED);
      int max = 1;
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextInt(max) + " ");
         max *= 2;
         if (max <= 0) {
            max = 1;
         }
         if (j % 4 == 3) {
            System.out.println();
         }
      }
      if (!(j % 4 == 3)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 longs");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextLong() + " ");
         if (j % 3 == 2) {
            System.out.println();
         }
      }
      if (!(j % 3 == 2)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 longs of different sizes");
      r = new MersenneTwisterFast(SEED);
      long max2 = 1;
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextLong(max2) + " ");
         max2 *= 2;
         if (max2 <= 0) {
            max2 = 1;
         }
         if (j % 4 == 3) {
            System.out.println();
         }
      }
      if (!(j % 4 == 3)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 floats");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextFloat() + " ");
         if (j % 4 == 3) {
            System.out.println();
         }
      }
      if (!(j % 4 == 3)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 doubles");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextDouble() + " ");
         if (j % 3 == 2) {
            System.out.println();
         }
      }
      if (!(j % 3 == 2)) {
         System.out.println();
      }

      System.out.println("\nGrab the first 1000 gaussian doubles");
      r = new MersenneTwisterFast(SEED);
      for (j = 0; j < 1000; j++) {
         System.out.print(r.nextGaussian() + " ");
         if (j % 3 == 2) {
            System.out.println();
         }
      }
      if (!(j % 3 == 2)) {
         System.out.println();
      }

   }
}
