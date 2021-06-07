package libcorrect.reed_solomon;

import java.util.Arrays;
import java.util.Random;

public class RSTestbench {
    private long blockLength_U;
    private long messageLength_U;
    private long minDistance_U;
    private byte[] msg_U;
    private byte[] encoded_U;
    private int[] indices;
    private byte[] corruptedEncoded_U;
    private byte[] erasureLocations_U;
    private byte[] recvmsg_U;

    private final static Random RANDOM = new Random(1);

    public static void shuffle(int[] a, long len) {
/*        for (long i = 0; Long.compareUnsigned(i, len - 2) < 0; i++) {
            long j = Long.remainderUnsigned(RANDOM.nextInt(), len - i) + i;
            int temp = a[(int) i];
            a[(int) i] = a[(int) j];
            a[(int) j] = temp;
        } */
    }

    public static void rsCorrectEncode(CorrectReedSolomon  encoder, byte[] msg_U, long msgLength_U, byte[] msgOut_U) {
        encoder.correctReedSolomonEncode(msg_U, msgLength_U, msgOut_U);
    }

    public static void rsCorrectDecode(CorrectReedSolomon decoder, byte[] encoded_U, long encodedLength_U, byte[] erasureLocations_U,
                                       long erasureLength_U, byte[] msg_U, long padLength_U, long numRoots_U) {
        decoder.correctReedSolomonDecodeWithErasures(encoded_U, encodedLength_U, erasureLocations_U, erasureLength_U, msg_U);
    }

    public RSTestbench (long bLength_U, long mDistance_U) {

        messageLength_U = bLength_U - mDistance_U;
        blockLength_U = bLength_U;
        minDistance_U = mDistance_U;
        msg_U = new byte[(int)messageLength_U];
        encoded_U =  new byte[(int)blockLength_U];
        indices = new int[(int)blockLength_U];

        corruptedEncoded_U = new byte[(int)blockLength_U];
        erasureLocations_U = new byte[(int)minDistance_U];
        recvmsg_U = new byte[(int)messageLength_U];
    }

    public RsTestRun testRsErrors(CorrectReedSolomon test, long msgLength_U, long numErrors_U, long numErasures_U) {
        RsTestRun run = new RsTestRun();
        run.setOutputMatches(false);
        if(Long.compareUnsigned(msgLength_U, messageLength_U) > 0) {
            return run;
        }
        for(long i_U = 0; Long.compareUnsigned(i_U, msgLength_U) < 0; i_U++) {
 //           msg_U[(int)i_U] = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 256);
            msg_U[(int)i_U] = (byte)((i_U & Integer.MAX_VALUE) % 256);
        }
        long blockLength_U = msgLength_U + minDistance_U;
        long padLength_U = messageLength_U - msgLength_U;

        rsCorrectEncode(test, msg_U, msgLength_U, encoded_U);

        corruptedEncoded_U = Arrays.copyOf(encoded_U, (int)blockLength_U);
        for(int i = 0; Long.compareUnsigned(i, blockLength_U) < 0; i++) {
            indices[i] = i;
        }
        shuffle(indices, blockLength_U);

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), numErasures_U) < 0; i_U++) {
            int index = indices[i_U];
//            byte corruptionMask_U = (byte)((RANDOM.nextInt() & Integer.MAX_VALUE) % 255 + 1);
            byte corruptionMask_U = (byte)((i_U & Integer.MAX_VALUE) % 255 + 1);
            corruptedEncoded_U[index] ^= Byte.toUnsignedInt(corruptionMask_U);
            erasureLocations_U[i_U] = (byte)index;
        }

        for(int i_U = 0; Long.compareUnsigned(Integer.toUnsignedLong(i_U), numErrors_U) < 0; i_U++) {
            int index = indices[(int)(Integer.toUnsignedLong(i_U) + numErasures_U)];
//            byte corruptionMask_U = (byte)(RANDOM.nextInt() % 255 + 1);
            byte corruptionMask_U = (byte)(i_U % 255 + 1);
            corruptedEncoded_U[index] ^= Byte.toUnsignedInt(corruptionMask_U);
        }

        rsCorrectDecode(test, corruptedEncoded_U, blockLength_U, erasureLocations_U, numErasures_U,
                        recvmsg_U, padLength_U, minDistance_U);

        run.setOutputMatches(Arrays.equals(msg_U, recvmsg_U));

        return run;
    }




}
