package libcorrect.convolutional;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static libcorrect.convolutional.Convolutional.correctConvR129Polynomial;

public class ConvolutionalSimple {

    @Test
    void convTestPassThrough() {
        byte[] msgIn = "abcde".getBytes();
        byte[] encoded = new byte[100];
        byte[] msgOut = new byte[msgIn.length];
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        conv.encode(msgIn, msgIn.length, encoded);
        long enclen = encoded.length;
        long decodedLen = conv.decode(encoded,enclen, msgOut);
        assert decodedLen == msgIn.length;
        assert Arrays.equals(msgOut,msgIn);
    }

    @Test
    void convTestPassWithCorrection() {
        byte[] msgIn = "abcde".getBytes();
        byte[] encoded = new byte[100];
        byte[] msgOut = new byte[msgIn.length];
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        conv.encode(msgIn, msgIn.length, encoded);
        encoded[3] = (byte) (encoded[3]+1);
        long enclen = encoded.length;
        long decodedLen = conv.decode(encoded,enclen, msgOut);
        assert decodedLen == 5;
        assert Arrays.equals(msgOut,msgIn);
    }
}
