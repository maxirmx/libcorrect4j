/*
 * libcorrect4j
 * Polynomial.java
 * Created from src/correct/reed-solomon/encode.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.reed_solomon;

public class CorrectReedSolomon {
    private long blockLength_U;
    private long messageLength_U;
    private long minDistance_U;
    private byte firstConsecutiveRoot_U;
    private byte generatorRootGap_U;
    private Field field;
    private Polynomial generator;
    private byte[] generatorRoots_U;
    private byte[][] generatorRootExp_U;
    private Polynomial encodedPolynomial;
    private Polynomial encodedRemainder;
    private byte[] syndromes_U;
    private byte[] modifiedSyndromes_U;
    private Polynomial receivedPolynomial;
    private Polynomial errorLocator;
    private Polynomial errorLocatorLog;
    private Polynomial erasureLocator;
    private byte[] errorRoots_U;
    private byte[] errorVals_U;
    private byte[] errorLocations_U;

    private byte[][] elementExp_U;
    //  scratch (do no allocations at steady state)
    //  used during find_error_locator
    private Polynomial lastErrorLocator;

     // used during error value search
    private Polynomial errorEvaluator;
    private Polynomial errorLocatorDerivative;
    private Polynomial[] initFromRootsScratch = new Polynomial[2];
    private boolean hasInitDecode;



    public long correctReedSolomonEncode(byte[] msg_U, long msgLength_U, byte[] encoded_U) {
        if(Long.compareUnsigned(msgLength_U, messageLength_U) > 0) {
            return -1;
        }

        long padLength_U = messageLength_U - msgLength_U;

        // 0-fill the rest of the coefficients -- this length will always be > 0
        // because the order of this poly is block_length and the msg_length <= message_length
        // e.g. 255 and 223
        encodedPolynomial.flushCoeff();

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            // message goes from high order to low order but libcorrect polynomials go low to high
            // so we reverse on the way in and on the way out
            // we'd have to do a copy anyway so this reversal should be free
            encodedPolynomial.setCoeff((int)(Integer.toUnsignedLong(encodedPolynomial.getOrder()) - (Integer.toUnsignedLong(i_U) + padLength_U)), msg_U[i_U]);
        }

        Polynomial.polynomialMod(field, encodedPolynomial, generator, encodedRemainder);

        // now return byte order to highest order to lowest order
        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            encoded_U[i_U] = encodedPolynomial.getCoeff((int)(Integer.toUnsignedLong(encodedPolynomial.getOrder()) - (Integer.toUnsignedLong(i_U) + padLength_U)));
        }

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            encoded_U[(int)(msgLength_U + Integer.toUnsignedLong(i_U))] = encodedRemainder.getCoeff((int)(minDistance_U - Integer.toUnsignedLong(i_U + 1)));
        }

        return blockLength_U;

    }

}
