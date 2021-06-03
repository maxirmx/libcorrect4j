/*
 * libcorrect4j
 * Polynomial.java
 * Created from src/correct/reed-solomon/encode.c
 *              src/correct/reed-solomon/encode.c @ https://github.com/quiet/libcorrect
 */

package libcorrect.reed_solomon;

import java.util.Arrays;

import static libcorrect.reed_solomon.Polynomial.*;

public class CorrectReedSolomon {
    private static final short correctRsPrimitivePolynomial_8_4_3_2_0 = 0x11d;        // x^8 + x^4 + x^3 + x^2 + 1
    private static final short correctRsPrimitivePolynomial_8_5_3_1_0 = 0x12b;        // x^8 + x^5 + x^3 + x + 1
    private static final short correctRsPrimitivePolynomial_8_5_3_2_0 = 0x12d;        // x^8 + x^5 + x^3 + x^2 + 1
    private static final short correctRsPrimitivePolynomial_8_6_3_2_0 = 0x14d;        // x^8 + x^6 + x^3 + x^2 + 1
    private static final short correctRsPrimitivePolynomial_8_6_4_3_2_1_0 = 0x15f;    // x^8 + x^6 + x^4 + x^3 + x^2 + x + 1;
    private static final short correctRsPrimitivePolynomial_8_6_5_1_0 = 0x163;        // x^8 + x^6 + x^5 + x + 1
    private static final short correctRsPrimitivePolynomial_8_6_5_2_0 = 0x165;        // x^8 + x^6 + x^5 + x^2 + 1
    private static final short correctRsPrimitivePolynomial_8_6_5_3_0 = 0x169;        // x^8 + x^6 + x^5 + x^3 + 1
    private static final short correctRsPrimitivePolynomial_8_6_5_4_0 = 0x171;        // x^8 + x^6 + x^5 + x^4 + 1
    private static final short correctRsPrimitivePolynomial_8_7_2_1_0 = 0x187;        // x^8 + x^7 + x^2 + x + 1
    private static final short correctRsPrimitivePolynomial_8_7_3_2_0 = 0x18d;        // x^8 + x^7 + x^3 + x^2 + 1
    private static final short correctRsPrimitivePolynomial_8_7_5_3_0 = 0x1a9;        // x^8 + x^7 + x^5 + x^3 + 1
    private static final short correctRsPrimitivePolynomial_8_7_6_1_0 = 0x1c3;        // x^8 + x^7 + x^6 + x + 1
    private static final short correctRsPrimitivePolynomial_8_7_6_3_2_1_0 = 0x1cf;    // x^8 + x^7 + x^6 + x^3 + x^2 + x + 1
    private static final short correctRsPrimitivePolynomial_8_7_6_5_2_1_0 = 0x1e7;    // x^8 + x^7 + x^6 + x^5 + x^2 + x + 1
    private static final short correctRsPrimitivePolynomial_8_7_6_5_4_2_0 = 0x1f5;    // x^8 + x^7 + x^6 + x^5 + x^4 + x^2 + 1
    private static final short correctRsPrimitivePolynomialCcsds      = 0x187;        // x^8 + x^7 + x^2 + x + 1



    private final long blockLength_U;
    private final long messageLength_U;
    private final long minDistance_U;
    private final byte firstConsecutiveRoot_U;
    private final byte generatorRootGap_U;
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

    // coeff must be of size nroots + 1
    // e.g. 2 roots (x + alpha)(x + alpha^2) yields a poly with 3 terms x^2 + g0*x + g1
    private static Polynomial reedSolomonBuildGenerator(Field field, int nroots_U, byte firstConsecutiveRoot_U, int rootGap_U, Polynomial generator, byte[] roots_U) {
        for (int i_U = 0; Integer.compareUnsigned(i_U, nroots_U) < 0; i_U++) {
            roots_U[i_U] = field.exp_U[Integer.remainderUnsigned(rootGap_U * (i_U + Byte.toUnsignedInt(firstConsecutiveRoot_U)), 255)];
        }
        return polynomialCreateFromRoots(field, nroots_U, roots_U);

    }
    /* correct_reed_solomon_create allocates and initializes an
     * encoder/decoder for a given reed solomon error correction
     * code. The block size must be 255 bytes with 8-bit symbols.
     *
     * This block can repair corrupted bytes. It can handle as
     * many as num_roots/2 bytes having corruption and still recover
     * the encoded payload. However, using more num_roots
     * adds more parity overhead and substantially increases
     * the computational time for decoding.
     *
     * primitive_polynomial should be one of the given values in this
     * file. Sane values for first_consecutive_root and
     * generator_root_gap are 1 and 1. Not all combinations of
     * values produce valid codes.
     */

    public CorrectReedSolomon(short primitivePolynomial_U, byte fConsecutiveRoot_U, byte gRootGap_U, long numRoots_U) {
        field = new Field(primitivePolynomial_U);
        blockLength_U = 255;
        minDistance_U = numRoots_U;

        messageLength_U = blockLength_U - minDistance_U;
        firstConsecutiveRoot_U = fConsecutiveRoot_U;
        generatorRootGap_U = gRootGap_U;
        generatorRoots_U = new byte[(int) minDistance_U];

        generator = reedSolomonBuildGenerator(field, (int) minDistance_U, firstConsecutiveRoot_U, generatorRootGap_U, generator, generatorRoots_U);

        encodedPolynomial = new Polynomial((int) (blockLength_U - 1));
        encodedRemainder = new Polynomial((int) (blockLength_U - 1));

        hasInitDecode = false;
    }

    public void correctReedSolomonDebugPrint() {
        for (int i_U = 0; Integer.compareUnsigned(i_U, 256) < 0; i_U++) {
            System.out.printf("%3d  %3d    %3d  %3d\n", i_U, Byte.toUnsignedInt(field.exp_U[i_U]), i_U, Byte.toUnsignedInt(field.log_U[i_U]));
        }
        System.out.println();

        System.out.print("roots: ");
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            System.out.print(Byte.toUnsignedInt(generatorRoots_U[i_U]));
            if (Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U - 1) < 0) {
                System.out.print(", ");
            }
        }
        System.out.println('\n');

        System.out.print("generator: ");
        for (int i_U = 0; Integer.compareUnsigned(i_U, generator.getOrder() + 1) < 0; i_U++) {
            System.out.print(Byte.toUnsignedInt(generator.getCoeff(i_U)) + "*x^" + i_U);
            if (Integer.compareUnsigned(i_U, generator.getOrder()) < 0) {
                System.out.print(" + ");
            }
        }
        System.out.println('\n');

        System.out.print("generator (alpha format): ");
        for (int i_U = generator.getOrder() + 1; Integer.compareUnsigned(i_U, 0) > 0; i_U--) {
            System.out.print("alpha^" + Byte.toUnsignedInt(field.log_U[Byte.toUnsignedInt(generator.getCoeff(i_U - 1))]) + "*x^" + (i_U - 1));
            if (Integer.compareUnsigned(i_U, 1) > 0) {
                System.out.print(" + ");
            }
        }
        System.out.println("\n");

        System.out.print("remainder: ");
        boolean hasPrinted = false;
        for (int i_U = 0; Integer.compareUnsigned(i_U, encodedRemainder.getOrder() + 1) < 0; i_U++) {
            if (encodedRemainder.getCoeff(i_U) == 0) {
                continue;
            }
            if (hasPrinted) {
                System.out.print(" + ");
            }
            hasPrinted = true;
            System.out.print(Byte.toUnsignedInt(errorLocator.getCoeff(i_U)) + "*x^" + i_U);
        }
        System.out.println("\n");

        System.out.print("syndromes: ");
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            System.out.print(Byte.toUnsignedInt(syndromes_U[i_U]));
            if (Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U - 1) < 0) {
                System.out.print(", ");
            }
        }
        System.out.println("\n");

        System.out.println("numerrors: " + errorLocator.getOrder() + "\n");

        System.out.print("error locator: ");
        hasPrinted = false;
        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder() + 1) < 0; i_U++) {
            if (errorLocator.getCoeff(i_U) == 0) {
                continue;
            }
            if (hasPrinted) {
                System.out.print(" + ");
            }
            hasPrinted = true;
            System.out.print(Byte.toUnsignedInt(errorLocator.getCoeff(i_U)) + "*x^" + i_U);
        }
        System.out.println("\n");

        System.out.print("error roots: ");
        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) < 0; i_U++) {
            System.out.print(polynomialEval(field, errorLocator, errorRoots_U[i_U]) + "@" + Byte.toUnsignedInt(errorRoots_U[i_U]));
            if (Integer.compareUnsigned(i_U, errorLocator.getOrder() - 1) < 0) {
                System.out.print(", ");
            }
        }
        System.out.println("\n");

        hasPrinted = false;
        for(int i_U = 0; Integer.compareUnsigned(i_U, errorEvaluator.getOrder()) < 0; i_U++) {
            if(errorEvaluator.getCoeff(i_U) == 0) {
                continue;
            }
            if(hasPrinted) {
                System.out.print(" + ");
            }
            hasPrinted = true;
            System.out.print(Byte.toUnsignedInt(errorEvaluator.getCoeff(i_U)) + "*x^" + i_U);
        }
        System.out.println("\n");

        hasPrinted = false;
        for(int i_U = 0; Integer.compareUnsigned(i_U, errorLocatorDerivative.getOrder()) < 0; i_U++) {
            if(errorLocatorDerivative.getCoeff(i_U) == 0) {
                continue;
            }
            if(hasPrinted) {
                System.out.print(" + ");
            }
            hasPrinted = true;
            System.out.print(Byte.toUnsignedInt(errorLocatorDerivative.getCoeff(i_U)) + "*x^" + i_U);
        }
        System.out.println("\n");

        System.out.print("error locator: ");
        for(int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) < 0; i_U++) {
            System.out.print(Byte.toUnsignedInt(errorVals_U[i_U]) + "@" + Byte.toUnsignedInt(errorLocations_U[i_U]));
            if(Integer.compareUnsigned(i_U, errorLocator.getOrder() - 1) < 0) {
                System.out.print(", ");
            }
        }
        System.out.println("\n");

    }

    /* correct_reed_solomon_encode uses the rs instance to encode
     * parity information onto a block of data. msg_length should be
     * no more than the payload size for one block e.g. no more
     * than 223 for a (255, 223) code. Shorter blocks will be encoded
     * with virtual padding where the padding is not emitted.
     *
     * encoded should be at least msg_length + parity length bytes long
     *
     * It is allowable for msg and encoded to be the same pointer. In
     * that case, the parity bytes will be written after the msg bytes
     * end.
     *
     * This function returns the number of bytes written to encoded.
     */

    public long correctReedSolomonEncode(byte[] msg_U, long msgLength_U, byte[] encoded_U) {
        if (Long.compareUnsigned(msgLength_U, messageLength_U) > 0) {
            return -1;
        }

        long padLength_U = messageLength_U - msgLength_U;

        // 0-fill the rest of the coefficients -- this length will always be > 0
        // because the order of this poly is block_length and the msg_length <= message_length
        // e.g. 255 and 223
        encodedPolynomial.flushCoeff();

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            // message goes from high order to low order but libcorrect polynomials go low to high
            // so we reverse on the way in and on the way out
            // we'd have to do a copy anyway so this reversal should be free
            encodedPolynomial.setCoeff((int) (Integer.toUnsignedLong(encodedPolynomial.getOrder()) - (Integer.toUnsignedLong(i_U) + padLength_U)), msg_U[i_U]);
        }

        Polynomial.polynomialMod(field, encodedPolynomial, generator, encodedRemainder);

        // now return byte order to highest order to lowest order
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            encoded_U[i_U] = encodedPolynomial.getCoeff((int) (Integer.toUnsignedLong(encodedPolynomial.getOrder()) - (Integer.toUnsignedLong(i_U) + padLength_U)));
        }

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            encoded_U[(int) (msgLength_U + Integer.toUnsignedLong(i_U))] = encodedRemainder.getCoeff((int) (minDistance_U - Integer.toUnsignedLong(i_U + 1)));
        }

        return blockLength_U;

    }


    /* correct_reed_solomon_decode uses the rs instance to decode
     * a payload from a block containing payload and parity bytes.
     * This function can recover in spite of some bytes being corrupted.
     *
     * In most cases, if the block is too corrupted, this function
     * will return -1 and not perform decoding. It is possible but
     * unlikely that the payload written to msg will contain
     * errors when this function returns a positive value.
     *
     * msg should be long enough to contain a decoded payload for
     * this encoded block.
     *
     * This function returns a positive number of bytes written to msg
     * if it has decoded or -1 if it has encountered an error.
     */

    public long correctReedSolomonDecode(byte[] encoded_U, long encodedLength_U, byte[] msg_U) {
        if (Long.compareUnsigned(encodedLength_U, blockLength_U) > 0) {
            return -1;
        }

        // the message is the non-remainder part
        long msgLength_U = encodedLength_U - minDistance_U;
        // if they handed us a nonfull block, we'll write in 0s
        long padLength_U = blockLength_U - encodedLength_U;

        if (!hasInitDecode) {
            // initialize rs for decoding
            correctReedSolomonDecoderCreate();
        }

        // we need to copy to our local buffer
        // the buffer we're given has the coordinates in the wrong direction
        // e.g. byte 0 corresponds to the 254th order coefficient
        // so we're going to flip and then write padding
        // the final copied buffer will look like
        // | rem (rs->min_distance) | msg (msg_length) | pad (pad_length) |

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), encodedLength_U) < 0; i_U++) {
            receivedPolynomial.setCoeff(i_U, encoded_U[(int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1))]);
        }

        // fill the pad_length with 0s
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), padLength_U) < 0; i_U++) {
            receivedPolynomial.setCoeff((int) (Integer.toUnsignedLong(i_U) + encodedLength_U), (byte) 0);
        }


        boolean allZero = reedSolomonFindSyndromes(field, receivedPolynomial, generatorRootExp_U, syndromes_U, minDistance_U);

        if (allZero) {
            // syndromes were all zero, so there was no error in the message
            // copy to msg and we are done
            for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
                msg_U[i_U] = receivedPolynomial.getCoeff((int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1)));
                return msgLength_U;
            }
        }
        int order_U = reedSolomonFindErrorLocator(0);
        // XXX fix this vvvv
        errorLocator.setOrder(order_U);

        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) <= 0; i_U++) {
            // this is a little strange since the coeffs are logs, not elements
            // also, we'll be storing log(0) = 0 for any 0 coeffs in the error locator
            // that would seem bad but we'll just be using this in chien search, and we'll skip all 0 coeffs
            // (you might point out that log(1) also = 0, which would seem to alias. however, that's ok,
            //   because log(1) = 255 as well, and in fact that's how it's represented in our log table)
            errorLocatorLog.setCoeff(i_U, field.log_U[Byte.toUnsignedInt(errorLocator.getCoeff(i_U))]);
        }
        errorLocatorLog.setOrder(errorLocator.getOrder());
        if (!reedSolomonFactorizeErrorLocator(field, 0, errorLocatorLog, errorRoots_U, elementExp_U)) {
            // roots couldn't be found, so there were too many errors to deal with
            // RS has failed for this message
            return -1;
        }

        reedSolomonFindErrorLocations(field, generatorRootGap_U, errorRoots_U, errorLocations_U, errorLocator.getOrder(), 0);
        reedSolomonFindErrorValues();

        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) < 0; i_U++) {
            receivedPolynomial.setCoeff(Byte.toUnsignedInt(errorLocations_U[i_U]),
                    field.fieldSub(receivedPolynomial.getCoeff(Byte.toUnsignedInt(errorLocations_U[i_U])), errorVals_U[i_U]));
        }

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            msg_U[i_U] = receivedPolynomial.getCoeff((int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1)));
        }
        return msgLength_U;
    }

    /* correct_reed_solomon_decode_with_erasures uses the rs
     * instance to decode a payload from a block containing payload
     * and parity bytes. Additionally, the user can provide the
     * indices of bytes which have been suspected to be corrupted.
     * This erasure information is typically provided by a demodulating
     * or receiving device. This function can recover with
     * some additional errors on top of the erasures.
     *
     * In order to successfully decode, the quantity
     * (num_erasures + 2*num_errors) must be less than
     * num_roots.
     *
     * erasure_locations shold contain erasure_length items.
     * erasure_length should not exceed the number of parity
     * bytes encoded into this block.
     *
     * In most cases, if the block is too corrupted, this function
     * will return -1 and not perform decoding. It is possible but
     * unlikely that the payload written to msg will contain
     * errors when this function returns a positive value.
     *
     * msg should be long enough to contain a decoded payload for
     * this encoded block.
     *
     * This function returns a positive number of bytes written to msg
     * if it has decoded or -1 if it has encountered an error.
     */
    public long correctReedSolomonDecodeWithErasures(byte[] encoded_U, long encodedLength_U, byte[] erasureLocations_U, long erasureLength_U, byte[] msg_U) {
        if (erasureLength_U == 0) {
            return correctReedSolomonDecode(encoded_U, encodedLength_U, msg_U);
        }

        if (Long.compareUnsigned(encodedLength_U, blockLength_U) > 0) {
            return -1;
        }
        if (Long.compareUnsigned(erasureLength_U, minDistance_U) > 0) {
            return -1;
        }

        // the message is the non-remainder part
        long msgLength_U = encodedLength_U - minDistance_U;
        // if they handed us a nonfull block, we'll write in 0s
        long padLength_U = blockLength_U - encodedLength_U;

        if (!hasInitDecode) {
            correctReedSolomonDecoderCreate();
        }

        // we need to copy to our local buffer
        // the buffer we're given has the coordinates in the wrong direction
        // e.g. byte 0 corresponds to the 254th order coefficient
        // so we're going to flip and then write padding
        // the final copied buffer will look like
        // | rem (rs->min_distance) | msg (msg_length) | pad (pad_length) |

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), encodedLength_U) < 0; i_U++) {
            receivedPolynomial.setCoeff(i_U, encoded_U[(int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1))]);
        }

        // fill the pad_length with 0s
        for (int i_U = 0; Integer.toUnsignedLong(i_U) < padLength_U; i_U++) {
            receivedPolynomial.setCoeff((int) (Integer.toUnsignedLong(i_U) + encodedLength_U), (byte) 0);
        }

        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), erasureLength_U) < 0; i_U++) {
            // remap the coordinates of the erasures
            errorLocations_U[i_U] = (byte) (blockLength_U - (Byte.toUnsignedLong(erasureLocations_U[i_U]) + padLength_U + 1));
        }

        reedSolomonFindErrorRootsFromLocations(field, generatorRootGap_U, errorLocations_U, errorRoots_U, (int) erasureLength_U);

        erasureLocator = reedSolomonFindErrorLocatorFromRoots(field, (int) erasureLength_U, errorRoots_U, erasureLocator, initFromRootsScratch);

        boolean allZero = reedSolomonFindSyndromes(field, receivedPolynomial, generatorRootExp_U, syndromes_U, minDistance_U);

        if (allZero) {
            // syndromes were all zero, so there was no error in the message
            // copy to msg and we are done
            for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
                msg_U[i_U] = receivedPolynomial.getCoeff((int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1)));
                return msgLength_U;
            }
        }

        reedSolomonFindModifiedSyndromes(syndromes_U, erasureLocator, modifiedSyndromes_U);

        byte[] syndromeCopy_U = Arrays.copyOf(syndromes_U, (int) minDistance_U);

        for (int i_U = (int) erasureLength_U; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            syndromes_U[(int) (Integer.toUnsignedLong(i_U) - erasureLength_U)] = modifiedSyndromes_U[i_U];
        }

        int order_U = reedSolomonFindErrorLocator(erasureLength_U);
        // XXX fix this vvvv
        errorLocator.setOrder(order_U);

        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) <= 0; i_U++) {
            // this is a little strange since the coeffs are logs, not elements
            // also, we'll be storing log(0) = 0 for any 0 coeffs in the error locator
            // that would seem bad but we'll just be using this in chien search, and we'll skip all 0 coeffs
            // (you might point out that log(1) also = 0, which would seem to alias. however, that's ok,
            //   because log(1) = 255 as well, and in fact that's how it's represented in our log table)
            errorLocatorLog.setCoeff(i_U, field.log_U[(Byte.toUnsignedInt(errorLocator.getCoeff(i_U)))]);
        }
        errorLocatorLog.setOrder(errorLocator.getOrder());

    /*
    for (unsigned int i = 0; i < erasure_length; i++) {
        rs->error_roots[i] = field_div(rs->field, 1, rs->error_roots[i]);
    }
    */

        if (!reedSolomonFactorizeErrorLocator(field, (int) erasureLength_U, errorLocator, errorRoots_U, elementExp_U)) {
            // roots couldn't be found, so there were too many errors to deal with
            // RS has failed for this message
            return -1;
        }

        Polynomial tempPoly = new Polynomial((int) (Integer.toUnsignedLong(errorLocator.getOrder()) + erasureLength_U));
        polynomialMul(field, erasureLocator, errorLocator, tempPoly);
        Polynomial placeholderPoly = errorLocator.clone();
        errorLocator = tempPoly.clone();

        reedSolomonFindErrorLocations(field, generatorRootGap_U, errorRoots_U, errorLocations_U, errorLocator.getOrder(), (int) erasureLength_U);

        syndromes_U = Arrays.copyOf(syndromeCopy_U, (int) minDistance_U);
        reedSolomonFindErrorValues();

        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) < 0; i_U++) {
            receivedPolynomial.setCoeff(Byte.toUnsignedInt(errorLocations_U[i_U]),
                    field.fieldSub(receivedPolynomial.getCoeff(Byte.toUnsignedInt(errorLocations_U[i_U])), errorVals_U[i_U]));
        }

        errorLocator = placeholderPoly;
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), msgLength_U) < 0; i_U++) {
            msg_U[i_U] = receivedPolynomial.getCoeff((int) (encodedLength_U - Integer.toUnsignedLong(i_U + 1)));
        }
        return msgLength_U;
    }



    // calculate all syndromes of the received polynomial at the roots of the generator
// because we're evaluating at the roots of the generator, and because the transmitted
//   polynomial was made to be a product of the generator, we know that the transmitted
//   polynomial is 0 at these roots
// any nonzero syndromes we find here are the values of the error polynomial evaluated
//   at these roots, so these values give us a window into the error polynomial. if
//   these syndromes are all zero, then we can conclude the error polynomial is also
//   zero. if they're nonzero, then we know our message received an error in transit.
// returns true if syndromes are all zero
    private static boolean reedSolomonFindSyndromes(Field field, Polynomial msgpoly, byte[][] generatorRootExp_U, byte[] syndromes_U, long minDistance_U) {
        boolean allZero = true;
        Arrays.fill(syndromes_U, 0, (int) minDistance_U, (byte) 0);
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            // profiling reveals that this function takes about 50% of the cpu time of
            // decoding. so, in order to speed it up a little, we precompute and save
            // the successive powers of the roots of the generator, which are
            // located in generator_root_exp
            byte eval_U = msgpoly.polynomialEvalLut(field, generatorRootExp_U[i_U]);
            if (eval_U != 0) {
                allZero = false;
            }
            syndromes_U[i_U] = eval_U;
        }
        return allZero;
    }

    // Berlekamp-Massey algorithm to find LFSR that describes syndromes
// returns number of errors and writes the error locator polynomial to rs->error_locator
    private int reedSolomonFindErrorLocator(long numErasures_U) {
        int numerrors_U = 0;
        errorLocator.flushCoeff();

        // initialize to f(x) = 1
        errorLocator.setCoeff(0, (byte) 1);
        errorLocator.setOrder(0);

        lastErrorLocator = errorLocator.clone();

        byte discrepancy_U;
        byte lastDiscrepancy_U = 1;
        int delayLength_U = 1;

        for (int i_U = errorLocator.getOrder(); Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U - numErasures_U) < 0; i_U++) {
            discrepancy_U = syndromes_U[i_U];
            for (int j_U = 1; Integer.compareUnsigned(j_U, numerrors_U) <= 0; j_U++) {
                discrepancy_U = (byte) field.fieldAdd(discrepancy_U, field.fieldMul(errorLocator.getCoeff(j_U), syndromes_U[i_U - j_U]));
            }

            if (discrepancy_U == 0) {
                // our existing LFSR describes the new syndrome as well
                // leave it as-is but update the number of delay elements
                //   so that if a discrepancy occurs later we can eliminate it
                delayLength_U++;
                continue;
            }

            if (Integer.compareUnsigned(2 * numerrors_U, i_U) <= 0) {
                // there's a discrepancy, but we still have room for more taps
                // lengthen LFSR by one tap and set weight to eliminate discrepancy

                // shift the last locator by the delay length, multiply by discrepancy,
                //   and divide by the last discrepancy
                // we move down because we're shifting up, and this prevents overwriting
                for (int j = lastErrorLocator.getOrder(); j >= 0; j--) {
                    // the bounds here will be ok since we have a headroom of numerrors
                    lastErrorLocator.setCoeff(j + delayLength_U, field.fieldDiv(
                            field.fieldMul(lastErrorLocator.getCoeff(j), discrepancy_U), lastDiscrepancy_U));
                }
                for (int j = delayLength_U - 1; j >= 0; j--) {
                    lastErrorLocator.setCoeff(j, (byte) 0);
                }

                // locator = locator - last_locator
                // we will also update last_locator to be locator before this loop takes place
                byte temp_U;
                for (int j = 0; Integer.compareUnsigned(j, lastErrorLocator.getOrder() + delayLength_U) <= 0; j++) {
                    temp_U = errorLocator.getCoeff(j);
                    errorLocator.setCoeff(j, (byte) field.fieldAdd(errorLocator.getCoeff(j), lastErrorLocator.getCoeff(j)));
                    lastErrorLocator.setCoeff(j, temp_U);
                }
                int tempOrder_U = errorLocator.getOrder();
                errorLocator.setOrder(lastErrorLocator.getOrder() + delayLength_U);
                lastErrorLocator.setOrder(tempOrder_U);

                // now last_locator is locator before we started,
                //   and locator is (locator - (discrepancy/last_discrepancy) * x^(delay_length) * last_locator)

                numerrors_U = i_U + 1 - numerrors_U;
                lastDiscrepancy_U = discrepancy_U;
                delayLength_U = 1;
                continue;
            }

            // no more taps
            // unlike the previous case, we are preserving last locator,
            //    but we'll update locator as before
            // we're basically flattening the two loops from the previous case because
            //    we no longer need to update last_locator
            for (int j_U = lastErrorLocator.getOrder(); j_U >= 0; j_U--) {
                errorLocator.setCoeff(j_U + delayLength_U, (byte) field.fieldAdd(errorLocator.getCoeff(j_U + delayLength_U),
                        field.fieldDiv(field.fieldMul(lastErrorLocator.getCoeff(j_U), discrepancy_U), lastDiscrepancy_U)));
            }
            errorLocator.setOrder(Integer.compareUnsigned(lastErrorLocator.getOrder() + delayLength_U, errorLocator.getOrder()) > 0 ?
                    lastErrorLocator.getOrder() + delayLength_U : errorLocator.getOrder());
            delayLength_U++;
        }
        return errorLocator.getOrder();
    }

    // find the roots of the error locator polynomial
    // Chien search
    public static boolean reedSolomonFactorizeErrorLocator(Field field, int numSkip_U, Polynomial locatorLog, byte[] roots_U, byte[][] elementExp_U) {
        // normally it'd be tricky to find all the roots
        // but, the finite field is awfully finite...
        // just brute force search across every field element
        int root_U = numSkip_U;
        for (int i = 0; i < locatorLog.getOrder(); i++) {
            roots_U[numSkip_U + i] = 0;
        }
        for (short i_U = 0; Short.toUnsignedInt(i_U) < 256; i_U++) {
            // we make two optimizations here to help this search go faster
            // a) we have precomputed the first successive powers of every single element
            //   in the field. we need at most n powers, where n is the largest possible
            //   degree of the error locator
            // b) we have precomputed the error locator polynomial in log form, which
            //   helps reduce some lookups that would be done here
            if (locatorLog.polynomialEvalLogLut(field, elementExp_U[Short.toUnsignedInt(i_U)]) == 0) {
                roots_U[root_U] = (byte) i_U;
                root_U++;
            }
        }
        // this is where we find out if we are have too many errors to recover from
        // berlekamp-massey may have built an error locator that has 0 discrepancy
        // on the syndromes but doesn't have enough roots
        return root_U == locatorLog.getOrder() + numSkip_U;
    }

    // use error locator and syndromes to find the error evaluator polynomial
    public static void reedSolomonFindErrorEvaluator(Field field, Polynomial locator, Polynomial syndromes, Polynomial errorEvaluator) {
        // the error evaluator, omega(x), is S(x)*Lamba(x) mod x^(2t)
        // where S(x) is a polynomial constructed from the syndromes
        //   S(1) + S(2)*x + ... + S(2t)*x(2t - 1)
        // and Lambda(x) is the error locator
        // the modulo is implicit here -- we have limited the max length of error_evaluator,
        //   which polynomial_mul will interpret to mean that it should not compute
        //   powers larger than that, which is the same as performing mod x^(2t)
        Polynomial.polynomialMul(field, locator, syndromes, errorEvaluator);
    }

    // use error locator, error roots and syndromes to find the error values
    // that is, the elements in the finite field which can be added to the received
    //   polynomial at the locations of the error roots in order to produce the
    //   transmitted polynomial
    // forney algorithm
    public void reedSolomonFindErrorValues() {
        // error value e(j) = -(X(j)^(1-c) * omega(X(j)^-1))/(lambda'(X(j)^-1))
        // where X(j)^-1 is a root of the error locator, omega(X) is the error evaluator,
        //   lambda'(X) is the first formal derivative of the error locator,
        //   and c is the first consecutive root of the generator used in encoding

        // first find omega(X), the error evaluator
        // we generate S(x), the polynomial constructed from the roots of the syndromes
        // this is *not* the polynomial constructed by expanding the products of roots
        // S(x) = S(1) + S(2)*x + ... + S(2t)*x(2t - 1)
        Polynomial syndromePoly = new Polynomial((int) (minDistance_U - 1), syndromes_U);
        errorEvaluator.flushCoeff();
        reedSolomonFindErrorEvaluator(field, errorLocator, syndromePoly, errorEvaluator);

        // now find lambda'(X)
        errorLocatorDerivative.setOrder(errorLocator.getOrder() - 1);
        polynomialFormalDerivative(field, errorLocator, errorLocatorDerivative);

        // calculate each e(j)
        for (int i_U = 0; Integer.compareUnsigned(i_U, errorLocator.getOrder()) < 0; i_U++) {
            if (Byte.toUnsignedInt(errorRoots_U[i_U]) == 0) {
                continue;
            }
            errorVals_U[i_U] = field.fieldMul(field.fieldPow(errorRoots_U[i_U],
                    Byte.toUnsignedInt(firstConsecutiveRoot_U) - 1), field.fieldDiv(errorEvaluator.polynomialEvalLut(field, elementExp_U[Byte.toUnsignedInt(errorRoots_U[i_U])]),
                    errorLocatorDerivative.polynomialEvalLut(field, elementExp_U[Byte.toUnsignedInt(errorRoots_U[i_U])])));
        }
    }

    public static void reedSolomonFindErrorLocations(Field field, byte generatorRootGap_U, byte[] errorRoots_U, byte[] errorLocations_U, int numErrors_U, int numSkip_U) {
        for (int i_U = 0; Integer.compareUnsigned(i_U, numErrors_U) < 0; i_U++) {
            // the error roots are the reciprocals of the error locations, so div 1 by them

            // we do mod 255 here because the log table aliases at index 1
            // the log of 1 is both 0 and 255 (alpha^255 = alpha^0 = 1)
            // for most uses it makes sense to have log(1) = 255, but in this case
            // we're interested in a byte index, and the 255th index is not even valid
            // just wrap it back to 0

            if (Byte.toUnsignedInt(errorRoots_U[i_U]) == 0) {
                continue;
            }

            short loc_U = field.fieldDiv((byte) 1, errorRoots_U[i_U]);
            for (short j_U = 0; Short.toUnsignedInt(j_U) < 256; j_U++) {
                if (field.fieldPow((byte) j_U, generatorRootGap_U) == Short.toUnsignedInt(loc_U)) {
                    errorLocations_U[i_U] = field.log_U[j_U];
                    break;
                }
            }
        }
    }

    // erasure method -- take given locations and convert to roots
// this is the inverse of reed_solomon_find_error_locations
    private static void reedSolomonFindErrorRootsFromLocations(Field field, byte generatorRootGap_U, byte[] errorLocations_U, byte[] errorRoots_U, int numErrors_U) {
        for (int i_U = 0; Integer.compareUnsigned(i_U, numErrors_U) < 0; i_U++) {
            byte loc_U = field.fieldPow(field.exp_U[Byte.toUnsignedInt(errorLocations_U[i_U])], generatorRootGap_U);
            // field_element_t loc = field.exp[error_locations[i]];
            errorRoots_U[i_U] = field.fieldDiv((byte) 1, loc_U);
        }

    }

    // erasure method -- given the roots of the error locator, create the polynomial
    private static Polynomial reedSolomonFindErrorLocatorFromRoots(Field field, int numErrors_U, byte[] errorRoots_U, Polynomial errorLocator, Polynomial[] scratch) {
        // multiply out roots to build the error locator polynomial
        return polynomialInitFromRoots(field, numErrors_U, errorRoots_U, errorLocator, scratch);
    }

    // erasure method
    private void reedSolomonFindModifiedSyndromes(byte[] syndromes_U, Polynomial errorLocator, byte[] modifiedSyndromes_U) {
        Polynomial syndromePoly = new Polynomial((int) (minDistance_U - 1), syndromes_U);
        Polynomial modifiedSyndromePoly = new Polynomial((int) (minDistance_U - 1), modifiedSyndromes_U);

        polynomialMul(field, errorLocator, syndromePoly, modifiedSyndromePoly);
    }

    public void correctReedSolomonDecoderCreate() {
        hasInitDecode = true;
        syndromes_U = new byte[(int) minDistance_U];
        modifiedSyndromes_U = new byte[(int) (2 * minDistance_U)];
        receivedPolynomial = new Polynomial((int) (blockLength_U - 1));
        errorLocator = new Polynomial((int) minDistance_U);
        errorLocatorLog = new Polynomial((int) minDistance_U);
        erasureLocator = new Polynomial((int) minDistance_U);


        errorRoots_U = new byte[(int) (2 * minDistance_U)];
        errorVals_U = new byte[(int) minDistance_U];
        errorLocations_U = new byte[(int) minDistance_U];

        lastErrorLocator = new Polynomial((int) minDistance_U);
        errorEvaluator = new Polynomial((int) minDistance_U);
        errorLocatorDerivative = new Polynomial((int) (minDistance_U - 1));

        // calculate and store the first block_length powers of every generator root
        // we would have to do this work in order to calculate the syndromes
        // if we save it, we can prevent the need to recalculate it on subsequent calls
        // total memory usage is min_distance * block_length bytes e.g. 32 * 255 ~= 8k
        generatorRootExp_U = new byte[(int) minDistance_U][];
        for (int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), minDistance_U) < 0; i_U++) {
            generatorRootExp_U[i_U] = new byte[(int) blockLength_U];
            polynomialBuildExpLut(field, generatorRoots_U[i_U], (int) (blockLength_U - 1), generatorRootExp_U[i_U]);
        }

        // calculate and store the first min_distance powers of every element in the field
        // we would have to do this for chien search anyway, and its size is only 256 * min_distance bytes
        // for min_distance = 32 this is 8k of memory, a pittance for the speedup we receive in exchange
        // we also get to reuse this work during error value calculation
        elementExp_U = new byte[256][];
        for (short i = 0; i < 256; i++) {
            elementExp_U[Short.toUnsignedInt(i)] = new byte[(int) minDistance_U];
            polynomialBuildExpLut(field, (byte) i, (int) (minDistance_U - 1), elementExp_U[Short.toUnsignedInt(i)]);
        }

        initFromRootsScratch[0] = new Polynomial((int) minDistance_U);
        initFromRootsScratch[1] = new Polynomial((int) minDistance_U);

    }
}
