package libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static libcorrect.reed_solomon.CorrectReedSolomon.correctRsPrimitivePolynomialCcsds;

public class ReedSolomonTest_D32 extends ReedSolomonTest {
    void ReadSolominTest_32() {
        init(32);
    }

    @Test
    void RS_T_1() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, Long.divideUnsigned(messageLength_U, 2), 0, 0, 20_000);
    }

    @Test
    void RS_T_2() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, messageLength_U, 0, 0, 20_000);
    }

    @Test
    void RS_T_3() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, Long.divideUnsigned(messageLength_U, 2), Long.divideUnsigned(minDistance_U, 2), 0, 20_000);
    }

    @Test
    void RS_T_4() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, messageLength_U, Long.divideUnsigned(minDistance_U, 2), 0, 20_000);
    }

    @Test
    void RS_T_5() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, Long.divideUnsigned(messageLength_U, 2), 0, minDistance_U, 20_000);
    }

    @Test
    void RS_T_6() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, messageLength_U, 0, minDistance_U, 20_000);
    }

    @Test
    void RS_T_7() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, Long.divideUnsigned(messageLength_U, 2), Long.divideUnsigned(minDistance_U, 4), Long.divideUnsigned(minDistance_U, 2), 20_000);
    }

    @Test
    void RS_T_8() {
        RANDOM.setSeed(System.currentTimeMillis());
        runTests(rs, testbench, blockLength_U, messageLength_U, Long.divideUnsigned(minDistance_U, 4), Long.divideUnsigned(minDistance_U, 2), 20_000);
    }

}
