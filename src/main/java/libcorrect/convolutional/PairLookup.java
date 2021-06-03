/*
 * libcorrect4j
 * PairLookup.java
 * Created from src/correct/convolutional/lookup.c @ https://github.com/quiet/libcorrect
 */
package libcorrect.convolutional;

public class PairLookup {
    private final int[] keys_U;
    private final int[] outputs_U;
    private final int outputMask_U;
    private final int outputWidth_U;
    private final int outputsLen_U;
    private final int[] distances_U;

/*
 *  C popcount clone taken from the helpful http://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
 *  It used to be state of the art for some reason but bitCount works good enough these days

    private static int popcount(int x) {

        x = x - (x >> 1 & 0x55555555);
        x = (x & 0x33333333) + (x >> 2 & 0x33333333);
        return (x + (x >> 4) & 0x0f0f0f0f) * 0x01010101 >> 24;
    }
 */

    public  PairLookup (int rate_U, int order_U, int[] table_U) {
        this.keys_U = new int[1 << (order_U - 1)];
        this.outputs_U = new int[1 << (rate_U * 2)];
        int[] invOutputs_U = new int[1 << (rate_U * 2)];
        int outputCounter_U = 1;

        // for every (even-numbered) shift register state, find the concatenated output of the state
        //   and the subsequent state that follows it (low bit set). then, check to see if this
        //   concatenated output has a unique key assigned to it already. if not, give it a key.
        //   if it does, retrieve the key. assign this key to the shift register state.

        for(int i = 0; Integer.compareUnsigned(i, 1 << order_U - 1) < 0; i++) {
            // first get the concatenated pair of outputs
            int out_U = table_U[i * 2 + 1];
            out_U <<= rate_U;
            out_U |= table_U[i * 2];
            // does this concatenated output exist in the outputs table yet?
            if(invOutputs_U[out_U] == 0) {
                invOutputs_U[out_U] = outputCounter_U;
                this.outputs_U[outputCounter_U] =  out_U;
                outputCounter_U++;
            }
            // set the opaque key for the ith shift register state to the concatenated output entry
            this.keys_U[i] = invOutputs_U[out_U];
        }
        this.outputsLen_U = outputCounter_U;
        this.outputMask_U = (1 << rate_U) - 1;
        this.outputWidth_U = rate_U;
        this.distances_U = new int[this.outputsLen_U];
    }
    public void fillDistance(short[] distances_U) {
        for(int i = 1; Integer.compareUnsigned(i, this.outputsLen_U) < 0; i ++) {
            int concatOut_U = this.outputs_U[i];
            int i0_U = concatOut_U & this.outputMask_U;
            concatOut_U >>>= this.outputWidth_U;
            int i1_U = concatOut_U;

            this.distances_U[i]=Short.toUnsignedInt(distances_U[i1_U]) << 16 |
                                  Short.toUnsignedInt(distances_U[i0_U]);
        }
    }
    public int getKey(int i) {
        return keys_U[i];
    }
    public int getDistance(int i) {
        return distances_U[i];
    }

}
