package libcorrect.reed_solomon;

import java.util.Arrays;
import java.util.Random;

public class RSTestBench {
    private final long messageLength;
    private final long minDistance;
    private final byte[] msg;
    private final byte[] encoded;
    private final int[] indices;
    private byte[] corruptedEncoded;
    private final byte[] erasureLocations;
    private final byte[] recvMsg;

    private final static Random RANDOM = new Random(1);

    public static void shuffle(int[] a, long len) {
        for (long i = 0; Long.compareUnsigned(i, len - 2) < 0; i++) {
            long j = Long.remainderUnsigned(RANDOM.nextInt(), len - i) + i;
            int temp = a[(int) i];
            a[(int) i] = a[(int) j];
            a[(int) j] = temp;
        }
    }

    public static void rsCorrectEncode(CorrectReedSolomon  encoder, byte[] msg, long msgLength, byte[] msgOut) {
        encoder.encode(msg, msgLength, msgOut);
    }

    public static void rsCorrectDecode(CorrectReedSolomon decoder, byte[] encoded, long encodedLength, byte[] erasureLocations,
                                       long erasureLength, byte[] msg, long padLength, long numRoots) {
        decoder.decodeWithErasures(encoded, encodedLength, erasureLocations, erasureLength, msg);
    }

    public RSTestBench(long bLength, long mDistance) {

        messageLength = bLength - mDistance;
        minDistance = mDistance;
        msg = new byte[(int) messageLength];
        encoded =  new byte[(int) bLength];
        indices = new int[(int) bLength];

        corruptedEncoded = new byte[(int) bLength];
        erasureLocations = new byte[(int) minDistance];
        recvMsg = new byte[(int) messageLength];
    }

    public RsTestRun testRsErrors(CorrectReedSolomon test, long msgLength, long numErrors, long numErasures) {
        RsTestRun run = new RsTestRun();
        run.setOutputMatches(false);
        if(Long.compareUnsigned(msgLength, messageLength) > 0) {
            return run;
        }
        for(long i = 0; Long.compareUnsigned(i, msgLength) < 0; i++) {
           msg[(int)i] = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 256);
        }
        long blockLength = msgLength + minDistance;
        long padLength = messageLength - msgLength;

        rsCorrectEncode(test, msg, msgLength, encoded);

        corruptedEncoded = Arrays.copyOf(encoded, (int)blockLength);
        for(int i = 0; Long.compareUnsigned(i, blockLength) < 0; i++) {
            indices[i] = i;
        }
        shuffle(indices, blockLength);

        for(int i = 0; Long.compareUnsigned(Integer.toUnsignedLong(i), numErasures) < 0; i++) {
            int index = indices[i];
            byte corruptionMask = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask);
            erasureLocations[i] = (byte)index;
        }

        for(int i = 0; Long.compareUnsigned(Integer.toUnsignedLong(i),numErrors) < 0; i++) {
            int index = indices[(int)(Integer.toUnsignedLong(i) + numErasures)];
            byte corruptionMask = (byte)(RANDOM.nextInt() % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask);
        }

        rsCorrectDecode(test, corruptedEncoded, blockLength, erasureLocations, numErasures,
                recvMsg, padLength, minDistance);

        run.setOutputMatches(Arrays.equals(msg, recvMsg));

        return run;
    }




}
