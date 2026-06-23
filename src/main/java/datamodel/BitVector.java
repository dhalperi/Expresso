package datamodel;

import java.util.Arrays;

public class BitVector {
    final byte[] bv;

    public BitVector(byte[] bv) {
        this.bv = bv;
    }

    public byte[] getBv() {
        return bv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitVector bitVector = (BitVector) o;
        return Arrays.equals(bv, bitVector.bv);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bv);
    }
}
