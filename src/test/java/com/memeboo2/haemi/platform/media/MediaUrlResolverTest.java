package com.memeboo2.haemi.platform.media;

import com.memeboo2.haemi.platform.media.application.MediaUrlResolver;
import com.memeboo2.haemi.platform.media.infrastructure.StoragePort;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaUrlResolverTest {

    @Test
    void 기존_로컬_serving_URL에서는_key_쿼리_파라미터를_복원한다() throws Exception {
        StoragePort storage = mock(StoragePort.class);
        when(storage.generateServingUrl("memory_image/photo.jpg"))
                .thenReturn(URI.create("http://localhost/serve?key=memory_image/photo.jpg"));
        MediaUrlResolver resolver = resolver(storage, "haemi-media");

        String result = resolver.toServingUrl("http://localhost:8080/api/v1/internal/storage/serve?key=memory_image/photo.jpg");

        assertThat(result).contains("key=memory_image/photo.jpg");
        verify(storage).generateServingUrl("memory_image/photo.jpg");
    }

    @Test
    void 기존_path_style_S3_URL에서는_bucket_접두어를_제거한다() throws Exception {
        StoragePort storage = mock(StoragePort.class);
        when(storage.generateServingUrl("memory_image/photo.jpg"))
                .thenReturn(URI.create("https://storage.example/haemi-media/memory_image/photo.jpg?new-signature"));
        MediaUrlResolver resolver = resolver(storage, "haemi-media");

        resolver.toServingUrl("https://storage.example/haemi-media/memory_image/photo.jpg?X-Amz-Signature=old");

        verify(storage).generateServingUrl("memory_image/photo.jpg");
    }

    private MediaUrlResolver resolver(StoragePort storage, String bucket) throws Exception {
        MediaUrlResolver resolver = new MediaUrlResolver(storage);
        Field field = MediaUrlResolver.class.getDeclaredField("bucket");
        field.setAccessible(true);
        field.set(resolver, bucket);
        return resolver;
    }
}
