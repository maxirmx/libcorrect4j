# About

libcorrect4j is Java implementation ofForward Error Correction techniques. With libcorrect4j, you can add extra redundancy to data packets before transmitting them over unreliable channels. Upon receiving the packet, the encoded data can be decoded to retrieve the original information.

The library utilizes two key algorithms, Convolutional codes and Reed-Solomon, to achieve error correction. Convolutional codes are adept at handling constant background noise, while Reed-Solomon is effective in correcting burst errors. These algorithms have been instrumental in the field of telecommunications. In particular, libcorrect employs a Viterbi algorithm decoder for convolutional codes.

libcorrect4j is a high-performance library released under the MIT license. The author also aims for the library to serve as an educational resource for understanding the underlying algorithms.
