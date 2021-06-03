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

    public static long distance(byte[] a, byte[] b, long len) {
        long dist = 0;
        for(long i = 0; Long.compareUnsigned(i, len) < 0; i++) {
            if(a[(int)i] != b[(int)i]) {
            }
            dist += bitCount(Byte.toUnsignedInt(a[(int)i]) ^ Byte.toUnsignedInt(b[(int)i]));
        }
        return dist;
    }

    public static void gaussian(double[] res, long nRes, double sigma) {
        for(long i = 0; Long.compareUnsigned(i, nRes) < 0; i += 2) {
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
            res[(int)i] = z0 * sigma;

            if(Long.compareUnsigned(i + 1, nRes) < 0) {
                double z1 = v * base;
                res[(int)(i + 1)] = z1 * sigma;
            }
        }
    }

    public static void encodeBpsk(byte[] msg, double[] voltages, long nSyms, double bpskVoltage) {
        byte mask = (byte)0x80;
        for(long i = 0; Long.compareUnsigned(i, nSyms) < 0; i++) {
            voltages[(int)i] = (Byte.toUnsignedInt(msg[(int)Long.divideUnsigned(i, 8)]) & Byte.toUnsignedInt(mask)) != 0 ? bpskVoltage : -bpskVoltage;
            mask = (byte)(Byte.toUnsignedInt(mask) >> 1);
            if(mask == 0) {
                mask = (byte)0x80;
            }

        }
    }

    public static void byte2bit(byte[] bytes, byte[] bits, long nBits) {
        byte cmask = (byte)0x80;
        for(long i = 0; Long.compareUnsigned(i, nBits) < 0; i++) {
            bits[(int)i] = (byte)((Byte.toUnsignedInt(bytes[(int)Long.divideUnsigned(i, 8)]) & Byte.toUnsignedInt(cmask)) != 0 ? 255 : 0);
            cmask = (byte)(Byte.toUnsignedInt(cmask) >> 1);
            if(cmask == 0) {
                cmask = (byte)0x80;
            }
        }
    }

    public static void decodeBpsk(byte[] soft, byte[] msg, long nSyms) {
        byte mask = (byte)0x80;
        for(long i_U = 0; Long.compareUnsigned(i_U, nSyms) < 0; i_U++) {
            byte bit_U = (byte)(Byte.toUnsignedInt(soft[(int)i_U]) > 127 ? 1 : 0);
            if(bit_U != 0) {
                msg[(int)Long.divideUnsigned(i_U, 8)] |= mask;
            }
            mask = (byte)(Byte.toUnsignedInt(mask) >> 1);
            if(mask == 0) {
                mask = (byte)0x80;
            }
        }
    }

    public static void decodeBpskSoft(double[] voltages, byte[] soft, long nSyms, double bpskVoltage) {
        for(long i = 0; Long.compareUnsigned(i, nSyms) < 0; i++) {
            double rel = voltages[(int)i] / bpskVoltage;
            if(rel > 1) {
                soft[(int)i] = (byte)255;
            } else if(rel < -1) {
                soft[(int)i] = (byte)0;
            } else {
                soft[(int)i] = (byte)(127.5 + 127.5 * rel);
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

    public static void buildWhiteNoise(double[] noise, long nSyms, double ebN0, double bpskBitEnergy) {
        double sigma = sigmaForEbN0(ebN0, bpskBitEnergy);
        gaussian(noise, nSyms, sigma);
    }

    public static void addWhiteNoise(double[] signal, double[] noise, long nSyms) {
        double sqrt2 = Math.sqrt(2);
        for(long i = 0; Long.compareUnsigned(i, nSyms) < 0; i++) {
            // we want to add the noise in to the signal
            // but we can't add them directly, because they're expressed as magnitudes
            //   and the signal is real valued while the noise is complex valued

            // we'll assume that the noise is exactly half real, half imaginary
            // which means it forms a 90-45-45 triangle in the complex plane
            // that means that the magnitude we have here is sqrt(2) * the real valued portion
            // so, we'll divide by sqrt(2)
            // (we are effectively throwing away the complex portion)
            signal[(int)i] = signal[(int)i] + noise[(int)i] / sqrt2;
        }
    }

}
