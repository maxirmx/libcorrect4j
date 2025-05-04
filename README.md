# libcorrect4j

<img width="96" alt="fec-small" align="left" src="https://github.com/maxirmx/libcorrect4j/assets/2081498/6630ceae-8cda-41fd-b361-cf549a0bf75b">

**libcorrect4j** is a Java library implementing Forward Error Correction (FEC) techniques, enabling the addition of redundancy to data packets for reliable transmission over noisy or unreliable channels.

This library is inspired by the [libcorrect](https://github.com/quiet/libcorrect) C library and offers a pure Java alternative.

libcorrect4j provides implementations of two key FEC algorithms:

- **Convolutional Codes** – Effective for correcting random errors in data streams, utilizing the Viterbi algorithm for decoding.
- **Reed-Solomon Codes** – Ideal for correcting burst errors, commonly used in storage and transmission systems.


## Features

- Pure Java implementation with no native dependencies.
- Supports both convolutional and Reed-Solomon codes.
- Suitable for educational purposes and production use.
- Released under the MIT License.
