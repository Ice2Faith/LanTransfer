package i2f.net.core;

import java.io.Serializable;

/**
 * @author ltb
 * @date 2022/2/20 15:36
 * @desc
 */
public class NetTransferHead implements Serializable {
    public static final String MIME_TEXT_PLAIN="text/plain";
    public static final String MIME_TEXT_HTML="text/html";
    public static final String MIME_TEXT_JSON="application/json";
    public static final String MIME_TEXT_XML="text/xml";
    public static final String MIME_OCTET_STREAM="application/octet-stream";
    public static final String MIME_VIDEO_MP4="video/mp4";
    public static final String MIME_AUDIO_MPEG="audio/mpeg";
    public static final String MIME_IMAGE_JPEG="image/jpeg";
    public static final String MIME_IMAGE_PNG="image/png";
    public static final String MIME_IMAGE_GIF="image/gif";

    private static final long serialVersionUID = 1L;

    private String date;
    private String mimeType;
    private long contentLength;
    private String name;
    private String charset;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
