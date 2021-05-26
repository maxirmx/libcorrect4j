/*
 * libcorrect4j
 * PairLookup.java
 * Created from src/correct/convolutional/lookup.c @ https://github.com/quiet/libcorrect
 */
package libcorrect.convolutional;


public class PairLookup {
    private int[] keys_U;
    private int[] outputs_U;
    private int outputMask_U;
    private int outputWidth_U;
    private int outputsLen_U;
    private int[] distances_U;

    public static void fillTable(int rate_U, int order_U, short[] poly_U, int[] table_U) {
        for(int i_U = 0; Integer.compareUnsigned(i_U, 1 << order_U) < 0; i_U++) {
            int out_U = 0;
            int mask_U = 1;
            for(int j_U = 0; Integer.compareUnsigned(j_U, rate_U) < 0; j_U++) {
                out_U |= Integer.bitCount(i_U & Short.toUnsignedInt(poly_U[j_U])) % 2 != 0 ? mask_U : 0;
                mask_U <<= 1;
            }
            table_U[i_U] = out_U;
        }
    }

    public  PairLookup (int rate_U, int order_U, int[] table_U) {
        this.keys_U = new int[1 << (order_U - 1)];
        this.outputs_U = new int[1 << (rate_U * 2)];
        int[] invOutputs_U = new int[1 << (rate_U * 2)];
        int outputCounter_U = 1;

        // for every (even-numbered) shift register state, find the concatenated output of the state
        //   and the subsequent state that follows it (low bit set). then, check to see if this
        //   concatenated output has a unique key assigned to it already. if not, give it a key.
        //   if it does, retrieve the key. assign this key to the shift register state.

        for(int i_U = 0; Integer.compareUnsigned(i_U, 1 << order_U - 1) < 0; i_U++) {
            // first get the concatenated pair of outputs
            int out_U = table_U[i_U * 2 + 1];
            out_U <<= rate_U;
            out_U |= table_U[i_U * 2];
            // does this concatenated output exist in the outputs table yet?
            if(invOutputs_U[out_U] == 0) {
                invOutputs_U[out_U] = outputCounter_U;
                this.outputs_U[outputCounter_U] =  out_U;
                outputCounter_U++;
            }
            // set the opaque key for the ith shift register state to the concatenated output entry
            this.keys_U[i_U] = invOutputs_U[out_U];
        }
        this.outputsLen_U = outputCounter_U;
        this.outputMask_U = (1 << rate_U) - 1;
        this.outputWidth_U = rate_U;
        this.distances_U = new int[this.outputsLen_U];
    }
    public void pairLookupFillDistance(short[] distances_U) {
        for(int i_U = 1; Integer.compareUnsigned(i_U, this.outputsLen_U) < 0; i_U ++) {
            int concatOut_U = this.outputs_U[i_U];
            int i0_U = concatOut_U & this.outputMask_U;
            concatOut_U >>>= this.outputWidth_U;
            int i1_U = concatOut_U;

            this.distances_U[i_U]=Short.toUnsignedInt(distances_U[i1_U]) << 16 |
                                  Short.toUnsignedInt(distances_U[i0_U]);
        }
    }

}
