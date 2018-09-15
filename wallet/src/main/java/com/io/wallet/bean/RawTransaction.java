package com.io.wallet.bean;

import com.io.wallet.utils.StringUtils;

import org.bouncycastle.jcajce.provider.digest.SHA3;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class RawTransaction {

    public String txID;
    /**
     * version
     */
    public Integer version;
    /**
     * size
     */
    public Integer size;
    /**
     * time_range
     */
    public Integer timeRange;

    /**
     * status
     */
    public Integer fee;

    /**
     * List of specified inputs for a transaction.
     */
    public List<AnnotatedInput> inputs;

    /**
     * List of specified outputs for a transaction.
     */
    public List<AnnotatedOutput> outputs;


    public String toJson() {
        return StringUtils.serializer.toJson(this);
    }

    public static RawTransaction fromJson(String json) {
        return StringUtils.serializer.fromJson(json, RawTransaction.class);
    }

    public static RawTransaction fromSuccessRespon(String json) {
        Type responType = new ParameterizedTypeImpl(Respon.class, new Class[]{RawTransaction.class});
        Respon<RawTransaction> result = StringUtils.serializer.fromJson(json, responType);
        return result.data;
    }

    public static class AnnotatedInput {

        public String inputID;
        /**
         * address
         */
        private String address;

        /**
         * The number of units of the asset being issued or spent.
         */
        private long amount;

        /**
         * The definition of the asset being issued or spent (possibly null).
         */
        private Map<String, Object> assetDefinition;

        /**
         * The id of the asset being issued or spent.
         */
        private String assetId;

        /**
         * The control program which must be satisfied to transfer this output.
         */
        private String controlProgram;

        /**
         * The id of the output consumed by this input. Null if the input is an
         * issuance.
         */
        private String spentOutputId;

        /**
         * The type of the input.<br>
         * Possible values are "issue" and "spend".
         */
        private String type;

        @Override
        public String toString() {
            return StringUtils.serializer.toJson(this);
        }

    }

    public static class AnnotatedOutput {

        /**
         * address
         */
        private String address;

        /**
         * The number of units of the asset being controlled.
         */
        private long amount;

        /**
         * The definition of the asset being controlled (possibly null).
         */
        private Map<String, Object> assetDefinition;

        /**
         * The id of the asset being controlled.
         */
        public String assetId;

        /**
         * The control program which must be satisfied to transfer this output.
         */
        private String controlProgram;

        /**
         * The id of the output.
         */
        private String id;

        /**
         * The output's position in a transaction's list of outputs.
         */
        private Integer position;

        /**
         * The type the output.<br>
         * Possible values are "control" and "retire".
         */
        private String type;

    }

    public byte[] hashFn(byte[] hashedInputHex, byte[] txID) {
        SHA3.Digest256 digest256 = new SHA3.Digest256();
        // data = hashedInputHex + txID
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(hashedInputHex, 0, hashedInputHex.length);
        out.write(txID, 0, txID.length);
        byte[] data = out.toByteArray();

        return digest256.digest(data);
    }

}
