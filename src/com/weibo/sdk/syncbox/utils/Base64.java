package com.weibo.sdk.syncbox.utils;

import java.io.UnsupportedEncodingException;

/*
 * @ClassName: Base64
 * @Description:Base64工具类
 * @author LiuZhao
 * @date 2012-9-20 下午12:00:40
 */
public class Base64 {

	private int bitWorkArea;
	protected byte[] buffer;
	protected int pos;
	private int readPos;
	protected boolean eof;
	protected int currentLinePos;
	protected int modulus;
	static final byte[] CHUNK_SEPARATOR = { 13, 10 };
	private int unencodedBlockSize;
	private int encodedBlockSize;
	protected int lineLength;
	private int chunkSeparatorLength = 0;
	private byte[] encodeTable;
	private int decodeSize;
	private int encodeSize;
	private byte[] lineSeparator;
	private static final byte[] DECODE_TABLE = { -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
			-1, -1, 62, -1, 62, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
			-1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
			12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1,
			-1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
			40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };
	private final byte[] decodeTable = DECODE_TABLE;
	private static final byte[] STANDARD_ENCODE_TABLE = { 65, 66, 67, 68, 69,
			70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86,
			87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
			108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
			121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47 };
	private static final byte[] URL_SAFE_ENCODE_TABLE = { 65, 66, 67, 68, 69,
			70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86,
			87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
			108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120,
			121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 45, 95 };

	public static String encodeBase64String(byte[] binaryData) {
		String str = null;
		try {
			str = new String((encodeBase64(binaryData, false, 2147483647)),
					"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}

	public static byte[] encodeBase64(byte[] binaryData, boolean urlSafe,
			int maxResultSize) {
		if ((binaryData == null) || (binaryData.length == 0)) {
			return binaryData;
		}
		Base64 b64 = new Base64(0, CHUNK_SEPARATOR, urlSafe);
		long len = b64.getEncodedLength(binaryData);
		if (len > maxResultSize) {
			throw new IllegalArgumentException(
					"Input array too big, the output array would be bigger "
							+ "(" + len
							+ ") than the specified maximum size of "
							+ maxResultSize);
		}
		return b64.encode(binaryData);
	}

	public Base64(int lineLength, byte[] lineSeparator, boolean urlSafe) {

		this.unencodedBlockSize = 3;
		this.encodedBlockSize = 4;
		this.lineLength = ((lineLength > 0) && (chunkSeparatorLength > 0) ? lineLength
				/ encodedBlockSize * encodedBlockSize
				: 0);
		this.chunkSeparatorLength = (lineSeparator == null) ? 0
				: lineSeparator.length;

		if (lineSeparator != null) {
			if (containsAlphabetOrPad(lineSeparator)) {
				String sep;
				try {
					sep = new String(lineSeparator, "UTF-8");
					throw new IllegalArgumentException(
							"lineSeparator must not contain base64 characters: ["
									+ sep + "]");

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			if (lineLength > 0) {
				this.encodeSize = (4 + lineSeparator.length);
				this.lineSeparator = new byte[lineSeparator.length];
				System.arraycopy(lineSeparator, 0, this.lineSeparator, 0,
						lineSeparator.length);
			} else {
				this.encodeSize = 4;
				this.lineSeparator = null;
			}
		} else {
			this.encodeSize = 4;
			this.lineSeparator = null;
		}
		this.decodeSize = (this.encodeSize - 1);
		this.encodeTable = (urlSafe ? URL_SAFE_ENCODE_TABLE
				: STANDARD_ENCODE_TABLE);
	}

	protected boolean containsAlphabetOrPad(byte[] arrayOctet) {
		if (arrayOctet == null) {
			return false;
		}
		for (byte element : arrayOctet) {
			if ((61 == element) || (isInAlphabet(element))) {
				return true;
			}
		}
		return false;
	}

	public long getEncodedLength(byte[] pArray) {
		long len = (pArray.length + this.unencodedBlockSize - 1)
				/ this.unencodedBlockSize * this.encodedBlockSize;
		if (this.lineLength > 0) {
			len += (len + this.lineLength - 1L) / this.lineLength
					* this.chunkSeparatorLength;
		}
		return len;
	}

	protected boolean isInAlphabet(byte octet) {
		return (octet >= 0) && (octet < this.decodeTable.length)
				&& (this.decodeTable[octet] != -1);
	}

	public byte[] encode(byte[] pArray) {
		reset();
		if ((pArray == null) || (pArray.length == 0)) {
			return pArray;
		}
		encode(pArray, 0, pArray.length);
		encode(pArray, 0, -1);
		byte[] buf = new byte[this.pos - this.readPos];
		readResults(buf, 0, buf.length);
		return buf;
	}

	int readResults(byte[] b, int bPos, int bAvail) {
		if (this.buffer != null) {
			int len = Math.min(available(), bAvail);
			System.arraycopy(this.buffer, this.readPos, b, bPos, len);
			this.readPos += len;
			if (this.readPos >= this.pos) {
				this.buffer = null;
			}
			return len;
		}
		return this.eof ? -1 : 0;
	}

	int available() {
		return this.buffer != null ? this.pos - this.readPos : 0;
	}

	private void reset() {
		this.buffer = null;
		this.pos = 0;
		this.readPos = 0;
		this.currentLinePos = 0;
		this.modulus = 0;
		this.eof = false;
	}

	void encode(byte[] in, int inPos, int inAvail) {
		if (this.eof) {
			return;
		}

		if (inAvail < 0) {
			this.eof = true;
			if ((0 == this.modulus) && (this.lineLength == 0)) {
				return;
			}
			if ((this.buffer == null)
					|| (this.buffer.length < this.pos + this.encodeSize))
				resizeBuffer();
			int savedPos = this.pos;
			switch (this.modulus) {
			case 1:
				this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 2 & 0x3F)];
				this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea << 4 & 0x3F)];

				if (this.encodeTable == STANDARD_ENCODE_TABLE) {
					this.buffer[(this.pos++)] = 61;
					this.buffer[(this.pos++)] = 61;
				}
				break;
			case 2:
				this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 10 & 0x3F)];
				this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 4 & 0x3F)];
				this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea << 2 & 0x3F)];

				if (this.encodeTable == STANDARD_ENCODE_TABLE) {
					this.buffer[(this.pos++)] = 61;
				}
				break;
			}
			this.currentLinePos += this.pos - savedPos;

			if ((this.lineLength > 0) && (this.currentLinePos > 0)) {
				System.arraycopy(this.lineSeparator, 0, this.buffer, this.pos,
						this.lineSeparator.length);
				this.pos += this.lineSeparator.length;
			}
		} else {
			for (int i = 0; i < inAvail; i++) {
				if ((this.buffer == null)
						|| (this.buffer.length < this.pos + this.encodeSize))
					resizeBuffer();
				this.modulus = ((this.modulus + 1) % 3);
				int b = in[(inPos++)];
				if (b < 0) {
					b += 256;
				}
				this.bitWorkArea = ((this.bitWorkArea << 8) + b);
				if (0 == this.modulus) {
					this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 18 & 0x3F)];
					this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 12 & 0x3F)];
					this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea >> 6 & 0x3F)];
					this.buffer[(this.pos++)] = this.encodeTable[(this.bitWorkArea & 0x3F)];
					this.currentLinePos += 4;
					if ((this.lineLength > 0)
							&& (this.lineLength <= this.currentLinePos)) {
						System.arraycopy(this.lineSeparator, 0, this.buffer,
								this.pos, this.lineSeparator.length);
						this.pos += this.lineSeparator.length;
						this.currentLinePos = 0;
					}
				}
			}
		}
	}

	private void resizeBuffer() {
		if (this.buffer == null) {
			this.buffer = new byte[8192];
			this.pos = 0;
			this.readPos = 0;
		} else {
			byte[] b = new byte[this.buffer.length * 2];
			System.arraycopy(this.buffer, 0, b, 0, this.buffer.length);
			this.buffer = b;
		}
	}

	public static byte[] decodeBase64(String base64String) {
		Base64 b4 = new Base64(0, CHUNK_SEPARATOR, false);
		byte[] base64Bytes = null;
		try {
			base64Bytes = base64String.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] result = b4.decode(base64Bytes);
		return result;
	}

	public byte[] decode(byte[] pArray) {
		reset();
		if ((pArray == null) || (pArray.length == 0)) {
			return pArray;
		}
		decode(pArray, 0, pArray.length);
		decode(pArray, 0, -1);
		byte[] result = new byte[this.pos];
		readResults(result, 0, result.length);
		return result;
	}

	void decode(byte[] in, int inPos, int inAvail) {
		if (this.eof) {
			return;
		}
		if (inAvail < 0) {
			this.eof = true;
		}
		for (int i = 0; i < inAvail; i++) {
			ensureBufferSize(this.decodeSize);
			byte b = in[(inPos++)];
			if (b == 61) {
				this.eof = true;
				break;
			}
			if ((b >= 0) && (b < DECODE_TABLE.length)) {
				int result = DECODE_TABLE[b];
				if (result >= 0) {
					this.modulus = ((this.modulus + 1) % 4);
					this.bitWorkArea = ((this.bitWorkArea << 6) + result);
					if (this.modulus == 0) {
						this.buffer[(this.pos++)] = (byte) (this.bitWorkArea >> 16 & 0xFF);
						this.buffer[(this.pos++)] = (byte) (this.bitWorkArea >> 8 & 0xFF);
						this.buffer[(this.pos++)] = (byte) (this.bitWorkArea & 0xFF);
					}

				}

			}

		}

		if ((this.eof) && (this.modulus != 0)) {
			ensureBufferSize(this.decodeSize);

			switch (this.modulus) {
			case 2:
				this.bitWorkArea >>= 4;
				this.buffer[(this.pos++)] = (byte) (this.bitWorkArea & 0xFF);
				break;
			case 3:
				this.bitWorkArea >>= 2;
				this.buffer[(this.pos++)] = (byte) (this.bitWorkArea >> 8 & 0xFF);
				this.buffer[(this.pos++)] = (byte) (this.bitWorkArea & 0xFF);
			}
		}
	}

	protected void ensureBufferSize(int size) {
		if ((this.buffer == null) || (this.buffer.length < this.pos + size))
			resizeBuffer();
	}

	public static byte[] encodeBase64(byte[] binaryData) {
		return encodeBase64(binaryData, false, 2147483647);
	}
}
