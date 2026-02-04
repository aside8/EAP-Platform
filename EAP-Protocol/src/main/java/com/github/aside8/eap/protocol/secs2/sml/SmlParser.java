package com.github.aside8.eap.protocol.secs2.sml;

import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import com.github.aside8.eap.protocol.secs2.SecsFormatCode;
import com.github.aside8.eap.protocol.secs2.SECSII;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Minimal, safe PoC SML parser -> SecsDataItem.
 *
 * Supported subset (explicit forms only):
 * - A["ascii text"]
 * - B[0x01 0x02 ...] (hex bytes, space or comma separated)
 * - U1[1,2], U2[...], U4[...], U8[...] (unsigned integers)
 * - I1/I2/I4/I8 (signed integers)
 * - F4/F8 (floats/doubles)
 * - BOOLEAN[1 0 true false]
 * - L[ <item> <item> ... ] (lists)
 *
 * Safety: enforces depth/size limits from SmlOptions and reports position on error.
 */
public final class SmlParser {
    private final String in;
    private final SmlOptions opts;
    private int p;
    private int totalBytes;

    public SmlParser(String in, SmlOptions opts) {
        this.in = in == null ? "" : in;
        this.opts = opts == null ? SmlOptions.DEFAULT : opts;
        this.p = 0;
        this.totalBytes = 0;
    }

    public static SECSII parseItem(String sml) {
        return new SmlParser(sml, null).parse();
    }

    public static SECSII parseItem(String sml, SmlOptions opts) {
        return new SmlParser(sml, opts).parse();
    }

    private SECSII parse() {
        skipWs();
        if (eof()) throw error("empty input");
        SecsDataItem item = parseItem(0);
        skipWs();
        if (!eof()) throw error("trailing data");
        return item;
    }

    private SecsDataItem parseItem(int depth) {
        if (depth > opts.maxListDepth) throw error("list depth exceeded");
        skipWs();
        if (eof()) throw error("unexpected end of input while expecting item");
        char t = peek();
        switch (t) {
            case 'A' -> {
                expect('A');
                expect('[');
                String s = parseQuotedString();
                expect(']');
                totalBytes += s.getBytes().length;
                if (totalBytes > opts.maxTotalBytes) throw error("total bytes exceed limit");
                return SecsDataItem.ascii(s);
            }
            case 'B' -> {
                expect('B');
                expect('[');
                byte[] bytes = parseHexBytes();
                expect(']');
                totalBytes += bytes.length;
                if (totalBytes > opts.maxTotalBytes) throw error("total bytes exceed limit");
                return SecsDataItem.binary(bytes);
            }
            case 'L' -> {
                expect('L');
                expect('[');
                ArrayList<SECSII> items = new ArrayList<>();
                while (true) {
                    skipWsAndCommas();
                    if (peek() == ']') break;
                    if (items.size() >= opts.maxElementsPerList) throw error("list element count exceeded");
                    items.add(parseItem(depth + 1));
                    skipWsAndCommas();
                    if (eof()) throw error("unterminated list");
                }
                expect(']');
                return SecsDataItem.list(items.toArray(new SECSII[0]));
            }
            default -> {
                // numeric / typed forms: e.g. U4[1,2,3]
                String token = parseToken();
                if (token == null || token.isEmpty()) throw error("expected type token");
                String up = token.toUpperCase(Locale.ROOT);
                if (up.startsWith("U") || up.startsWith("I") || up.startsWith("F") || up.equals("BOOLEAN")) {
                    expect('[');
                    String inner = readUntil(']');
                    expect(']');
                    return buildPrimitive(up, inner.trim());
                }
                throw error("unsupported type: " + token);
            }
        }
    }

    private SecsDataItem buildPrimitive(String type, String content) {
        if (type.equals("BOOLEAN")) {
            String[] parts = splitElems(content);
            byte[] vals = new byte[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String v = parts[i].toLowerCase(Locale.ROOT);
                if (v.equals("true")) vals[i] = 1;
                else if (v.equals("false")) vals[i] = 0;
                else if (v.equals("0")) vals[i] = 0;
                else if (v.equals("1")) vals[i] = 1;
                else throw errorAt("invalid boolean value: " + parts[i]);
            }
            return SecsDataItem.bool(toBooleanArray(vals));
        }

        if (type.startsWith("U") || type.startsWith("I")) {
            String[] parts = splitElems(content);
            int width = Integer.parseInt(type.substring(1));
            switch (width) {
                case 1 -> {
                    if (type.startsWith("U")) {
                        short[] out = new short[parts.length];
                        for (int i = 0; i < parts.length; i++) out[i] = (short) parseLongWithHex(parts[i]);
                        return SecsDataItem.uint1(out);
                    } else {
                        byte[] out = new byte[parts.length];
                        for (int i = 0; i < parts.length; i++) out[i] = (byte) parseLongWithHex(parts[i]);
                        return SecsDataItem.int1(out);
                    }
                }
                case 2 -> {
                    if (type.startsWith("U")) {
                        int[] out = new int[parts.length];
                        for (int i = 0; i < parts.length; i++) out[i] = (int) parseLongWithHex(parts[i]);
                        return SecsDataItem.uint2(out);
                    } else {
                        short[] out = new short[parts.length];
                        for (int i = 0; i < parts.length; i++) out[i] = (short) parseLongWithHex(parts[i]);
                        return SecsDataItem.int2(out);
                    }
                }
                case 4 -> {
                    int[] out = new int[parts.length];
                    for (int i = 0; i < parts.length; i++) out[i] = (int) parseLongWithHex(parts[i]);
                    if (type.startsWith("U")) {
                        long[] lu = new long[out.length];
                        for (int i = 0; i < out.length; i++) lu[i] = out[i] & 0xFFFFFFFFL;
                        return SecsDataItem.uint4(lu);
                    }
                    return SecsDataItem.int4(out);
                }
                case 8 -> {
                    long[] out = new long[parts.length];
                    for (int i = 0; i < parts.length; i++) out[i] = parseLongWithHex(parts[i]);
                    if (type.startsWith("U")) return SecsDataItem.uint8(out);
                    return SecsDataItem.int8(out);
                }
                default -> throw error("unsupported integer width: " + width);
            }
        }

        if (type.startsWith("F")) {
            int width = Integer.parseInt(type.substring(1));
            String[] parts = splitElems(content);
            if (width == 4) {
                float[] out = new float[parts.length];
                for (int i = 0; i < parts.length; i++) out[i] = Float.parseFloat(parts[i]);
                return SecsDataItem.float4(out);
            } else {
                double[] out = new double[parts.length];
                for (int i = 0; i < parts.length; i++) out[i] = Double.parseDouble(parts[i]);
                return SecsDataItem.float8(out);
            }
        }

        throw error("unsupported primitive type: " + type);
    }

    /* ---------- token/lexing helpers ---------- */
    private String parseToken() {
        skipWs();
        int s = p;
        while (!eof()) {
            char c = peek();
            if (Character.isLetterOrDigit(c)) { p++; continue; }
            break;
        }
        return in.substring(s, p);
    }

    private String readUntil(char terminator) {
        int s = p;
        while (!eof() && peek() != terminator) p++;
        if (eof()) throw error("unterminated '" + terminator + "' sequence");
        return in.substring(s, p);
    }

    private String[] splitElems(String s) {
        if (s.trim().isEmpty()) return new String[0];
        return s.trim().split("[\t\n\r ,]+");
    }

    private long parseLongWithHex(String tok) {
        String t = tok.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("0x")) return Long.parseUnsignedLong(t.substring(2), 16);
        return Long.parseLong(t);
    }

    private byte[] parseHexBytes() {
        String inner = readUntil(']');
        // do not consume ']' here; caller will expect it
        String[] parts = splitElems(inner);
        ArrayList<Byte> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) continue;
            if (s.startsWith("0x") || s.matches("[0-9A-Fa-f]{2}")) {
                String hex = s.startsWith("0x") ? s.substring(2) : s;
                if (hex.length() % 2 != 0) throw errorAt("invalid hex byte: " + s);
                for (int i = 0; i < hex.length(); i += 2) {
                    int b = Integer.parseInt(hex.substring(i, i + 2), 16);
                    out.add((byte) b);
                }
            } else {
                throw errorAt("invalid hex token: " + s);
            }
        }
        byte[] arr = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
        return arr;
    }

    private char peek() {
        return in.charAt(p);
    }

    private void expect(char c) {
        skipWs();
        if (eof() || in.charAt(p) != c) throw error("expected '" + c + "'");
        p++;
    }

    private void skipWs() {
        while (!eof() && Character.isWhitespace(in.charAt(p))) p++;
    }

    private void skipWsAndCommas() {
        while (!eof()) {
            char c = in.charAt(p);
            if (Character.isWhitespace(c) || c == ',') p++;
            else break;
        }
    }

    private boolean eof() {
        return p >= in.length();
    }

    private String parseQuotedString() {
        skipWs();
        if (eof() || in.charAt(p) != '"') throw error("expected quoted string");
        p++; // consume '"'
        StringBuilder sb = new StringBuilder();
        while (!eof()) {
            char c = in.charAt(p++);
            if (c == '\\') {
                if (eof()) throw error("unterminated escape");
                char e = in.charAt(p++);
                if (e == '"') sb.append('"');
                else if (e == 'n') sb.append('\n');
                else if (e == 'r') sb.append('\r');
                else if (e == 't') sb.append('\t');
                else if (e == '\\') sb.append('\\');
                else throw errorAt("unsupported escape: \\" + e);
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        throw error("unterminated string");
    }

    private String parseTokenRest() {
        int s = p;
        while (!eof() && !Character.isWhitespace(peek()) && peek() != ',' && peek() != ']') p++;
        return in.substring(s, p);
    }

    /* ---------- errors ---------- */
    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException(msg + " at " + pos());
    }

    private IllegalArgumentException errorAt(String msg) {
        return new IllegalArgumentException(msg + " at " + pos());
    }

    private String pos() {
        int line = 1, col = 1;
        for (int i = 0; i < Math.min(p, in.length()); i++) {
            if (in.charAt(i) == '\n') { line++; col = 1; } else col++;
        }
        return "line=" + line + ",col=" + col + ",idx=" + p;
    }

    private static boolean[] toBooleanArray(byte[] b) {
        boolean[] r = new boolean[b.length];
        for (int i = 0; i < b.length; i++) r[i] = b[i] != 0;
        return r;
    }
}
