/*
 * libcorrect4j
 * ConvTestbench.java
 * Created from include/correct/util/error-sim.h
 *              util/error-sim.c
 *              tests/convolutional.c               @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;

import java.util.Arrays;
import java.util.Random;

public class ConvTestbench {
    public final static int maxBlockLen = 4_096;
    public final static Random RANDOM = new Random(1);


    private byte[] msgOut_U;
    private byte[] encoded_U;
    private double[] v;
    private double[] corrupted;
    private byte[] soft_U;
    private double[] noise;
    private long enclen_U;
    private long enclenBytes_U;
    private long msgLen_U;

    private Convolutional conv;


    public boolean assertTestResult(Convolutional c, long testLength_U, long rate_U, long order_U, double ebN0, double errorRate) {
        boolean res = true;
        conv = c;
        msgLen_U = testLength_U;
        double bpskVoltage = 1.0 / Math.sqrt(2.0);
        double bpskSymEnergy = 2 * Math.pow(bpskVoltage, 2.0);
        double bpskBitEnergy = bpskSymEnergy * Double.parseDouble(Long.toUnsignedString(rate_U));

        long errorCount_U = testConv(ebN0, bpskBitEnergy, bpskVoltage);
        double observedErrorRate = Double.parseDouble(Long.toUnsignedString(errorCount_U)) / (Double.parseDouble(Long.toUnsignedString(testLength_U)) * 8);
        if(observedErrorRate > errorRate) {
            System.out.printf("Test failed, expected error rate=%.2e, observed error rate=%.2e @%.1fdB for rate %d order %d\n", errorRate, observedErrorRate, ebN0, rate_U, order_U);
            res = false;
        } else {
            System.out.printf("Test passed, expected error rate=%.2e, observed error rate=%.2e @%.1fdB for rate %d order %d\n", errorRate, observedErrorRate, ebN0, rate_U, order_U);
        }
        return res;
    }

    private long testConv(double ebN0, double bpskBitEnergy, double bpskVoltage) {
        byte[] msg_U = new byte[(int) maxBlockLen];

        long numErrors_U = 0;

        while(msgLen_U != 0) {
            long blockLen_U = Long.compareUnsigned(maxBlockLen, msgLen_U) < 0 ? maxBlockLen : msgLen_U;
            msgLen_U -= blockLen_U;

            for(int j_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(j_U), blockLen_U) < 0; j_U++) {
                msg_U[j_U] = (byte)(RANDOM.nextInt() % 256);
            }

            resize(blockLen_U);

            ErrorSim.buildWhiteNoise(noise, enclen_U, ebN0, bpskBitEnergy);
            numErrors_U += testConvNoise(msg_U, blockLen_U, bpskVoltage);
        }
		return numErrors_U;
    }

    public int testConvNoise(byte[] msg_U, long nBytes_U, double bpskVoltage) {
        conv.correctConvolutionalEncode(msg_U, nBytes_U, encoded_U);
        ErrorSim.encodeBpsk(encoded_U, v, enclen_U, bpskVoltage);

        corrupted = Arrays.copyOf(v, (int)enclen_U);
        ErrorSim.addWhiteNoise(corrupted, noise, enclen_U);
        ErrorSim.decodeBpskSoft(corrupted, soft_U, enclen_U, bpskVoltage);

        Arrays.fill(msgOut_U, (byte) 0);

        long decodeLen = conv.correctConvolutionalDecodeSoft(soft_U,enclen_U, msgOut_U);

        if(decodeLen != nBytes_U) {
            System.out.printf("Expected to decode %d bytes, decoded %d bytes instead\n", nBytes_U, decodeLen);
            return -1;
        }

        return (int) ErrorSim.distance(msg_U, msgOut_U, nBytes_U);
    }

    public void resize(long msgLen_U) {


        if (msgOut_U != null) {
            msgOut_U = Arrays.copyOf(msgOut_U, (int) msgLen_U);
        } else {
            msgOut_U = new byte[(int) msgLen_U];
        }

        enclen_U = conv.correctConvolutionalEncodeLen(msgLen_U);
        enclenBytes_U = Long.remainderUnsigned(enclen_U, 8) != 0 ?
                                        Long.divideUnsigned(enclen_U, 8) + 1 :
                                        Long.divideUnsigned(enclen_U, 8);
        if (encoded_U != null) {
            encoded_U = Arrays.copyOf(encoded_U, (int)enclenBytes_U);
        } else {
            encoded_U = new byte[(int)enclenBytes_U];
        }
        if (v != null) {
            v = Arrays.copyOf(v, (int)enclen_U);
        } else {
            v = new double[(int)enclen_U];
        }
        if (corrupted != null) {
            corrupted = Arrays.copyOf(corrupted, (int)enclen_U);
        } else {
            corrupted = new double[(int)enclen_U];
        }
        if (noise != null) {
            noise = Arrays.copyOf(noise, (int)enclen_U);
        } else {
            noise = new double[(int)enclen_U];
        }
        if (soft_U != null) {
            soft_U = Arrays.copyOf(soft_U, (int)enclen_U);
        } else {
            soft_U = new byte[(int)enclen_U];
        }
    }

}
