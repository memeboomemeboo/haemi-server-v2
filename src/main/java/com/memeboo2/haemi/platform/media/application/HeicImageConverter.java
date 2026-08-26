package com.memeboo2.haemi.platform.media.application;

/** HEIC/HEIF 바이트를 JPEG 바이트로 변환한다. 네이티브 바이너리(libheif/ImageMagick)에 의존. */
public interface HeicImageConverter {

    byte[] toJpeg(byte[] heic);
}
