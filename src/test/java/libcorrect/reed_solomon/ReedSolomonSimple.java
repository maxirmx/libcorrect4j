package libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static libcorrect.reed_solomon.CorrectReedSolomon.*;

public class ReedSolomonSimple {
    protected long minDistance = 4;

    @Test
    void rsTestPassThrough() {
        byte[] msgIn = "abcdef".getBytes();
        CorrectReedSolomon rs = new CorrectReedSolomon(correctRsPrimitivePolynomial_8_7_6_1_0, (byte)1, (byte)1, minDistance);
        byte[] encoded = rs.encode(msgIn);
        byte[] msgOut = rs.decode(encoded);
        assert Arrays.equals(msgOut,msgIn);

    }

    @Test
    void rsTestPassWithCorrection() {
        byte[] msgIn = "abcdef".getBytes();
        CorrectReedSolomon rs = new CorrectReedSolomon(correctRsPrimitivePolynomial_8_7_2_1_0, (byte)1, (byte)1, minDistance);
        byte[] encoded =  rs.encode(msgIn);
        encoded[3] = (byte) (encoded[3]+1);
        byte[] msgOut = rs.decode(encoded);
        assert Arrays.equals(msgOut,msgIn);

    }

}
