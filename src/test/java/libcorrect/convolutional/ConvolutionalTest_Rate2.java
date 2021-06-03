/*
 * libcorrect4j
 * ConvolutionalTest_Rate2
 * Created from tests/convolutional.c               @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static libcorrect.convolutional.Convolutional.*;


class ConvolutionalTest_Rate2 {
    public final static Random RANDOM = new Random(1);
    // n.b. the error rates below are at 5.0dB/4.5dB for order 6 polys
    //  and 4.5dB/4.0dB for order 7-9 polys. this can be easy to miss.

    @Test
    void correctConvolutionalT6_1() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1_000_000, 2, 6, Float.POSITIVE_INFINITY, 0);
    }

    @Test
    void correctConvolutionalT6_2() {
        RANDOM.setSeed(System.currentTimeMillis());
        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1_000_000, 2, 6, 5.0, 5e-06);
    }

    @Test
    void correctConvolutionalT6_3() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 6, correctConvR126Polynomial);

        assert tb.assertTestResult(conv, 1_000_000, 2, 6, 4.5, 3e-05);
    }


    @Test
    void correctConvolutionalT7_1() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 7, correctConvR127Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 7, Float.POSITIVE_INFINITY, 0);
    }

    @Test
    void correctConvolutionalT7_2() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 7, correctConvR127Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 7, 4.5, 1e-05);
    }

    @Test
    void correctConvolutionalT7_3() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 7, correctConvR127Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 7, 4.0, 5e-05);
    }

    @Test
    void correctConvolutionalT8_1() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 8, correctConvR128Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 8, Float.POSITIVE_INFINITY, 0);
    }

    @Test
    void correctConvolutionalT8_2() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 8, correctConvR128Polynomial);
        assert tb.assertTestResult(conv,  1_000_000, 2, 8, 4.5, 5e-06);
    }

    @Test
    void correctConvolutionalT8_3() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 8, correctConvR128Polynomial);
        assert tb.assertTestResult(conv,  1_000_000, 2, 8, 4.0, 3e-05);
    }

    @Test
    void correctConvolutionalT9_1() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 9, Float.POSITIVE_INFINITY, 0);
    }

    @Test
    void correctConvolutionalT9_2() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 9, 4.5, 3e-06);
    }

    @Test
    void correctConvolutionalT9_3() {
        RANDOM.setSeed(System.currentTimeMillis());

        ConvTestbench tb = new ConvTestbench();
        Convolutional conv = new Convolutional(2, 9, correctConvR129Polynomial);
        assert tb.assertTestResult(conv, 1_000_000, 2, 9, 4.0, 1e-05);
    }
}