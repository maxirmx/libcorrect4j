package libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

public class ReedSolomonTest_D32 extends ReedSolomonTest {
    ReedSolomonTest_D32() {
        init(32);
    }

    @Test
    void RS_T_1() {
        runTests(Long.divideUnsigned(messageLength_U, 2), 0, 0, 20_000);
    }

    @Test
    void RS_T_2() {
        runTests(messageLength_U, 0, 0, 20_000);
    }

    @Test
    void RS_T_3() {
        runTests(Long.divideUnsigned(messageLength_U, 2), Long.divideUnsigned(minDistance_U, 2), 0, 20_000);
    }

    @Test
    void RS_T_4() {
        runTests(messageLength_U, Long.divideUnsigned(minDistance_U, 2), 0, 20_000);
    }

    @Test
    void RS_T_5() {
        runTests(Long.divideUnsigned(messageLength_U, 2), 0, minDistance_U, 20_000);
    }

    @Test
    void RS_T_6() {
        runTests(messageLength_U, 0, minDistance_U, 20_000);
    }

    @Test
    void RS_T_7() {
        runTests(Long.divideUnsigned(messageLength_U, 2), Long.divideUnsigned(minDistance_U, 4), Long.divideUnsigned(minDistance_U, 2), 20_000);
    }

    @Test
    void RS_T_8() {
        runTests(messageLength_U, Long.divideUnsigned(minDistance_U, 4), Long.divideUnsigned(minDistance_U, 2), 20_000);
    }

}
