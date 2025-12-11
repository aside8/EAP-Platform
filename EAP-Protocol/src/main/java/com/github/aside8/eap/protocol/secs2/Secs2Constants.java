package com.github.aside8.eap.protocol.secs2;

public final class Secs2Constants {
    private Secs2Constants() {
        // Private constructor to prevent instantiation
    }

    public enum COMMACK {
        OK((byte) 0),
        DENIED((byte) 1),
        NOT_ACC_OTHER((byte) 2),
        ALREADY_OPEN((byte) 3);

        private final byte code;

        COMMACK(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }

    public enum OFLACK {
        OK((byte) 0),
        DENIED((byte) 1);

        private final byte code;

        OFLACK(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }

    public enum ONLACK {
        OK((byte) 0),
        DENIED((byte) 1);

        private final byte code;

        ONLACK(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }

    public enum ACKC5 {
        OK((byte) 0),
        ALARM_NOT_SET((byte) 1),
        ALREADY_SET((byte) 2),
        ALARM_NOT_EXIST((byte) 3);

        private final byte code;

        ACKC5(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }

    public enum ACKC6 {
        OK((byte) 0),
        DENIED((byte) 1);

        private final byte code;

        ACKC6(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }

    public enum ACKC10 {
        OK((byte) 0),
        TOO_LONG((byte) 1),
        CANNOT_PERFORM((byte) 2);

        private final byte code;

        ACKC10(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }
    }
}
