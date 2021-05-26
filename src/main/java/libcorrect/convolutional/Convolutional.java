/*
 * libcorrect4j
 * Convolutional.java
 * Created from src/correct/convolutional/convolutional.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;


public class Convolutional {

  private int[] table_U;              /* size 2**order */
  private int rate_U;                /* e.g. 2, 3...  */
  private int order_U;         /* e.g. 7, 9...	 */
  private int numstates_U;            /* 2**order 	 */
  private BitWriter bitWriter;
  private BitReader bitReader;
  
  private boolean hasInitDecode;
  private short[]   distances_U;
  private PairLookup pairLookup;
  private int softMeasuremen;
  private HistoryBuffer historyBuffer;
  private ErrorBuffer errorBuffer;

	
private Convolutional(int rate_U, int order_U, short[] poly_U) throws IllegalArgumentException {
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

	public long correctConvolutionalEncodeLen(long msgLen_U) {
		long msgbits_U = 8 * msgLen_U;
		long encodedbits_U = rate_U * (msgbits_U + order_U + 1);
		return encodedbits_U;
	}

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
			System.out.printf("%2d: %d\n", i_U, Short.toUnsignedInt(errorBuffer.getWriteErrors(i_U)));
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

}
