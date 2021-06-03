package libcorrect.convolutional;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static libcorrect.convolutional.Convolutional.correctConvR129Polynomial;

public class ConvolutionalSimple {

    @Test
    void convTestPassThrough() {
        byte[] msgIn = "abcdef".getBytes();
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        long enclen = conv.encodeLen(msgIn.length);
        byte[] encoded = conv.encode(msgIn);
        byte[] msgOut = conv.decode(encoded,enclen);
        assert msgOut.length == msgIn.length;
        assert Arrays.equals(msgOut,msgIn);
    }

    @Test
    void convTestPassWithCorrection() {
        byte[] msgIn = "abcdef".getBytes();
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        long enclen = conv.encodeLen(msgIn.length);
        byte[] encoded =conv.encode(msgIn);
        encoded[3] = (byte) (encoded[3]+1);
        byte[] msgOut = conv.decode(encoded, enclen);
        assert msgOut.length == msgIn.length;
        assert Arrays.equals(msgOut,msgIn);
    }
}
