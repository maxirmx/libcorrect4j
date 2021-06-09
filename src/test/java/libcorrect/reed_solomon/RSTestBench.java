package libcorrect.reed_solomon;

import java.util.Arrays;
import java.util.Random;

public class RSTestBench {
    private final long messageLength;
    private final long minDistance;
    private final int[] indices;
    private final byte[] erasureLocations;

    private final static Random RANDOM = new Random(1);

    public static void shuffle(int[] a, long len) {
        for (long i = 0; Long.compareUnsigned(i, len - 2) < 0; i++) {
            long j = Long.remainderUnsigned(RANDOM.nextInt(), len - i) + i;
            int temp = a[(int) i];
            a[(int) i] = a[(int) j];
            a[(int) j] = temp;
        }
    }

    public RSTestBench(long bLength, long mDistance) {

        messageLength = bLength - mDistance;
        minDistance = mDistance;
        indices = new int[(int) bLength];
        erasureLocations = new byte[(int) minDistance];
    }

    public RsTestRun testRsErrors(ReedSolomon test, long msgLength, long numErrors, long numErasures) {
        RsTestRun run = new RsTestRun();
        run.setOutputMatches(false);
        if(Long.compareUnsigned(msgLength, messageLength) > 0) {
            return run;
        }

        byte[] msg = new byte[(int) msgLength];
        byte[] recvMsg = new byte[(int) msgLength];

        for(long i = 0; Long.compareUnsigned(i, msgLength) < 0; i++) {
           msg[(int)i] = (byte)(RANDOM.nextInt() % 256);
        }
        long blockLength = msgLength + minDistance;


        byte[] encoded = test.encode(msg);
        byte[] corruptedEncoded = Arrays.copyOf(encoded, (int)blockLength);

        for(int i = 0; Long.compareUnsigned(i, blockLength) < 0; i++) {
            indices[i] = i;
        }
        shuffle(indices, blockLength);

        for(int i = 0; Long.compareUnsigned(Integer.toUnsignedLong(i), numErasures) < 0; i++) {
            int index = indices[i];
            byte corruptionMask = (byte)(RANDOM.nextInt() % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask);
            erasureLocations[i] = (byte)index;
        }

        for(int i = 0; Long.compareUnsigned(Integer.toUnsignedLong(i),numErrors) < 0; i++) {
            int index = indices[(int)(Integer.toUnsignedLong(i) + numErasures)];
            byte corruptionMask = (byte)(RANDOM.nextInt() % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask);
        }

        recvMsg = test.decodeWithErasures(encoded, erasureLocations);

        run.setOutputMatches(Arrays.equals(msg, recvMsg));

        return run;
    }




}
