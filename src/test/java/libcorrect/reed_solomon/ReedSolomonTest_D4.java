package libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

public class ReedSolomonTest_D4 extends ReedSolomonTest {
    ReedSolomonTest_D4() {
        init(4);
    }
    
    @Test
    void RS_T_1() {
        runTests(Long.divideUnsigned(messageLength, 2), 0, 0, 20_000);
    }

    @Test
    void RS_T_2() {
        runTests(messageLength, 0, 0, 20_000);
    }

    @Test
    void RS_T_3() {
        runTests(Long.divideUnsigned(messageLength, 2), Long.divideUnsigned(minDistance, 2), 0, 20_000);
    }

    @Test
    void RS_T_4() {
        runTests(messageLength, Long.divideUnsigned(minDistance, 2), 0, 20_000);
    }

    @Test
    void RS_T_5() {
        runTests(Long.divideUnsigned(messageLength, 2), 0, minDistance, 20_000);
    }

    @Test
    void RS_T_6() {
        runTests(messageLength, 0, minDistance, 20_000);
    }

    @Test
    void RS_T_7() {
        runTests(Long.divideUnsigned(messageLength, 2), Long.divideUnsigned(minDistance, 4), Long.divideUnsigned(minDistance, 2), 20_000);
    }

    @Test
    void RS_T_8() {
        runTests(messageLength, Long.divideUnsigned(minDistance, 4), Long.divideUnsigned(minDistance, 2), 20_000);
    }

    
}
