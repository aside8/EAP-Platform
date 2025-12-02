package com.github.aside8.eap.protocol.secs2;

import com.github.aside8.eap.protocol.Codec;

import java.nio.charset.Charset;

/**
 * Interface for all SECS-II message bodies.
 */
public interface SECSII extends Codec {
    // Specific SECS-II methods can be added here, e.g., to inspect data items.

    /**
     * Creates a SECS-II data item of type List.
     *
     * @param items the SECS-II data items to include in the list.
     * @return a SECSII object representing the List data item.
     */
    static SECSII list(SECSII... items) {
        return SecsDataItem.list(items);
    }

    /**
     * Creates a SECS-II data item of type Binary (B).
     *
     * @param values the byte values.
     * @return a SECSII object representing the Binary data item.
     */
    static SECSII binary(byte... values) {
        return SecsDataItem.binary(values);
    }

    /**
     * Creates a SECS-II data item of type Boolean (BOOLEAN).
     *
     * @param values the boolean values.
     * @return a SECSII object representing the Boolean data item.
     */
    static SECSII bool(boolean... values) {
        return SecsDataItem.bool(values);
    }

    /**
     * Creates a SECS-II data item of type ASCII (A).
     *
     * @param value the ASCII string value.
     * @return a SECSII object representing the ASCII data item.
     */
    static SECSII ascii(String value) {
        return SecsDataItem.ascii(value);
    }

    /**
     * Creates a SECS-II data item of type ASCII (A).
     *
     * @param value the ASCII string value.
     * @return a SECSII object representing the ASCII data item.
     */
    static SECSII ascii(String value, Charset charset) {
        return SecsDataItem.ascii(value, charset);
    }

    /**
     * Creates a SECS-II data item of type 8-byte Integer (I8).
     *
     * @param values the 8-byte integer values.
     * @return a SECSII object representing the I8 data item.
     */
    static SECSII int8(long... values) {
        return SecsDataItem.int8(values);
    }

    /**
     * Creates a SECS-II data item of type 1-byte Integer (I1).
     *
     * @param values the 1-byte integer values.
     * @return a SECSII object representing the I1 data item.
     */
    static SECSII int1(byte... values) {
        return SecsDataItem.int1(values);
    }

    /**
     * Creates a SECS-II data item of type 2-byte Integer (I2).
     *
     * @param values the 2-byte integer values.
     * @return a SECSII object representing the I2 data item.
     */
    static SECSII int2(short... values) {
        return SecsDataItem.int2(values);
    }

    /**
     * Creates a SECS-II data item of type 4-byte Integer (I4).
     *
     * @param values the 4-byte integer values.
     * @return a SECSII object representing the I4 data item.
     */
    static SECSII int4(int... values) {
        return SecsDataItem.int4(values);
    }

    /**
     * Creates a SECS-II data item of type 8-byte Floating Point (F8).
     *
     * @param values the 8-byte floating point values.
     * @return a SECSII object representing the F8 data item.
     */
    static SECSII float8(double... values) {
        return SecsDataItem.float8(values);
    }

    /**
     * Creates a SECS-II data item of type 4-byte Floating Point (F4).
     *
     * @param values the 4-byte floating point values.
     * @return a SECSII object representing the F4 data item.
     */
    static SECSII float4(float... values) {
        return SecsDataItem.float4(values);
    }

    /**
     * Creates a SECS-II data item of type 8-byte Unsigned Integer (U8).
     *
     * @param values the 8-byte unsigned integer values.
     * @return a SECSII object representing the U8 data item.
     */
    static SECSII uint8(long... values) {
        return SecsDataItem.uint8(values);
    }

    /**
     * Creates a SECS-II data item of type 1-byte Unsigned Integer (U1).
     *
     * @param values the 1-byte unsigned integer values.
     * @return a SECSII object representing the U1 data item.
     */
    static SECSII uint1(short... values) {
        return SecsDataItem.uint1(values);
    }

    /**
     * Creates a SECS-II data item of type 2-byte Unsigned Integer (U2).
     *
     * @param values the 2-byte unsigned integer values.
     * @return a SECSII object representing the U2 data item.
     */
    static SECSII uint2(int... values) {
        return SecsDataItem.uint2(values);
    }

    /**
     * Creates a SECS-II data item of type 4-byte Unsigned Integer (U4).
     *
     * @param values the unsigned 4-byte integer values.
     * @return a SECSII object representing the U4 data item.
     */
    static SECSII uint4(long... values) {
        return SecsDataItem.uint4(values);
    }
}