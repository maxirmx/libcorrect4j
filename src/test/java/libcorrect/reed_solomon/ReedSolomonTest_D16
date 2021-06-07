ackage libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static libcorrect.reed_solomon.CorrectReedSolomon.correctRsPrimitivePolynomialCcsds;

public class ReedSolomonTest_D16 {
    private final static Random RANDOM = new Random(1);
    private final long blockLength_U = 255;
    private final long minDistance_U = 32;
    private final long messageLength_U = blockLength_U - minDistance_U;
    private CorrectReedSolomon rs = new CorrectReedSolomon(correctRsPrimitivePolynomialCcsds, (byte)1, (byte)1, minDistance_U);
    private RSTestbench testbench = new RSTestbench(blockLength_U, minDistance_U);
    
}
