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

    public static void rsCorrectEncode(CorrectReedSolomon  encoder, byte[] msg_U, long msgLength_U, byte[] msgOut_U) {
        encoder.encode(msg_U, msgLength_U, msgOut_U);
    }

    public static void rsCorrectDecode(CorrectReedSolomon decoder, byte[] encoded_U, long encodedLength_U, byte[] erasureLocations_U,
                                       long erasureLength_U, byte[] msg_U, long padLength_U, long numRoots_U) {
        decoder.decodeWithErasures(encoded_U, encodedLength_U, erasureLocations_U, erasureLength_U, msg_U);
    }

    public RSTestBench(long bLength_U, long mDistance_U) {

        messageLength = bLength_U - mDistance_U;
        minDistance = mDistance_U;
        msg = new byte[(int) messageLength];
        encoded =  new byte[(int) bLength_U];
        indices = new int[(int) bLength_U];

        corruptedEncoded = new byte[(int) bLength_U];
        erasureLocations = new byte[(int) minDistance];
        recvMsg = new byte[(int) messageLength];
    }

    public RsTestRun testRsErrors(CorrectReedSolomon test, long msgLength_U, long numErrors_U, long numErasures_U) {
        RsTestRun run = new RsTestRun();
        run.setOutputMatches(false);
        if(Long.compareUnsigned(msgLength_U, messageLength) > 0) {
            return run;
        }
        for(long i_U = 0; Long.compareUnsigned(i_U, msgLength_U) < 0; i_U++) {
           msg[(int)i_U] = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 256);
        }
        long blockLength_U = msgLength_U + minDistance;
        long padLength_U = messageLength - msgLength_U;

        rsCorrectEncode(test, msg, msgLength_U, encoded);

        corruptedEncoded = Arrays.copyOf(encoded, (int)blockLength_U);
        for(int i = 0; Long.compareUnsigned(i, blockLength_U) < 0; i++) {
            indices[i] = i;
        }
        shuffle(indices, blockLength_U);

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), numErasures_U) < 0; i_U++) {
            int index = indices[i_U];
            byte corruptionMask_U = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask_U);
            erasureLocations[i_U] = (byte)index;
        }

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U),numErrors_U) < 0; i_U++) {
            int index = indices[(int)(Integer.toUnsignedLong(i_U) + numErasures_U)];
            byte corruptionMask_U = (byte)(RANDOM.nextInt() % 255 + 1);
            corruptedEncoded[index] ^= Byte.toUnsignedInt(corruptionMask_U);
        }

        rsCorrectDecode(test, corruptedEncoded, blockLength_U, erasureLocations, numErasures_U,
                recvMsg, padLength_U, minDistance);

        run.setOutputMatches(Arrays.equals(msg, recvMsg));

        return run;
    }




}
