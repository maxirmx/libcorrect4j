package libcorrect.reed_solomon;

import java.util.Random;

public class ReedSolomonTest {
    protected final static Random RANDOM = new Random(1);
    
    protected static void runTests(CorrectReedSolomon rs, RSTestbench testbench, long blockLength_U,
                                long testMsgLength_U, long numErrors_U, long numErasures_U, long numIterations_U) {
        System.out.printf("testing reed solomon block length=%d, message length=%d, errors=%d, erasures=%d...", blockLength_U, testMsgLength_U, numErrors_U, numErasures_U);
        for (long i_U = 0; Long.compareUnsigned(i_U, numIterations_U) < 0; i_U++) {
            RsTestRun run = testbench.testRsErrors(rs, testMsgLength_U, numErrors_U, numErasures_U);
            assert run.getOutputMatches();
            System.out.println("PASSED");
        }

    }    
}
