public class Convolutional {
   private int[] table_U;              /* size 2**order */
   private long rate_U;                /* e.g. 2, 3...  */
   private final long order_U;         /* e.g. 7, 9...	 */
 	 private int numstates_U;            /* 2**order 	 */
   private BitWriter bitWriter;
   private BitReade  bitReade;
  
 	 private boolean hasInitDecode;
   private short[]   distances_U;
   private PairLookup pairLookup;
	 private int softMeasuremen;
   private HistoreBuffer historyBuffer;
   private ErrorBuffer errorBuffer;
}
