/*
 * libcorrect4j
 * Convolutional.java
 * Created from src/correct/convolutional/convolutional.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;


public class Convolutional {

  private int[] table_U;              /* size 2**order */
  private long rate_U;                /* e.g. 2, 3...  */
  private final long order_U;         /* e.g. 7, 9...	 */
  private int numstates_U;            /* 2**order 	 */
  private BitWriter bitWriter;
  private BitReader bitReader;
  
  private boolean hasInitDecode;
  private short[]   distances_U;
  private PairLookup pairLookup;
  private int softMeasuremen;
  private HistoryBuffer historyBuffer;
  private ErrorBuffer errorBuffer;

	
	
private Convolutional(long rate_U, long order_U, short[] poly_U) {
		if(Long.compareUnsigned(order_U, 8 * ((long)INT_SIZE)) > 0) {
       // XXX turn this into an error code
        // printf("order must be smaller than 8 * sizeof(shift_register_t)\n");
			throw EINVAL;
		}
		if(Long.compareUnsigned(rate_U, 2) < 0) {
			// XXX turn this into an error code
			// printf("rate must be 2 or greater\n");
			throw EINVAL;
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

	
	
}
