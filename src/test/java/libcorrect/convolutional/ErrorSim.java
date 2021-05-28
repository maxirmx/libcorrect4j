/*
 * libcorrect4j
 * ConvTestbench.java
 * Created from /util/error-sim.c  @ https://github.com/quiet/libcorrect
 */

package libcorrect.convolutional;

import java.util.Random;
import static java.lang.Integer.bitCount;

public class ErrorSim {
    public final static Random RANDOM = new Random(1);

    public static long distance(byte[] a_U, byte[] b_U, long len_U) {
        long dist_U = 0;
        for(long i_U = 0; Long.compareUnsigned(i_U, len_U) < 0; i_U++) {
            if(a_U[(int)i_U] != b_U[(int)i_U]) {
            }
            dist_U += bitCount(Byte.toUnsignedInt(a_U[(int)i_U]) ^ Byte.toUnsignedInt(b_U[(int)i_U]));
        }
        return dist_U;
    }

    public static void gaussian(double[] res, long nRes_U, double sigma) {
        for(long i_U = 0; Long.compareUnsigned(i_U, nRes_U) < 0; i_U += 2) {
            // compute using polar method of box muller
            double s, u, v;
            while(true) {
                u = RANDOM.nextDouble();
                v = RANDOM.nextDouble();
                s = Math.pow(u, 2.0) + Math.pow(v, 2.0);

                if(s > Double.MIN_VALUE && s < 1) {
                    break;
                }
            }

            double base = Math.sqrt((-2.0 * Math.log(s)) / s);
            double z0 = u * base;
            res[(int)i_U] = z0 * sigma;

            if(Long.compareUnsigned(i_U + 1, nRes_U) < 0) {
                double z1 = v * base;
                res[(int)(i_U + 1)] = z1 * sigma;
            }
        }
    }

    public static void encodeBpsk(byte[] msg_U, double[] voltages, long nSyms_U, double bpskVoltage) {
        byte mask_U = (byte)0x80;
        for(long i_U = 0; Long.compareUnsigned(i_U, nSyms_U) < 0; i_U++) {
            voltages[(int)i_U] = (Byte.toUnsignedInt(msg_U[(int)Long.divideUnsigned(i_U, 8)]) & Byte.toUnsignedInt(mask_U)) != 0 ? bpskVoltage : -bpskVoltage;
            mask_U = (byte)(Byte.toUnsignedInt(mask_U) >> 1);
            if(mask_U == 0) {
                mask_U = (byte)0x80;
            }
        }
    }

    public static void byte2bit(byte[] bytes_U, byte[] bits_U, long nBits_U) {
        byte cmask_U = (byte)0x80;
        for(long i_U = 0; Long.compareUnsigned(i_U, nBits_U) < 0; i_U++) {
            bits_U[(int)i_U] = (byte)((Byte.toUnsignedInt(bytes_U[(int)Long.divideUnsigned(i_U, 8)]) & Byte.toUnsignedInt(cmask_U)) != 0 ? 255 : 0);
            cmask_U = (byte)(Byte.toUnsignedInt(cmask_U) >> 1);
            if(cmask_U == 0) {
                cmask_U = (byte)0x80;
            }
        }
    }

    public static void decodeBpsk(byte[] soft_U, byte[] msg_U, long nSyms_U) {
        byte mask_U = (byte)0x80;
        for(long i_U = 0; Long.compareUnsigned(i_U, nSyms_U) < 0; i_U++) {
            byte bit_U = (byte)(Byte.toUnsignedInt(soft_U[(int)i_U]) > 127 ? 1 : 0);
            if(bit_U != 0) {
                msg_U[(int)Long.divideUnsigned(i_U, 8)] |= mask_U;
            }
            mask_U = (byte)(Byte.toUnsignedInt(mask_U) >> 1);
            if(mask_U == 0) {
                mask_U = (byte)0x80;
            }
        }
    }

    public static void decodeBpskSoft(double[] voltages, byte[] soft_U, long nSyms_U, double bpskVoltage) {
        for(long i_U = 0; Long.compareUnsigned(i_U, nSyms_U) < 0; i_U++) {
            double rel = voltages[(int)i_U] / bpskVoltage;
            if(rel > 1) {
                soft_U[(int)i_U] = (byte)255;
            } else if(rel < -1) {
                soft_U[(int)i_U] = (byte)0;
            } else {
                soft_U[(int)i_U] = (byte)(127.5 + 127.5 * rel);
            }
        }
    }

    public static double log2amp(double l) {
        return Math.pow(10.0, l / 10.0);
    }

    public static double amp2log(double a) {
        return 10.0 * Math.log10(a);
    }

    public static double sigmaForEbN0(double ebN0, double bpskBitEnergy) {
        // eb/n0 is the ratio of bit energy to noise energy
        // eb/n0 is expressed in dB so first we convert to amplitude
        double ebN0Amp = log2amp(ebN0);
        // now the conversion. sigma^2 = n0/2 = ((eb/n0)^-1 * eb)/2 = eb/(2 * (eb/n0))
        return Math.sqrt(bpskBitEnergy / (2.0 * ebN0Amp));
    }

    public static void buildWhiteNoise(double[] noise, long nSyms_U, double ebN0, double bpskBitEnergy) {
        double sigma = sigmaForEbN0(ebN0, bpskBitEnergy);
        gaussian(noise, nSyms_U, sigma);
    }

    public static void addWhiteNoise(double[] signal, double[] noise, long nSyms_U) {
        double sqrt2 = Math.sqrt(2);
        for(long i_U = 0; Long.compareUnsigned(i_U, nSyms_U) < 0; i_U++) {
            // we want to add the noise in to the signal
            // but we can't add them directly, because they're expressed as magnitudes
            //   and the signal is real valued while the noise is complex valued

            // we'll assume that the noise is exactly half real, half imaginary
            // which means it forms a 90-45-45 triangle in the complex plane
            // that means that the magnitude we have here is sqrt(2) * the real valued portion
            // so, we'll divide by sqrt(2)
            // (we are effectively throwing away the complex portion)
            signal[(int)i_U] = signal[(int)i_U] + noise[(int)i_U] / sqrt2;
        }
    }

}
