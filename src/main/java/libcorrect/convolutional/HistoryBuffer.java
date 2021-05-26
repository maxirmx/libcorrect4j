/*
 * libcorrect4j
 * HistoryBuffer.java
 * Created from src/correct/convolutional/history_buffer.c @ https://github.com/quiet/libcorrect
 */
package libcorrect.convolutional;

// generates output bits after accumulating sufficient history
public class HistoryBuffer {
    // history entries must be at least this old to be decoded
    private int minTracebackLength_U;
    // we'll decode entries in bursts. this tells us the length of the burst
    private int tracebackGroupLength_U;
    // we will store a total of cap entries. equal to min_traceback_length +
    // traceback_group_length
    private int cap;
    // how many states in the shift register? this is one of the dimensions of
    // history table
    private int numStates_U;
    // what's the high order bit of the shift register?
    private int highbit_U;
    // history is a compact history representation for every shift register
    // state,
    //  one bit per time slice
    private byte[][] history_U;
    // which slice are we writing next?
    private int index_U;
    //how many valid entries are there?
    private int len_U;
    // temporary store of fetched bits
    private byte[] fetched_U;
    // how often should we renormalize?
    private int renormalizeInterval_U;
    private int renormalizeCounter_U;

}
