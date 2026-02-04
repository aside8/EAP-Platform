package com.github.aside8.eap.protocol.secs2.sml;

import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import com.github.aside8.eap.protocol.secs2.SECSII;
import com.github.aside8.eap.protocol.secs2.SecsFormatCode;

/**
 * Printer that renders SECS-II data items into the SML-like formatted string used by
 * the project (human-readable, multi-line, indented). This class contains the
 * canonical formatting logic — `SecsDataItem.toFormatString()` delegates here.
 */
public final class SmlPrinter {
    public static final SmlPrinter DEFAULT = new SmlPrinter();

    private final int indentSize = 2;

    public String toSml(SECSII item) {
        if (item == null) return "";
        StringBuilder sb = new StringBuilder();
        render(item, sb, 0);
        return sb.toString();
    }

    /**
     * Produce a compact, parser‑friendly SML string that {@link SmlParser} can parse back.
     * Example: L[ U4[0], A["OK"] ]
     */
    public String toMachineSml(SECSII item) {
        if (item == null) return "";
        StringBuilder sb = new StringBuilder();
        renderCompact(item, sb);
        return sb.toString();
    }

    private void render(SECSII item, StringBuilder sb, int indent) {
        if (item instanceof SecsDataItem sdi) {
            renderSecsDataItem(sdi, sb, indent);
        } else {
            sb.append(item.toString());
        }
    }

    private void renderSecsDataItem(SecsDataItem sdi, StringBuilder sb, int indent) {
        SecsFormatCode formatCode = sdi.getFormatCode();
        indent(sb, indent);
        sb.append("  ".repeat(Math.max(0, 0))); // preserve historical leading two-space behaviour
        sb.append("<").append(formatCode.getSymbol()).append(" [");

        if (formatCode == SecsFormatCode.LIST) {
            int size = (sdi.getList() != null) ? sdi.getList().size() : 0;
            sb.append(size).append("]");

            if (size == 0) {
                sb.append(">\n");
                return;
            }

            sb.append("\n");
            for (SECSII it : sdi.getList()) {
                if (it instanceof SecsDataItem) {
                    render(it, sb, indent + 1);
                } else {
                    render(it, sb, indent + 1);
                }
            }
            indent(sb, indent);
            sb.append(">\n");

        } else {
            int length = (sdi.getRawBytes() != null) ? sdi.getRawBytes().length : 0;
            int size = (formatCode.getSize() > 0 && length > 0) ? length / formatCode.getSize() : length;
            sb.append(size).append("] ");

            if (sdi.getRawBytes() != null) {
                switch (formatCode) {
                    case ASCII -> sb.append('"').append(sdi.getAscii()).append('"');
                    case INT1 -> sb.append(arrayToString(sdi.getInt1()));
                    case INT2 -> sb.append(arrayToString(sdi.getInt2()));
                    case INT4 -> sb.append(arrayToString(sdi.getInt4()));
                    case INT8 -> sb.append(arrayToString(sdi.getInt8()));
                    case UINT1 -> sb.append(arrayToString(sdi.getUint1()));
                    case UINT2 -> sb.append(arrayToString(sdi.getUint2()));
                    case UINT4 -> sb.append(arrayToString(sdi.getUint4()));
                    case UINT8 -> sb.append(arrayToString(sdi.getUint8()));
                    case FLOAT4 -> sb.append(arrayToString(sdi.getFloat4()));
                    case FLOAT8 -> sb.append(arrayToString(sdi.getFloat8()));
                    case BOOLEAN -> sb.append(arrayToString(sdi.getBoolean()));
                    case BINARY -> {
                        StringBuilder hex = new StringBuilder();
                        for (byte b : sdi.getRawBytes()) {
                            hex.append(String.format("0x%02X ", b));
                        }
                        sb.append(hex.toString().trim());
                    }
                    default -> sb.append("...");
                }
            }
            sb.append(">\n");
        }
    }

    private static String arrayToString(Object arr) {
        if (arr == null) return "";
        String s = java.util.Arrays.toString((Object[]) boxPrimitiveArray(arr));
        return s.replaceAll("[\\[\\],]", "").trim();
    }

    private static Object boxPrimitiveArray(Object arr) {
        // helper: convert primitive arrays to Object[] for Arrays.toString
        if (arr instanceof byte[] a) {
            Byte[] o = new Byte[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof short[] a) {
            Short[] o = new Short[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof int[] a) {
            Integer[] o = new Integer[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof long[] a) {
            Long[] o = new Long[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof float[] a) {
            Float[] o = new Float[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof double[] a) {
            Double[] o = new Double[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        if (arr instanceof boolean[] a) {
            Boolean[] o = new Boolean[a.length];
            for (int i = 0; i < a.length; i++) o[i] = a[i];
            return o;
        }
        return new Object[0];
    }

    private static void indent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) sb.append("  ");
    }

    private static String tokenForFormat(SecsFormatCode fmt) {
        return switch (fmt) {
            case ASCII -> "A";
            case BINARY -> "B";
            case BOOLEAN -> "BOOLEAN";
            case INT1 -> "I1";
            case INT2 -> "I2";
            case INT4 -> "I4";
            case INT8 -> "I8";
            case UINT1 -> "U1";
            case UINT2 -> "U2";
            case UINT4 -> "U4";
            case UINT8 -> "U8";
            case FLOAT4 -> "F4";
            case FLOAT8 -> "F8";
            case LIST -> "L";
            default -> fmt.name();
        };
    }

    /* ---------- compact SML renderer (parseable by SmlParser) ---------- */
    private void renderCompact(SECSII item, StringBuilder sb) {
        if (item instanceof SecsDataItem sdi) {
            SecsFormatCode fmt = sdi.getFormatCode();
            if (fmt == SecsFormatCode.LIST) {
                sb.append("L[");
                int i = 0;
                for (SECSII e : sdi.getList()) {
                    if (i++ > 0) sb.append(", ");
                    renderCompact(e, sb);
                }
                sb.append("]");
                return;
            }

            switch (fmt) {
                case ASCII -> sb.append("A[").append('"').append(sdi.getAscii()).append('"').append("]");
                case BINARY -> {
                    sb.append("B[");
                    byte[] b = sdi.getRawBytes();
                    for (int j = 0; j < b.length; j++) {
                        if (j > 0) sb.append(' ');
                        sb.append(String.format("0x%02X", b[j]));
                    }
                    sb.append("]");
                }
                case BOOLEAN -> {
                    sb.append("BOOLEAN[");
                    boolean[] bo = sdi.getBoolean();
                    for (int j = 0; j < bo.length; j++) {
                        if (j > 0) sb.append(' ');
                        sb.append(bo[j] ? "true" : "false");
                    }
                    sb.append("]");
                }
                case INT1, INT2, INT4, INT8, UINT1, UINT2, UINT4, UINT8 -> {
                    String token = tokenForFormat(fmt);
                    sb.append(token).append('[');
                    if (fmt.getSize() == 1) {
                        if (fmt == SecsFormatCode.UINT1) {
                            short[] arr = sdi.getUint1();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Short.toString(arr[j]));
                            }
                        } else {
                            byte[] arr = sdi.getInt1();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Byte.toString(arr[j]));
                            }
                        }
                    } else if (fmt.getSize() == 2) {
                        if (fmt == SecsFormatCode.UINT2) {
                            int[] arr = sdi.getUint2();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Integer.toString(arr[j]));
                            }
                        } else {
                            short[] arr = sdi.getInt2();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Short.toString(arr[j]));
                            }
                        }
                    } else if (fmt.getSize() == 4) {
                        if (fmt == SecsFormatCode.UINT4) {
                            long[] arr = sdi.getUint4();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Long.toString(arr[j]));
                            }
                        } else {
                            int[] arr = sdi.getInt4();
                            for (int j = 0; j < arr.length; j++) {
                                if (j > 0) sb.append(',').append(' ');
                                sb.append(Integer.toString(arr[j]));
                            }
                        }
                    } else if (fmt.getSize() == 8) {
                        long[] arr = sdi.getUint8();
                        for (int j = 0; j < arr.length; j++) {
                            if (j > 0) sb.append(',').append(' ');
                            sb.append(Long.toString(arr[j]));
                        }
                    }
                    sb.append(']');
                }
                case FLOAT4, FLOAT8 -> {
                    String symbol = fmt.name();
                    sb.append(symbol).append('[');
                    if (fmt == SecsFormatCode.FLOAT4) {
                        float[] arr = sdi.getFloat4();
                        for (int j = 0; j < arr.length; j++) {
                            if (j > 0) sb.append(',').append(' ');
                            sb.append(Float.toString(arr[j]));
                        }
                    } else {
                        double[] arr = sdi.getFloat8();
                        for (int j = 0; j < arr.length; j++) {
                            if (j > 0) sb.append(',').append(' ');
                            sb.append(Double.toString(arr[j]));
                        }
                    }
                    sb.append(']');
                }
                default -> sb.append("UNKNOWN[]");
            }
        } else {
            sb.append(item.toString());
        }
    }
}
