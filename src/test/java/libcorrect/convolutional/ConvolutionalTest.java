package libcorrect.convolutional;

import org.junit.jupiter.api.Test;

import java.util.Random;


class ConvolutionalTest {
    public static long maxBlockLen_U = 4_096;
    public final static Random RANDOM = new Random(1);

    public static long testConv_U(AbstractData conv, AbstractData[] testbenchPtr, long msgLen_U, double ebN0, double bpskBitEnergy, double bpskVoltage) {
        byte[] msg_U = new byte[(int)maxBlockLen_U];

        long numErrors_U = 0;

        while(msgLen_U != 0) {
            long blockLen_U = Long.compareUnsigned(maxBlockLen_U, msgLen_U) < 0 ? maxBlockLen_U : msgLen_U;
            msgLen_U -= blockLen_U;

            for(int j_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(j_U), blockLen_U) < 0; j_U++) {
                msg_U[j_U] = (byte)(RANDOM.nextInt() % 256);
            }
        }
        return 0;
    }


    @Test
    void correctConvolutionalEncodeLen() {
    }


}