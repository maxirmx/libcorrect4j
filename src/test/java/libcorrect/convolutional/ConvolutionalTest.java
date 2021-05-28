package libcorrect.convolutional;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static libcorrect.convolutional.Convolutional.correctConvR126Polynomial;


class ConvolutionalTest {
    public final static Random RANDOM = new Random(1);

    @Test
    void correctConvolutionalT1_1() {
        RANDOM.setSeed(System.currentTimeMillis());

        // n.b. the error rates below are at 5.0dB/4.5dB for order 6 polys
        //  and 4.5dB/4.0dB for order 7-9 polys. this can be easy to miss.
        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1, 2, 6, Float.POSITIVE_INFINITY, 0);
    }

    @Test
    void correctConvolutionalT1_2() {
        RANDOM.setSeed(System.currentTimeMillis());

        // n.b. the error rates below are at 5.0dB/4.5dB for order 6 polys
        //  and 4.5dB/4.0dB for order 7-9 polys. this can be easy to miss.
        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1_000_000, 2, 6, 5.0, 5e-06);
    }

    @Test
    void correctConvolutionalT1_3() {
        RANDOM.setSeed(System.currentTimeMillis());

        // n.b. the error rates below are at 5.0dB/4.5dB for order 6 polys
        //  and 4.5dB/4.0dB for order 7-9 polys. this can be easy to miss.
        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1_000_000, 2, 6, 4.5, 3e-05);
    }

}