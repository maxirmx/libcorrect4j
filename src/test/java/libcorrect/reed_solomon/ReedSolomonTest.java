package libcorrect.reed_solomon;

import java.util.Random;
import static libcorrect.reed_solomon.CorrectReedSolomon.correctRsPrimitivePolynomialCcsds;

public class ReedSolomonTest {
 //   protected final Random RANDOM = new Random(1);
    
    protected final long blockLength_U = 255;
    protected long minDistance_U;
    protected long messageLength_U;
    protected CorrectReedSolomon rs;
    protected RSTestbench testbench;
    
    protected void init(long mDistance) {
        minDistance_U = mDistance;
        messageLength_U = blockLength_U - minDistance_U;
//        RANDOM.setSeed(System.currentTimeMillis());
        rs = new CorrectReedSolomon(correctRsPrimitivePolynomialCcsds, (byte)1, (byte)1, minDistance_U);
        testbench = new RSTestbench(blockLength_U, minDistance_U);
    }
  
    protected void runTests(long testMsgLength_U, long numErrors_U, long numErasures_U, long numIterations_U) {
        System.out.printf("testing reed solomon block length=%d, message length=%d, errors=%d, erasures=%d...", blockLength_U, testMsgLength_U, numErrors_U, numErasures_U);
        for (long i_U = 0; Long.compareUnsigned(i_U, numIterations_U) < 0; i_U++) {
            RsTestRun run = testbench.testRsErrors(rs, testMsgLength_U, numErrors_U, numErasures_U);
            assert run.getOutputMatches();
        }
        System.out.println("PASSED");

    }    
}
