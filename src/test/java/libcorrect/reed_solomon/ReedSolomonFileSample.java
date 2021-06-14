package libcorrect.reed_solomon;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static libcorrect.reed_solomon.ReedSolomon.*;

public class ReedSolomonFileSample {

    boolean testAgainstSample(String inputFileName, String sampleFileName, short polynomial, long minDistance) {
        try {
            File i = new File(inputFileName);
            File s = new File(sampleFileName);
            byte[] input = Files.readAllBytes(i.toPath());
            byte[] sample = Files.readAllBytes(s.toPath());
            ReedSolomon rs = new ReedSolomon(polynomial, (byte) 1, (byte) 1, minDistance);
            byte[] encoded = rs.encode(input);
            return Arrays.equals(sample, encoded);
        } catch (Exception ignored) {

        }
        return false;
    }

    @Test
    void rsTestSample() {
        final long minDistance = 16;
        String inputFileName = this.getClass().getResource("/gupshup_audio_input.txt").getPath();
        String sampleFileName = this.getClass().getResource("/gupshup_audio_encoded.txt").getPath();

        assert testAgainstSample(inputFileName, sampleFileName,correctRsPrimitivePolynomialCcsds, minDistance);
    }


}
