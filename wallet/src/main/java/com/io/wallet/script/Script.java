package com.io.wallet.script;

import com.google.common.collect.Lists;
import com.io.wallet.utils.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.io.wallet.script.ScriptOpCodes.OP_0;
import static com.io.wallet.script.ScriptOpCodes.OP_1;
import static com.io.wallet.script.ScriptOpCodes.OP_16;
import static com.io.wallet.script.ScriptOpCodes.OP_1NEGATE;
import static com.io.wallet.script.ScriptOpCodes.OP_CHECKMULTISIG;
import static com.io.wallet.script.ScriptOpCodes.OP_CHECKMULTISIGVERIFY;
import static com.io.wallet.script.ScriptOpCodes.OP_CHECKSIG;
import static com.io.wallet.script.ScriptOpCodes.OP_CHECKSIGVERIFY;
import static com.io.wallet.script.ScriptOpCodes.OP_DUP;
import static com.io.wallet.script.ScriptOpCodes.OP_EQUAL;
import static com.io.wallet.script.ScriptOpCodes.OP_EQUALVERIFY;
import static com.io.wallet.script.ScriptOpCodes.OP_HASH160;
import static com.io.wallet.script.ScriptOpCodes.OP_INVALIDOPCODE;
import static com.io.wallet.script.ScriptOpCodes.OP_PUSHDATA1;
import static com.io.wallet.script.ScriptOpCodes.OP_PUSHDATA2;
import static com.io.wallet.script.ScriptOpCodes.OP_PUSHDATA4;

// TODO: Make this class a superclass with derived classes giving accessor methods for the various common templates.

/**
 * <p>Programs embedded inside transactions that control redemption of payments.</p>
 * <p/>
 * <p>Bitcoin transactions don't specify what they do directly. Instead <a href="https://en.bitcoin.it/wiki/Script">a
 * small binary stack language</a> is used to define programs that when evaluated return whether the transaction
 * "accepts" or rejects the other transactions connected to it.</p>
 * <p/>
 * <p>In SPV mode, scripts are not run, because that would require all transactions to be available and lightweight
 * clients don't have that data. In full mode, this class is used to run the interpreted language. It also has
 * static methods for building scripts.</p>
 */
public class Script {
    public static final long MAX_SCRIPT_ELEMENT_SIZE = 520;  // bytes
    public static final int SIG_SIZE = 75;

    // The program is a set of chunks where each element is either [opcode] or [data, data, data ...]
    protected List<ScriptChunk> chunks;
    // Unfortunately, scripts are not ever re-serialized or canonicalized when used in signature hashing. Thus we
    // must preserve the exact bytes that we read off the wire, along with the parsed form.
    protected byte[] program;

    // Creation time of the associated keys in seconds since the epoch.
    private long creationTimeSeconds;

    /**
     * Creates an empty script that serializes to nothing.
     */
    private Script() {
        chunks = Lists.newArrayList();
    }

    // Used from ScriptBuilder.
    Script(List<ScriptChunk> chunks) {
        this.chunks = Collections.unmodifiableList(new ArrayList<ScriptChunk>(chunks));
        creationTimeSeconds = StringUtils.currentTimeSeconds();
    }

    /**
     * Construct a Script that copies and wraps the programBytes array. The array is parsed and checked for syntactic
     * validity.
     *
     * @param programBytes Array of program bytes from a transaction.
     */
    public Script(byte[] programBytes) throws ScriptException {
        program = programBytes;
        parse(programBytes);
        creationTimeSeconds = StringUtils.currentTimeSeconds();
    }

    public Script(byte[] programBytes, long creationTimeSeconds) throws ScriptException {
        program = programBytes;
        parse(programBytes);
        this.creationTimeSeconds = creationTimeSeconds;
    }

    public long getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    public void setCreationTimeSeconds(long creationTimeSeconds) {
        this.creationTimeSeconds = creationTimeSeconds;
    }

    /**
     * Returns the program opcodes as a string, for example "[1234] DUP HASH160"
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (ScriptChunk chunk : chunks)
            buf.append(chunk).append(' ');
        if (buf.length() > 0)
            buf.setLength(buf.length() - 1);
        return buf.toString();
    }

    /**
     * Returns the serialized program as a newly created byte array.
     */
    public byte[] getProgram() {
        try {
            // Don't round-trip as Satoshi's code doesn't and it would introduce a mismatch.
            if (program != null)
                return Arrays.copyOf(program, program.length);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (ScriptChunk chunk : chunks) {
                chunk.write(bos);
            }
            program = bos.toByteArray();
            return program;
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /**
     * Returns an immutable list of the scripts parsed form.
     */
    public List<ScriptChunk> getChunks() {
        return Collections.unmodifiableList(chunks);
    }

    private static final ScriptChunk STANDARD_TRANSACTION_SCRIPT_CHUNKS[];

    static {
        STANDARD_TRANSACTION_SCRIPT_CHUNKS = new ScriptChunk[]{
                new ScriptChunk(OP_DUP, null, 0),
                new ScriptChunk(OP_HASH160, null, 1),
                new ScriptChunk(OP_EQUALVERIFY, null, 23),
                new ScriptChunk(OP_CHECKSIG, null, 24),
        };
    }

    /**
     * <p>To run a script, first we parse it which breaks it up into chunks representing pushes of data or logical
     * opcodes. Then we can run the parsed chunks.</p>
     * <p/>
     * <p>The reason for this split, instead of just interpreting directly, is to make it easier
     * to reach into a programs structure and pull out bits of data without having to run it.
     * This is necessary to render the to/from addresses of transactions in a user interface.
     * The official client does something similar.</p>
     */
    private void parse(byte[] program) throws ScriptException {
        chunks = new ArrayList<ScriptChunk>(5);   // Common size.
        ByteArrayInputStream bis = new ByteArrayInputStream(program);
        int initialSize = bis.available();
        while (bis.available() > 0) {
            int startLocationInProgram = initialSize - bis.available();
            int opcode = bis.read();

            long dataToRead = -1;
            if (opcode >= 0 && opcode < OP_PUSHDATA1) {
                // Read some bytes of data, where how many is the opcode value itself.
                dataToRead = opcode;
            } else if (opcode == OP_PUSHDATA1) {
                if (bis.available() < 1) throw new ScriptException("Unexpected end of script");
                dataToRead = bis.read();
            } else if (opcode == OP_PUSHDATA2) {
                // Read a short, then read that many bytes of data.
                if (bis.available() < 2) throw new ScriptException("Unexpected end of script");
                dataToRead = bis.read() | (bis.read() << 8);
            } else if (opcode == OP_PUSHDATA4) {
                // Read a uint32, then read that many bytes of data.
                // Though this is allowed, because its value cannot be > 520, it should never actually be used
                if (bis.available() < 4) throw new ScriptException("Unexpected end of script");
                dataToRead = ((long) bis.read()) | (((long) bis.read()) << 8) | (((long) bis.read()) << 16) | (((long) bis.read()) << 24);
            }

            ScriptChunk chunk;
            if (dataToRead == -1) {
                chunk = new ScriptChunk(opcode, null, startLocationInProgram);
            } else {
                if (dataToRead > bis.available())
                    throw new ScriptException("Push of data element that is larger than remaining data");
                byte[] data = new byte[(int) dataToRead];
                checkState(dataToRead == 0 || bis.read(data, 0, (int) dataToRead) == dataToRead);
                chunk = new ScriptChunk(opcode, data, startLocationInProgram);
            }
            // Save some memory by eliminating redundant copies of the same chunk objects.
            for (ScriptChunk c : STANDARD_TRANSACTION_SCRIPT_CHUNKS) {
                if (c.equals(chunk)) chunk = c;
            }
            chunks.add(chunk);
        }
    }

    /**
     * Returns true if this script is of the form <sig> OP_CHECKSIG. This form was originally intended for transactions
     * where the peers talked to each other directly via TCP/IP, but has fallen out of favor with time due to that mode
     * of operation being susceptible to man-in-the-middle attacks. It is still used in coinbase outputs and can be
     * useful more exotic types of transaction, but today most payments are to addresses.
     */
    public boolean isSentToRawPubKey() {
        return chunks.size() == 2 && chunks.get(1).equalsOpCode(OP_CHECKSIG) &&
                !chunks.get(0).isOpCode() && chunks.get(0).data.length > 1;
    }

    /**
     * Returns true if this script is of the form DUP HASH160 <pubkey hash> EQUALVERIFY CHECKSIG, ie, payment to an
     * address like 1VayNert3x1KzbpzMGt2qdqrAThiRovi8. This form was originally intended for the case where you wish
     * to send somebody money with a written code because their node is offline, but over time has become the standard
     * way to make payments due to the short and recognizable base58 form addresses come in.
     */
    public boolean isSentToAddress() {
        return chunks.size() == 5 &&
                chunks.get(0).equalsOpCode(OP_DUP) &&
                chunks.get(1).equalsOpCode(OP_HASH160) &&
                chunks.get(2).data.length == 20 &&
                chunks.get(3).equalsOpCode(OP_EQUALVERIFY) &&
                chunks.get(4).equalsOpCode(OP_CHECKSIG);
    }

    /**
     * An alias for isPayToScriptHash.
     */
    @Deprecated
    public boolean isSentToP2SH() {
        return isPayToScriptHash();
    }

    public boolean isSendFromMultiSig() {
        boolean result = this.chunks.get(0).opcode == OP_0;
        for (int i = 1; i < this.chunks.size(); i++) {

            result &= this.chunks.get(i).data != null && this.chunks.get(i).data.length > 2;
        }
        if (result) {
            try {
                Script multiSigRedeem = new Script(this.chunks.get(this.chunks.size() - 1).data);
                result &= multiSigRedeem.isMultiSigRedeem();
            } catch (ScriptException ex) {
                result = false;
            }
        }
        return result;
    }

    public boolean isMultiSigRedeem() {
        boolean result = OP_1 <= this.chunks.get(0).opcode && this.chunks.get(0).opcode <= OP_16;
        for (int i = 1; i < this.chunks.size() - 2; i++) {
            result &= this.chunks.get(i).data != null && this.chunks.get(i).data.length > 2;
        }
        result &= OP_1 <= this.chunks.get(this.chunks.size() - 2).opcode && this.chunks.get(this.chunks.size() - 2).opcode <= OP_16;
        result &= this.chunks.get(this.chunks.size() - 1).opcode == OP_CHECKMULTISIG;
        return result;
    }

    /**
     * If a program matches the standard template DUP HASH160 <pubkey hash> EQUALVERIFY CHECKSIG
     * then this function retrieves the third element, otherwise it throws a ScriptException.<p>
     * <p/>
     * This is useful for fetching the destination address of a transaction.
     */
    public byte[] getPubKeyHash() throws ScriptException {
        if (isSentToAddress())
            return chunks.get(2).data;
        else if (isPayToScriptHash())
            return chunks.get(1).data;
        else
            throw new ScriptException("Script not in the standard scriptPubKey form");
    }

    /**
     * Returns the public key in this script. If a script contains two constants and nothing else, it is assumed to
     * be a scriptSig (input) for a pay-to-address output and the second constant is returned (the first is the
     * signature). If a script contains a constant and an OP_CHECKSIG opcode, the constant is returned as it is
     * assumed to be a direct pay-to-key scriptPubKey (output) and the first constant is the public key.
     *
     * @throws ScriptException if the script is none of the named forms.
     */
    public byte[] getPubKey() throws ScriptException {
        if (chunks.size() != 2) {
            throw new ScriptException("Script not of right size, expecting 2 but got " + chunks.size());
        }
        final ScriptChunk chunk0 = chunks.get(0);
        final byte[] chunk0data = chunk0.data;
        final ScriptChunk chunk1 = chunks.get(1);
        final byte[] chunk1data = chunk1.data;
        if (chunk0data != null && chunk0data.length > 2 && chunk1data != null && chunk1data.length > 2) {
            // If we have two large constants assume the input to a pay-to-address output.
            return chunk1data;
        } else if (chunk1.equalsOpCode(OP_CHECKSIG) && chunk0data != null && chunk0data.length > 2) {
            // A large constant followed by an OP_CHECKSIG is the key.
            return chunk0data;
        } else {
            throw new ScriptException("Script did not match expected form: " + toString());
        }
    }

    @Deprecated
    public byte[] getSig() throws ScriptException {
        if (chunks.size() == 1 && chunks.get(0).isPushData()) {
            return chunks.get(0).data;
        } else if (chunks.size() == 2 && chunks.get(0).isPushData()
                && chunks.get(1).isPushData()
                && chunks.get(0).data != null
                && chunks.get(0).data.length > 2
                && chunks.get(1).data != null
                && chunks.get(1).data.length > 2) {
            return chunks.get(0).data;
        } else {
            throw new ScriptException("Script did not match expected form: " + toString());
        }
    }

    public List<byte[]> getSigs() {
        List<byte[]> result = new ArrayList<byte[]>();
        if (chunks.size() == 1 && chunks.get(0).isPushData()) {
            result.add(chunks.get(0).data);
        } else if (chunks.size() == 2 && chunks.get(0).isPushData()
                && chunks.get(1).isPushData()
                && chunks.get(0).data != null
                && chunks.get(0).data.length > 2
                && chunks.get(1).data != null
                && chunks.get(1).data.length > 2) {
            result.add(chunks.get(0).data);
        } else if (chunks.size() >= 3 && chunks.get(0).opcode == OP_0) {
            boolean isPay2SHScript = true;
            for (int i = 1; i < this.chunks.size(); i++) {
                isPay2SHScript &= (this.chunks.get(i).data != null && this.chunks.get(i).data.length > 2);
            }
            if (isPay2SHScript) {
                for (int i = 1; i < this.chunks.size() - 1; i++) {
                    if (this.chunks.get(i).isPushData() && this.chunks.get(i).data != null
                            && this.chunks.get(i).data.length > 0
                            && this.chunks.get(i).data[0] == (byte) 48) {
                        result.add(this.chunks.get(i).data);
                    }
                }
            }
        }
        return result;
    }

    ////////////////////// Interface for writing scripts from scratch ////////////////////////////////

    /**
     * Writes out the given byte buffer to the output stream with the correct opcode prefix
     * To write an integer call writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(val, false)));
     */
    public static void writeBytes(OutputStream os, byte[] buf) throws IOException {
        if (buf.length < OP_PUSHDATA1) {
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 256) {
            os.write(OP_PUSHDATA1);
            os.write(buf.length);
            os.write(buf);
        } else if (buf.length < 65536) {
            os.write(OP_PUSHDATA2);
            os.write(0xFF & (buf.length));
            os.write(0xFF & (buf.length >> 8));
            os.write(buf);
        } else {
            throw new RuntimeException("Unimplemented");
        }
    }


    ////////////////////// Interface used during verification of transactions/blocks ////////////////////////////////

    private static int getSigOpCount(List<ScriptChunk> chunks, boolean accurate) throws ScriptException {
        int sigOps = 0;
        int lastOpCode = OP_INVALIDOPCODE;
        for (ScriptChunk chunk : chunks) {
            if (chunk.isOpCode()) {
                switch (chunk.opcode) {
                    case OP_CHECKSIG:
                    case OP_CHECKSIGVERIFY:
                        sigOps++;
                        break;
                    case OP_CHECKMULTISIG:
                    case OP_CHECKMULTISIGVERIFY:
                        if (accurate && lastOpCode >= OP_1 && lastOpCode <= OP_16)
                            sigOps += decodeFromOpN(lastOpCode);
                        else
                            sigOps += 20;
                        break;
                    default:
                        break;
                }
                lastOpCode = chunk.opcode;
            }
        }
        return sigOps;
    }

    static int decodeFromOpN(int opcode) {
        checkArgument((opcode == OP_0 || opcode == OP_1NEGATE) || (opcode >= OP_1 && opcode <= OP_16), "decodeFromOpN called on non OP_N opcode");
        if (opcode == OP_0)
            return 0;
        else if (opcode == OP_1NEGATE)
            return -1;
        else
            return opcode + 1 - OP_1;
    }

    public static int encodeToOpN(int value) {
        checkArgument(value >= -1 && value <= 16, "encodeToOpN called for " + value + " which we cannot encode in an opcode.");
        if (value == 0)
            return OP_0;
        else if (value == -1)
            return OP_1NEGATE;
        else
            return value - 1 + OP_1;
    }

    /**
     * Gets the count of regular SigOps in the script program (counting multisig ops as 20)
     */
    public static int getSigOpCount(byte[] program) throws ScriptException {
        Script script = new Script();
        try {
            script.parse(program);
        } catch (ScriptException e) {
            // Ignore errors and count up to the parse-able length
        }
        return getSigOpCount(script.chunks, false);
    }

    /**
     * Gets the count of P2SH Sig Ops in the Script scriptSig
     */
    public static long getP2SHSigOpCount(byte[] scriptSig) throws ScriptException {
        Script script = new Script();
        try {
            script.parse(scriptSig);
        } catch (ScriptException e) {
            // Ignore errors and count up to the parse-able length
        }
        for (int i = script.chunks.size() - 1; i >= 0; i--)
            if (!script.chunks.get(i).isOpCode()) {
                Script subScript = new Script();
                subScript.parse(script.chunks.get(i).data);
                return getSigOpCount(subScript.chunks, true);
            }
        return 0;
    }


    public int getNumberOfBytesRequiredToSpend(boolean isCompressed, @Nullable Script redeemScript) {
        if (isPayToScriptHash()) {
            // scriptSig: <sig> [sig] [sig...] <redeemscript>
            checkArgument(redeemScript != null, "P2SH script requires redeemScript to be spent");
            // for N of M CHECKMULTISIG redeem script we will need N signatures to spend
            ScriptChunk nChunk = redeemScript.getChunks().get(0);
            int n = Script.decodeFromOpN(nChunk.opcode);
            return n * SIG_SIZE + getProgram().length;
        } else if (isSentToMultiSig()) {
            // scriptSig: OP_0 <sig> [sig] [sig...]
            // for N of M CHECKMULTISIG script we will need N signatures to spend
            ScriptChunk nChunk = chunks.get(0);
            int n = Script.decodeFromOpN(nChunk.opcode);
            return n * SIG_SIZE + 1;
        } else if (isSentToRawPubKey()) {
            // scriptSig: <sig>
            return SIG_SIZE;
        } else if (isSentToAddress()) {
            // scriptSig: <sig> <pubkey>
            int uncompressedPubKeySize = 65;
            int compressedPubKeySize = 33;
            return SIG_SIZE + (isCompressed ? compressedPubKeySize : uncompressedPubKeySize);
        } else {
            throw new IllegalStateException("Unsupported script type");
        }
    }

    /**
     * <p>Whether or not this is a scriptPubKey representing a pay-to-script-hash output. In such outputs, the logic that
     * controls reclamation is not actually in the output at all. Instead there's just a hash, and it's up to the
     * spending input to provide a program matching that hash. This rule is "soft enforced" by the network as it does
     * not exist in Satoshis original implementation. It means blocks containing P2SH transactions that don't match
     * correctly are considered valid, but won't be mined upon, so they'll be rapidly re-orgd out of the chain. This
     * logic is defined by <a href="https://github.com/bitcoin/bips/blob/master/bip-0016.mediawiki">BIP 16</a>.</p>
     * <p/>
     * <p>bitcoinj does not support creation of P2SH transactions today. The goal of P2SH is to allow short addresses
     * even for complex scripts (eg, multi-sig outputs) so they are convenient to work with in things like QRcodes or
     * with copy/paste, and also to minimize the size of the unspent output set (which improves performance of the
     * Bitcoin system).</p>
     */
    public boolean isPayToScriptHash() {
        // We have to check against the serialized form because BIP16 defines a P2SH output using an exact byte
        // template, not the logical program structure. Thus you can have two programs that look identical when
        // printed out but one is a P2SH script and the other isn't! :(
        byte[] program = getProgram();
        return program.length == 23 &&
                (program[0] & 0xff) == OP_HASH160 &&
                (program[1] & 0xff) == 0x14 &&
                (program[22] & 0xff) == OP_EQUAL;
    }

    /**
     * Returns whether this script matches the format used for multisig outputs: [n] [keys...] [m] CHECKMULTISIG
     */
    public boolean isSentToMultiSig() {
        if (chunks.size() < 4) return false;
        ScriptChunk chunk = chunks.get(chunks.size() - 1);
        // Must end in OP_CHECKMULTISIG[VERIFY].
        if (!chunk.isOpCode()) return false;
        if (!(chunk.equalsOpCode(OP_CHECKMULTISIG) || chunk.equalsOpCode(OP_CHECKMULTISIGVERIFY)))
            return false;
        try {
            // Second to last chunk must be an OP_N opcode and there should be that many data chunks (keys).
            ScriptChunk m = chunks.get(chunks.size() - 2);
            if (!m.isOpCode()) return false;
            int numKeys = decodeFromOpN(m.opcode);
            if (numKeys < 1 || chunks.size() != 3 + numKeys) return false;
            for (int i = 1; i < chunks.size() - 2; i++) {
                if (chunks.get(i).isOpCode()) return false;
            }
            // First chunk must be an OP_N opcode too.
            if (decodeFromOpN(chunks.get(0).opcode) < 1) return false;
        } catch (IllegalStateException e) {
            return false;   // Not an OP_N opcode.
        }
        return true;
    }

    private static boolean equalsRange(byte[] a, int start, byte[] b) {
        if (start + b.length > a.length)
            return false;
        for (int i = 0; i < b.length; i++)
            if (a[i + start] != b[i])
                return false;
        return true;
    }

    // Utility that doesn't copy for internal use
    private byte[] getQuickProgram() {
        if (program != null)
            return program;
        return getProgram();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Script other = (Script) o;
        return Arrays.equals(getQuickProgram(), other.getQuickProgram());
    }

    @Override
    public int hashCode() {
        byte[] bytes = getQuickProgram();
        return Arrays.hashCode(bytes);
    }
}
