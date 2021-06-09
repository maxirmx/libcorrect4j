package libcorrect.reed_solomon;

import static libcorrect.reed_solomon.CorrectReedSolomon.correctRsPrimitivePolynomialCcsds;

public class ReedSolomonTest {
    protected final long blockLength = 255;
    protected long minDistance;
    protected long messageLength;
    protected CorrectReedSolomon rs;
    protected RSTestBench testBench;
    
    protected void init(long mDistance) {
        minDistance = mDistance;
        messageLength = blockLength - minDistance;
        rs = new CorrectReedSolomon(correctRsPrimitivePolynomialCcsds, (byte)1, (byte)1, minDistance);
        testBench = new RSTestBench(blockLength, minDistance);
    }
  
    protected void runTests(long testMsgLength, long numErrors, long numErasures, long numIterations) {
        System.out.printf("testing reed solomon block length=%d, message length=%d, errors=%d, erasures=%d...",
                            blockLength, testMsgLength, numErrors, numErasures);
        for (int i = 0; i < numIterations; i++) {
            RsTestRun run = testBench.testRsErrors(rs, testMsgLength, numErrors, numErasures);
            assert run.getOutputMatches();
        }
        System.out.println("PASSED");

    }    
}
