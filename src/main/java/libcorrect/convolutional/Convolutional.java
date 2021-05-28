/*
 * libcorrect4j
 * Convolutional.java
 * Created from src/correct/convolutional/convolutional.c
 * 				src/correct/convolutional/encode.c
 * 				src/correct/convolutional/decode.c  @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;


import static libcorrect.SoftMeasurement.CORRECT_SOFT_LINEAR;
import static libcorrect.convolutional.Metric.metricDistance;

public class Convolutional {
    // Convolutional Codes
	// Convolutional polynomials are 16 bits wide
	public final static short[] correctConvR126Polynomial = {073, 061};
	public final static short[] correctConvR127Polynomial = {0161, 0127};
	public final static short[] correctConvR128Polynomial = {0225, 0373};
	public final static short[] correctConvR129Polynomial = {0767, 0545};
	public final static short[] correctConvR136Polynomial = {053, 075, 047};
	public final static short[] correctConvR137Polynomial = {0137, 0153, 0121};
	public final static short[] correctConvR138Polynomial = {0333, 0257, 0351};
	public final static short[] correctConvR139Polynomial = {0417, 0627, 0675};


	 // Maximum of unsigned integral types.
	private final static int UINT8_MAX = 255;
	private final static int UINT16_MAX = 65_535;
	private final static byte softMax_U = (byte)UINT8_MAX;
	private final static short distanceMax_U = (short)UINT16_MAX;



	private final int[] table_U;              	/* size 2**order */
	private final int rate_U;                	/* e.g. 2, 3...  */
  	private final int order_U;         		    /* e.g. 7, 9...	 */
  	private final int numstates_U;              /* 2**order 	 */
  	private final BitWriter bitWriter;
  	private final BitReader bitReader;
  
  	private boolean hasInitDecode;
  	private short[]   distances_U;
  	private PairLookup pairLookup;
  	private int softMeasurement;
  	private HistoryBuffer historyBuffer;
  	private ErrorBuffer errorBuffer;

	/**
	 *  correct_convolutional_create allocates and initializes an encoder/decoder for
	 * a convolutional code with the given parameters. This function expects that
	 * poly will contain inv_rate elements. E.g., to create a conv. code instance
	 * with rate 1/2, order 7 and polynomials 0161, 0127, call
	 * correct_convolutional_create(2, 7, []correct_convolutional_polynomial_t{0161, 0127});
	 *
	 * If this call is successful, it returns a non-NULL pointer.
	 */
public Convolutional(int rate_U, int order_U, short[] poly_U) throws IllegalArgumentException {
		if(Integer.compareUnsigned(order_U, Integer.SIZE) > 0) {
       // XXX turn this into an error code
        // printf("order must be smaller than 8 * sizeof(shift_register_t)\n");
			throw new IllegalArgumentException("Convolutional: order must be smaller than 8 * sizeof(shift_register_t)");
		}
		if(Long.compareUnsigned(rate_U, 2) < 0) {
			// XXX turn this into an error code
			// printf("rate must be 2 or greater\n");
			throw new IllegalArgumentException("Convolutional: rate must be 2 or greater");
		}
		this.order_U = order_U;
		this.rate_U = rate_U;
		this.numstates_U = (1 << order_U);
		
		this.table_U = new int [1 << order_U];
		PairLookup.fillTable(this.rate_U, this.order_U, poly_U, this.table_U);

		bitWriter = new BitWriter(null,0);
		bitReader = new BitReader(null,0);
	
		this.hasInitDecode = false;
	}
	/**
	 * correct_convolutional_encode_len returns the number of *bits*
	 * in a msg_len of given size, in *bytes*. In order to convert
	 * this returned length to bytes, save the result of the length
	 * modulo 8. If it's nonzero, then the length in bytes is
	 * length/8 + 1. If it is zero, then the length is just
	 * length/8.
	 */

	public long correctConvolutionalEncodeLen(long msgLen_U) {
		long msgbits_U = 8 * msgLen_U;
		long encodedbits_U = rate_U * (msgbits_U + order_U + 1);
		return encodedbits_U;
	}

	/**
	 * correct_convolutional_encode uses the given conv instance to
	 * encode a block of data and write it to encoded. The length of
	 * encoded must be long enough to hold the resulting encoded length,
	 * which can be calculated by calling correct_convolutional_encode_len.
	 * However, this length should first be converted to bytes, as that
	 * function returns the length in bits.
	 *
	 * This function returns the number of bits written to encoded. If
	 * this is not an exact multiple of 8, then it occupies an additional
	 * byte.
	 */

	// shift in most significant bit every time, one byte at a time
	// shift register takes most recent bit on right, shifts left
	// poly is written in same order, just & mask message w/ poly

	// assume that encoded length is long enough?
	public long correctConvolutionalEncode(byte[] msg_U, long msgLen_U,byte[] encoded_U) {

		// convolutional code convolves filter coefficients, given by
		//     the polynomial, with some history from our message.
		//     the history is stored as single subsequent bits in shiftregister
		int shiftregister_U = 0;

		// shiftmask is the shiftregister bit mask that removes bits
		//      that extend beyond order
		// e.g. if order is 7, then remove the 8th bit and beyond
		int shiftmask_U = (1 << order_U) - 1;

		long encodedLenBits_U = correctConvolutionalEncodeLen(msgLen_U);
		long encodedLen_U = Long.remainderUnsigned(encodedLenBits_U, 8) != 0 ?
									Long.divideUnsigned(encodedLenBits_U, 8) + 1 :
									Long.divideUnsigned(encodedLenBits_U, 8);
		bitWriter.bitWriterReconfigure(encoded_U, encodedLen_U);
		bitReader.bitReaderReconfigure(msg_U, msgLen_U);

		for(long i_U = 0; Long.compareUnsigned(i_U, 8 * msgLen_U) < 0; i_U++) {
			// shiftregister has oldest bits on left, newest on right
			shiftregister_U <<= 1;
			shiftregister_U |= bitReader.bitReaderRead(1);
			shiftregister_U &= shiftmask_U;
			// shift most significant bit from byte and move down one bit at a time

			// we do direct lookup of our convolutional output here
			// all of the bits from this convolution are stored in this row
			int out_U = table_U[shiftregister_U];
			bitWriter.bitWriterWrite((byte)out_U, rate_U);
		}
		// now flush the shiftregister
		// this is simply running the loop as above but without any new inputs
		// or rather, the new input string is all 0s
		for(long i_U = 0; Long.compareUnsigned(i_U, order_U + 1) < 0; i_U++) {
			shiftregister_U <<= 1;
			shiftregister_U &= shiftmask_U;
			int out_U = table_U[shiftregister_U];
			bitWriter.bitWriterWrite((byte) out_U, rate_U);
		}

		// 0-fill any remaining bits on our final byte
		bitWriter.bitWriterFlushByte();

		return encodedLenBits_U;
	}
	public void convDecodePrintIter(int iter_U, int winnerIndex_U) {
		if(Integer.compareUnsigned(iter_U, 2_220) < 0) {
			return;
		}
		System.out.println("iteration: " + iter_U);
		System.out.println("errors:");
		for(int i_U = 0; Integer.compareUnsigned(i_U, Integer.divideUnsigned(numstates_U, 2)) < 0; i_U++) {
			System.out.printf("%2d: %d\n", i_U, Short.toUnsignedInt(errorBuffer.getWriteError(i_U)));
		}
		System.out.println();
		System.out.println("history:");
		for(int i = 0; Integer.compareUnsigned(i, Integer.divideUnsigned(numstates_U, 2)) < 0; i++) {
			System.out.printf("%2d: ", i);
			for(int j = 0; Integer.compareUnsigned(j, winnerIndex_U) <= 0; j++) {
				System.out.printf("%d", historyBuffer.getHistory(j,i) != 0 ? 1 : 0);
			}
			System.out.println();
		}
		System.out.println();
	}

	private void convolutionalDecodeWarmup(int sets_U, byte[] soft_U) {
		// first phase: load shiftregister up from 0 (order goes from 1 to conv->order)
		// we are building up error metrics for the first order bits
		for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), this.order_U - 1) < 0 && Integer.compareUnsigned(i_U, sets_U) < 0; i_U++) {
			// peel off rate bits from encoded to recover the same `out` as in the encoding process
			// the difference being that this `out` will have the channel noise/errors applied
			int out_U=0;
			if(soft_U == null) {
				out_U = bitReader.bitReaderRead(this.rate_U);
			}
			// walk all of the state we have so far
			for(int j_U = 0; Integer.compareUnsigned(j_U, 1 << i_U + 1) < 0; j_U += 1) {
				int last_U = (int)(j_U >>> 1);
				short dist_U;

				if(soft_U != null) {
					if (softMeasurement == CORRECT_SOFT_LINEAR) {
						dist_U = Metric.metricSoftDistanceLinear(table_U[j_U], soft_U, this.rate_U, i_U * this.rate_U);
					} else {
						dist_U = Metric.metricSoftDistanceQuadratic(table_U[j_U], soft_U, this.rate_U, i_U * this.rate_U);
					}
				} else {
					dist_U = metricDistance(table_U[(int) j_U],out_U);
				}
				errorBuffer.setWriteError(j_U, (short) (dist_U + errorBuffer.getReadError(last_U)));
			}
			errorBuffer.errorBufferSwap();
		}
	}
	private void convolutionalDecodeInner(int sets_U, byte[] soft_U) {
		int highbit_U = 1 << order_U - 1;
		for(int i_U = order_U - 1; Long.compareUnsigned(Integer.toUnsignedLong(i_U), Integer.toUnsignedLong(sets_U) - order_U + 1) < 0; i_U++) {
			// lasterrors are the aggregate bit errors for the states of shiftregister for the previous
			// time slice
			if(soft_U != null) {
				if(softMeasurement == CORRECT_SOFT_LINEAR) {
					for(int j_U = 0; Integer.compareUnsigned(j_U, 1 << rate_U) < 0; j_U++) {
						distances_U[j_U] = Metric.metricSoftDistanceLinear(j_U, soft_U, rate_U,i_U * rate_U);
					}
				} else {
					for(int j_U = 0; Integer.compareUnsigned(j_U, 1 << rate_U) < 0; j_U++) {
						distances_U[j_U] = (short)Metric.metricSoftDistanceQuadratic(j_U, soft_U, rate_U,i_U * rate_U);
					}
				}
			} else {
				int out_U = bitReader.bitReaderRead(rate_U);
				for (int i2_U = 0; Integer.compareUnsigned(i2_U, 1 << rate_U) < 0; i2_U++) {
					distances_U[i2_U] = metricDistance(i2_U, out_U);
				}
			}

			pairLookup.pairLookupFillDistance(distances_U);

			// a mask to get the high order bit from the shift register
			int numIter_U = highbit_U << 1;
			// aggregate bit errors for this time slice

			byte[] history_U = historyBuffer.historyBufferGetSlice();
			// walk through all states, ignoring oldest bit
			// we will track a best register state (path) and the number of bit errors at that path at
			// this time slice
			// this loop considers two paths per iteration (high order bit set, clear)
			// so, it only runs numstates/2 iterations
			// we'll update the history for every state and find the path with the least aggregated bit
			// errors

			// now run the main loop
			// we calculate 2 sets of 2 register states here (4 states per iter)
			// this creates 2 sets which share a predecessor, and 2 sets which share a successor
			//
			// the first set definition is the two states that are the same except for the least order
			// bit
			// these two share a predecessor because their high n - 1 bits are the same (differ only by
			// newest bit)
			//
			// the second set definition is the two states that are the same except for the high order
			// bit
			// these two share a successor because the oldest high order bit will be shifted out, and
			// the other bits will be present in the successor
			//
			int highbase_U = highbit_U >>> 1;
			for(int low_U = 0, high_U = highbit_U, base_U = 0; Integer.compareUnsigned(high_U, numIter_U) < 0; low_U += 8, high_U += 8, base_U += 4) {
				// shifted-right ancestors
				// low and low_plus_one share low_past_error
				//   note that they are the same when shifted right by 1
				// same goes for high and high_plus_one
				for(int offset_U = 0, baseOffset_U = 0; Integer.compareUnsigned(baseOffset_U, 4) < 0; offset_U += 2, baseOffset_U += 1) {
					int lowKey_U = pairLookup.getKey(base_U + baseOffset_U);
					int highKey_U = pairLookup.getKey(highbase_U + base_U + baseOffset_U);
					int lowConcatDist_U = pairLookup.getDistance(lowKey_U);
					int highConcatDist_U = pairLookup.getDistance(highKey_U);

					short lowPastError_U = errorBuffer.getReadError(base_U + baseOffset_U);
					short highPastError_U = errorBuffer.getReadError(highbase_U + base_U + baseOffset_U);

					short lowError_U = (short)((lowConcatDist_U & 0xffff) + Short.toUnsignedInt(lowPastError_U));
					short highError_U = (short)((highConcatDist_U & 0xffff) + Short.toUnsignedInt(highPastError_U));

					int successor_U = low_U + offset_U;
					short error_U;
					byte historyMask_U;
					if(Short.toUnsignedInt(lowError_U) <= Short.toUnsignedInt(highError_U)) {
						error_U = lowError_U;
						historyMask_U = 0;
					} else {
						error_U = highError_U;
						historyMask_U = 1;
					}
					errorBuffer.setWriteError(successor_U,error_U);
					history_U[successor_U] = historyMask_U;

					int lowPlusOne_U = low_U + offset_U + 1;
					short lowPlusOneError_U = (short)((lowConcatDist_U >>> 16) + Short.toUnsignedInt(lowPastError_U));
					short highPlusOneError_U = (short)((highConcatDist_U >>> 16) + Short.toUnsignedInt(highPastError_U));

					short plusOneError_U;
					int plusOneSuccessor_U = lowPlusOne_U;
					byte plusOneHistoryMask_U;
					if(Short.toUnsignedInt(lowPlusOneError_U) <= Short.toUnsignedInt(highPlusOneError_U)) {
						plusOneError_U = lowPlusOneError_U;
						plusOneHistoryMask_U = 0;
					} else {
						plusOneError_U = highPlusOneError_U;
						plusOneHistoryMask_U = 1;
					}

					errorBuffer.setWriteError(plusOneSuccessor_U, plusOneError_U);
					history_U[plusOneSuccessor_U] = plusOneHistoryMask_U;

				}
			}
			historyBuffer.historyBufferProcess(errorBuffer.getWriteErrors(), bitWriter);
			errorBuffer.errorBufferSwap();
		}
	}

	private void convolutionalDecodeTail(int sets_U, byte[] soft_U) {
		// flush state registers
		// now we only shift in 0s, skipping 1-successors
		int highbit_U = 1 << order_U - 1;

		for(int i_U = (int)(Integer.toUnsignedLong(sets_U) - order_U + 1); Integer.compareUnsigned(i_U, sets_U) < 0; i_U++) {
			// lasterrors are the aggregate bit errors for the states of shiftregister for the previous
			// time slice
			byte[] history_U = historyBuffer.historyBufferGetSlice();
			// calculate the distance from all output states to our sliced bits
			if(soft_U != null) {
				if(softMeasurement == CORRECT_SOFT_LINEAR) {
					for(int j_U = 0; Integer.compareUnsigned(j_U, 1 << rate_U) < 0; j_U++) {
						distances_U[j_U] = Metric.metricSoftDistanceLinear(j_U, soft_U, rate_U,i_U * rate_U);
					}
				} else {
					for(int j_U = 0; Integer.compareUnsigned(j_U, 1 << rate_U) < 0; j_U++) {
						distances_U[j_U] = (short)Metric.metricSoftDistanceQuadratic(j_U, soft_U, rate_U,i_U * rate_U);
					}
				}
			} else {
				int out_U = bitReader.bitReaderRead(rate_U);
				for (int i2_U = 0; Integer.compareUnsigned(i2_U, 1 << rate_U) < 0; i2_U++) {
					distances_U[i2_U] = metricDistance(i2_U, out_U);
				}
			}

			// a mask to get the high order bit from the shift register
			int numIter_U = highbit_U << 1;
			int skip_U = 1 << order_U - Integer.toUnsignedLong(sets_U - i_U);
			int baseSkip_U = skip_U >>> 1;

			int highbase_U = highbit_U >>> 1;
			for(int low_U = 0, high_U = highbit_U, base_U = 0; Integer.compareUnsigned(high_U, numIter_U) < 0; low_U += skip_U, high_U += skip_U, base_U += baseSkip_U) {
				int lowOutput_U = table_U[low_U];
				int highOutput_U = table_U[high_U];
				short lowDist_U = distances_U[lowOutput_U];
				short highDist_U = distances_U[highOutput_U];

				short lowPastError_U = errorBuffer.getReadError(base_U);
				short highPastError_U = errorBuffer.getReadError(highbase_U + base_U);

				short lowError_U = (short)(Short.toUnsignedInt(lowDist_U) + Short.toUnsignedInt(lowPastError_U));
				short highError_U = (short)(Short.toUnsignedInt(highDist_U) + Short.toUnsignedInt(highPastError_U));

				int successor_U = low_U;
				short error_U;
				byte historyMask_U;
				if(Short.toUnsignedInt(lowError_U) <= Short.toUnsignedInt(highError_U)) {
					error_U = lowError_U;
					historyMask_U = 0;
				} else {
					error_U = highError_U;
					historyMask_U = 1;
				}
				errorBuffer.setWriteError(successor_U,error_U);
				history_U[successor_U] = historyMask_U;

			}
			historyBuffer.historyBufferProcessSkip(errorBuffer.getWriteErrors(), bitWriter, skip_U);
			errorBuffer.errorBufferSwap();
		}

	}

	private void convolutionalDecodeInit(int minTraceback_U, int tracebackLength_U, int renormalizeInterval_U) {
		hasInitDecode = true;
		distances_U = new short[1 << rate_U];
		pairLookup = new PairLookup(rate_U, order_U, table_U);
		softMeasurement = CORRECT_SOFT_LINEAR;

		// we limit history to go back as far as 5 * the order of our polynomial
		historyBuffer = new HistoryBuffer(minTraceback_U, tracebackLength_U, renormalizeInterval_U,
								numstates_U / 2, 1 << (order_U - 1));
		errorBuffer = new ErrorBuffer(numstates_U);
	}

	private long convolutionalDecode(long numEncodedBits_U, long numEncodedBytes_U, byte[] msg_U, byte[] softEncoded_U) {
		if(!this.hasInitDecode) {
			long maxErrorPerInput_U = rate_U * Byte.toUnsignedLong(softMax_U);
			int renormalizeInterval_U = (int)Long.divideUnsigned(Short.toUnsignedLong(distanceMax_U), maxErrorPerInput_U);
			convolutionalDecodeInit(5 * order_U, 15 * order_U, renormalizeInterval_U);
		}

		int sets_U = (int) Long.divideUnsigned(numEncodedBits_U, rate_U);
		// XXX fix this vvvvvv
		long decodedLenBytes_U = numEncodedBytes_U;
		bitWriter.bitWriterReconfigure(msg_U, decodedLenBytes_U);
		errorBuffer.errorBufferReset();
		historyBuffer.historyBufferReset();

		// no outputs are generated during warmup
		convolutionalDecodeWarmup(sets_U, softEncoded_U);
		convolutionalDecodeInner(sets_U, softEncoded_U);
		convolutionalDecodeTail(sets_U, softEncoded_U);

		historyBuffer.historyBufferFlush(bitWriter);

		return bitWriter.bitWriterLength_U();
	}

	/**
	 * correct_convolutional_decode uses the given conv instance to
	 * decode a block encoded by correct_convolutional_encode. This
	 * call can cope with some bits being corrupted. This function
	 * cannot detect if there are too many bits corrupted, however,
	 * and will still write a message even if it is not recovered
	 * correctly. It is up to the user to perform checksums or CRC
	 * in order to guarantee that the decoded message is intact.
	 *
	 * num_encoded_bits should contain the length of encoded in *bits*.
	 * This value need not be an exact multiple of 8. However,
	 * it must be a multiple of the inv_rate used to create
	 * the conv instance.
	 *
	 * This function writes the result to msg, which must be large
	 * enough to hold the decoded message. A good conservative size
	 * for this buffer is the number of encoded bits multiplied by the
	 * rate of the code, e.g. for a rate 1/2 code, divide by 2. This
	 * value should then be converted to bytes to find the correct
	 * length for msg.
	 *
	 * This function returns the number of bytes written to msg. If
	 * it fails, it returns -1.
	 */
	public long correctConvolutionalDecode(byte[] encoded_U, long numEncodedBits_U, byte[] msg_U) throws IllegalArgumentException {
		if(Long.remainderUnsigned(numEncodedBits_U, this.rate_U) != 0) {
			// XXX turn this into an error code
			// printf("encoded length of message must be a multiple of rate\n");
			throw new IllegalArgumentException("correctConvolutionalDecode: encoded length of message must be a multiple of rate");
		}

		long numEncodedBytes_U = Long.remainderUnsigned(numEncodedBits_U, 8) != 0 ? Long.divideUnsigned(numEncodedBits_U, 8) + 1 :
																						  Long.divideUnsigned(numEncodedBits_U, 8);
		bitReader.bitReaderReconfigure(encoded_U, numEncodedBytes_U);

		return convolutionalDecode(numEncodedBits_U, numEncodedBytes_U, msg_U, null);
	}
	/**
	 *  correct_convolutional_decode_soft uses the given conv instance
	 * to decode a block encoded by correct_convolutional_encode and
	 * then modulated/demodulated to 8-bit symbols. This function expects
	 * that 1 is mapped to 255 and 0 to 0. An erased symbol should be
	 * set to 128. The decoded message may contain errors.
	 *
	 * num_encoded_bits should contain the length of encoded in *bits*.
	 * This value need not be an exact multiple of 8. However,
	 * it must be a multiple of the inv_rate used to create
	 * the conv instance.
	 *
	 * This function writes the result to msg, which must be large
	 * enough to hold the decoded message. A good conservative size
	 * for this buffer is the number of encoded bits multiplied by the
	 * rate of the code, e.g. for a rate 1/2 code, divide by 2. This
	 * value should then be converted to bytes to find the correct
	 * length for msg.
	 *
	 * This function returns the number of bytes written to msg. If
	 * it fails, it returns -1.
	 */

	public long correctConvolutionalDecodeSoft(byte[] encoded_U, long numEncodedBits_U, byte[] msg_U) throws IllegalArgumentException {
		if(Long.remainderUnsigned(numEncodedBits_U, this.rate_U) != 0) {
			// XXX turn this into an error code
			// printf("encoded length of message must be a multiple of rate\n");
			throw new IllegalArgumentException("correctConvolutionalDecode: encoded length of message must be a multiple of rate");
		}

		long numEncodedBytes_U = Long.remainderUnsigned(numEncodedBits_U, 8) != 0 ? Long.divideUnsigned(numEncodedBits_U, 8) + 1 :
								 Long.divideUnsigned(numEncodedBits_U, 8);

		return convolutionalDecode(numEncodedBits_U, numEncodedBytes_U, msg_U, encoded_U);
	}

}
