/*
 * libcorrect4j
 * HistoryBuffer.java
 * Created from src/correct/convolutional/history_buffer.c @ https://github.com/quiet/libcorrect
 */
package libcorrect.convolutional;

// generates output bits after accumulating sufficient history
public class HistoryBuffer {
    // history entries must be at least this old to be decoded
    private final int minTracebackLength_U;
    // we'll decode entries in bursts. this tells us the length of the burst
    private final int tracebackGroupLength_U;
    // we will store a total of cap entries. equal to min_traceback_length +
    // traceback_group_length
    private final int cap_U;
    // how many states in the shift register? this is one of the dimensions of
    // history table
    private final int numStates_U;
    // what's the high order bit of the shift register?
    private final int highbit_U;
    // history is a compact history representation for every shift register
    // state,
    //  one bit per time slice
    private final byte[][] history_U;
    // which slice are we writing next?
    private int index_U;
    //how many valid entries are there?
    private int len_U;
    // temporary store of fetched bits
    private final byte[] fetched_U;
    // how often should we renormalize?
    private final int renormalizeInterval_U;
    private int renormalizeCounter_U;

    public byte getHistory(int i, int j) {
        return history_U[i][j];
    }

    public HistoryBuffer(int minTracebackLength_U,
                         int tracebackGroupLength_U,
                         int renormalizeInterval_U,
                         int numStates_U,
                         int highbit_U) {
        this.minTracebackLength_U = minTracebackLength_U;
        this.tracebackGroupLength_U = tracebackGroupLength_U;
        this.cap_U = minTracebackLength_U + tracebackGroupLength_U;
        this.numStates_U = numStates_U;
        this.highbit_U = highbit_U;

        this.history_U = new byte[this.cap_U][this.numStates_U];
        this.fetched_U = new byte[this.cap_U];

        this.index_U = 0;
        this.len_U = 0;

        this.renormalizeCounter_U = 0;
        this.renormalizeInterval_U = renormalizeInterval_U;
    }

    public void historyBufferReset() {
        len_U = 0;
        index_U = 0;
    }

    public byte[] historyBufferGetSlice() {
        return history_U[index_U];
    }

    public int historyBufferSearch(short[] distances_U, int searchEvery_U) {
        int bestpath_U = 0;
        int leasterror_U = Integer.MAX_VALUE;
        // search for a state with the least error
        for(int state_U = 0; Integer.compareUnsigned(state_U, numStates_U) < 0; state_U += searchEvery_U) {
            if(Short.toUnsignedInt(distances_U[state_U]) < leasterror_U) {
                leasterror_U = distances_U[state_U];
                bestpath_U = state_U;
            }
        }
        return bestpath_U;
    }

    public void historyBufferRenormalize(short[] distances_U, int minRegister_U) {
        short minDistance_U = distances_U[minRegister_U];
        for(int i_U = 0; Integer.compareUnsigned(i_U, numStates_U) < 0; i_U++) {
            distances_U[i_U] = (short)(Short.toUnsignedInt(distances_U[i_U]) - Short.toUnsignedInt(minDistance_U));
        }
    }

    public void historyBufferTraceback(int bestpath_U,
                                       int minTracebackLength_U,
                                       BitWriter output) {
        int fetchedIndex_U = 0;
        int highbit_U = this.highbit_U;
        int index_U = this.index_U;
        int cap_U = this.cap_U;
        for(int j_U = 0; Integer.compareUnsigned(j_U, minTracebackLength_U) < 0; j_U++) {
            if(index_U == 0) {
                index_U = cap_U - 1;
            } else {
                index_U--;
            }
            // we're walking backwards from what the work we did before
            // so, we'll shift high order bits in
            // the path will cross multiple different shift register states, and we determine
            //   which state by going backwards one time slice at a time
            byte history_U = this.history_U[index_U][bestpath_U];
            int pathbit_U = history_U != 0 ? highbit_U : 0;
            bestpath_U |= pathbit_U;
            bestpath_U >>>= 1;
        }
        int prefetchIndex_U = index_U;
        if(prefetchIndex_U == 0) {
            prefetchIndex_U = cap_U - 1;
        } else {
            prefetchIndex_U--;
        }
        int len_U = this.len_U;
        for(int j_U = minTracebackLength_U; Integer.compareUnsigned(j_U, len_U) < 0; j_U++) {
            index_U = prefetchIndex_U;
            if(prefetchIndex_U == 0) {
                prefetchIndex_U = cap_U - 1;
            } else {
                prefetchIndex_U--;
            }
            // noop (see include/correct/portable.h) prefetch(history_U[prefetchIndex_U]);
            // we're walking backwards from what the work we did before
            // so, we'll shift high order bits in
            // the path will cross multiple different shift register states, and we determine
            //   which state by going backwards one time slice at a time
            byte history_U = this.history_U[index_U][bestpath_U];
            int pathbit_U = history_U != 0 ? highbit_U : 0;
            bestpath_U |= pathbit_U;
            bestpath_U >>>= 1;
            this.fetched_U[fetchedIndex_U]=(byte)(pathbit_U != 0 ? 1 : 0);
            fetchedIndex_U++;
        }
        output.bitWriterWriteBitlistReversed(this.fetched_U, fetchedIndex_U);
        this.len_U -= fetchedIndex_U;
    }
    public void historyBufferProcessSkip(short[] distances_U, BitWriter output, int skip_U) {
        this.index_U++;
        if(this.index_U == this.cap_U) {
            this.index_U = 0;
        }

        this.renormalizeCounter_U++;
        this.len_U++;
        // there are four ways these branches can resolve
        // a) we are neither renormalizing nor doing a traceback
        // b) we are renormalizing but not doing a traceback
        // c) we are renormalizing and doing a traceback
        // d) we are not renormalizing but we are doing a traceback
        // in case c, we want to save the effort of finding the bestpath
        //    since that's expensive
        // so we have to check for that case after we renormalize

        if(this.renormalizeCounter_U == this.renormalizeInterval_U) {
            this.renormalizeCounter_U = 0;
            int bestpath_U = historyBufferSearch(distances_U, skip_U);
            historyBufferRenormalize(distances_U, bestpath_U);
            if(this.len_U == this.cap_U) {
                // reuse the bestpath found for renormalizing
                historyBufferTraceback(bestpath_U, this.minTracebackLength_U, output);
            }
        } else if(this.len_U == this.cap_U) {
            // not renormalizing, find the bestpath here
            int bestpath_U = historyBufferSearch(distances_U, skip_U);
            historyBufferTraceback(bestpath_U, this.minTracebackLength_U, output);
        }
    }

    public void historyBufferProcess(short[] distances_U, BitWriter output) {
        historyBufferProcessSkip(distances_U, output, 1);
    }

    public void historyBufferFlush(BitWriter output) {
        historyBufferTraceback(0, 0, output);
    }


}
