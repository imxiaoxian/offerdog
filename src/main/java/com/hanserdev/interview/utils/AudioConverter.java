package com.hanserdev.interview.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * 将 PCM 16bit 单声道音频包装为 WAV（采样率需与 PCM 实际一致）。
 */
public final class AudioConverter {

    private static final int DEFAULT_SAMPLE_RATE = 16000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8;

    private AudioConverter() {
    }

    /**
     * 将原始 PCM 音频数据转换为 WAV 格式（默认按 16kHz 声明，兼容旧调用）。
     */
    public static byte[] pcmToWav(byte[] pcmData) {
        return pcmToWav(pcmData, DEFAULT_SAMPLE_RATE);
    }

    /**
     * @param sampleRateHz PCM 实际采样率（须与前端 AudioContext.sampleRate 一致）
     */
    public static byte[] pcmToWav(byte[] pcmData, int sampleRateHz) {
        if (pcmData == null || pcmData.length == 0) {
            return new byte[0];
        }
        if (sampleRateHz <= 0) {
            sampleRateHz = DEFAULT_SAMPLE_RATE;
        }

        byte[] wavHeader = createWavHeader(pcmData.length, sampleRateHz);
        byte[] wavData = new byte[wavHeader.length + pcmData.length];

        System.arraycopy(wavHeader, 0, wavData, 0, wavHeader.length);
        System.arraycopy(pcmData, 0, wavData, wavHeader.length, pcmData.length);

        return wavData;
    }

    private static byte[] createWavHeader(int audioDataSize, int sampleRateHz) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(36 + audioDataSize));
            dos.writeBytes("WAVE");

            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short) 1));
            dos.writeShort(Short.reverseBytes((short) CHANNELS));
            dos.writeInt(Integer.reverseBytes(sampleRateHz));
            dos.writeInt(Integer.reverseBytes(sampleRateHz * CHANNELS * BYTES_PER_SAMPLE));
            dos.writeShort(Short.reverseBytes((short) (CHANNELS * BYTES_PER_SAMPLE)));
            dos.writeShort(Short.reverseBytes((short) BITS_PER_SAMPLE));

            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(audioDataSize));
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * 粗略检查是否为 WAV 格式。
     */
    public static boolean isWavFormat(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }
        return data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
    }
}
