package de.dlr.ivf.tapas.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author sche_ai
 */
public class Checksum {

    private static final String HEXES = "0123456789ABCDEF";

    public static String generateFileChecksum(File file, HashType algorithm) throws NoSuchAlgorithmException, IOException {

        byte[] filebytes = Files.readAllBytes(file.toPath());
        byte[] hash = MessageDigest.getInstance(algorithm.getValue()).digest(filebytes);

        return getHex(hash);
    }

    static String getHex(byte[] raw) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public enum HashType {

        SHA512("SHA-512"), SHA256("SHA-256"), MD5("MD5");

        private final String value;

        HashType(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

    }

}
